package soa.lab2.adapter;

import java.net.ServerSocket;
import java.util.List;
import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import soa.lab2.starship.dto.*;
import soa.lab2.starship.soap.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises real HTTP REST -> CXF SOAP/XML -> a SOAP endpoint, including faults. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestSoapIntegrationTest {
    private static final StarshipSoapPort SOAP = mock(StarshipSoapPort.class);
    private static Server server;
    private static int soapPort;

    @DynamicPropertySource
    static void upstream(DynamicPropertyRegistry registry) throws Exception {
        try (var socket = new ServerSocket(0)) {
            soapPort = socket.getLocalPort();
        }
        var factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(StarshipSoapPort.class);
        factory.setServiceBean(SOAP);
        factory.setAddress("http://localhost:" + soapPort + "/services/starships");
        server = factory.create();
        registry.add("starship.soap-url", () -> "http://localhost:" + soapPort + "/services/starships");
    }

    @Autowired TestRestTemplate rest;
    @Autowired StarshipSoapPort client;

    @BeforeEach
    void resetSoap() { reset(SOAP); }

    @AfterAll
    static void shutdownSoap() { if (server != null) server.destroy(); }

    @Test
    void preservesDefaultPaginationAndEmptyContent() throws Exception {
        when(SOAP.getStarships(0, 10)).thenReturn(new PageOfStarshipDTO()
                .page(0).size(10).totalElements(0).totalPages(0));

        var response = rest.getForEntity("/api/v1/starships", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"content\":[]", "\"size\":10", "\"totalElements\":0");
        verify(SOAP).getStarships(0, 10);
    }

    @Test
    void forwardsRepeatedFilterExpressionsWithoutSplittingCommas() throws Exception {
        when(SOAP.getStarshipSpaceMarines(eq(7), eq(2), eq(5), anyList()))
                .thenReturn(new PageOfSpaceMarineDTO().page(2).size(5).totalElements(0).totalPages(0));

        var response = rest.getForEntity("/api/v1/starship/7/space-marines?page=2&size=5"
                + "&filter=health,gt,10&filter=name,eq,a,b", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"content\":[]");
        verify(SOAP).getStarshipSpaceMarines(7, 2, 5, List.of("health,gt,10", "name,eq,a,b"));
    }

    @Test
    void preservesMarineRepresentationAndNullableFields() throws Exception {
        var marine = new SpaceMarineDTO().id(42L).name("Marine")
                .creationDate("2026-11-19T12:34:56+03:00").health(100)
                .weaponType("COMBI_FLAMER").coordinates(new CoordinatesDTO().x(1.5).y(2))
                .chapter(new ChapterDTO().name("Chapter").world("Terra"));
        when(SOAP.loadSpaceMarine(7, 42)).thenReturn(marine);

        var response = rest.postForEntity("/api/v1/starship/7/load/42", null, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"id\":42", "\"creationDate\":\"2026-11-19T12:34:56+03:00\"",
                "\"achievements\":null", "\"meleeWeapon\":null", "\"parentLegion\":null");
        verify(SOAP).loadSpaceMarine(7, 42);
    }

    @Test
    void unloadResponseRemainsJsonInteger() throws Exception {
        when(SOAP.unloadAll(7)).thenReturn(3);

        var response = rest.postForEntity("/api/v1/starship/7/unload-all", null, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("3");
        verify(SOAP).unloadAll(7);
    }

    @Test
    void translatesTypedSoapFaultWithOriginalStatusMessageAndField() throws Exception {
        when(SOAP.loadSpaceMarine(7, 42)).thenThrow(new StarshipFault("Already boarded",
                new FaultDetail(409, "Already boarded", "spaceMarineId")));

        var response = rest.postForEntity("/api/v1/starship/7/load/42", null, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo("{\"status\":409,\"message\":\"Already boarded\",\"field\":\"spaceMarineId\"}");
    }

    @Test
    void delegatesDomainValidationToSoapService() throws Exception {
        when(SOAP.getStarships(-1, 101)).thenThrow(new StarshipFault("Invalid pagination",
                new FaultDetail(400, "Invalid pagination", null)));

        var response = rest.getForEntity("/api/v1/starships?page=-1&size=101", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"message\":\"Invalid pagination\"", "\"field\":null");
        verify(SOAP).getStarships(-1, 101);
    }

    @Test
    void rejectsMalformedHttpParametersAndWrongMethodsBeforeSoap() {
        assertThat(rest.getForEntity("/api/v1/starships?page=abc", String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(rest.exchange("/api/v1/starships", HttpMethod.POST, null, String.class)
                .getStatusCode().value()).isEqualTo(405);
        verifyNoInteractions(SOAP);
    }

    @Test
    void mapsUnmodeledSoapFaultToBadGateway() throws Exception {
        when(SOAP.unloadAll(7)).thenThrow(new IllegalStateException("Internal details"));

        var response = rest.postForEntity("/api/v1/starship/7/unload-all", null, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody()).contains("\"status\":502", "\"field\":null")
                .doesNotContain("Internal details");
    }

    @Test
    void mapsUnavailableSoapEndpointToServiceUnavailable() throws Exception {
        int unavailablePort;
        try (var socket = new ServerSocket(0)) { unavailablePort = socket.getLocalPort(); }
        var context = ((BindingProvider) client).getRequestContext();
        Object original = context.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + unavailablePort + "/services/starships");
        try {
            var response = rest.getForEntity("/api/v1/starships", String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getBody()).contains("\"status\":503", "\"field\":null");
            verifyNoInteractions(SOAP);
        } finally {
            if (original == null) context.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            else context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, original);
        }
    }
}
