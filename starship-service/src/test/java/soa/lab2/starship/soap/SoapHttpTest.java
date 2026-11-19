package soa.lab2.starship.soap;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.model.SpaceMarine;
import soa.lab2.starship.service.StarshipCommandService;
import soa.lab2.starship.service.StarshipQueryService;
import java.net.URI;
import java.net.http.*;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SoapHttpTest.TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.tomcat.accesslog.enabled=false", "cxf.path=/services"})
class SoapHttpTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @Import({SoapConfiguration.class, StarshipSoapEndpoint.class, StarshipMapper.class})
    static class TestApplication {}

    @LocalServerPort int port;
    @MockitoBean StarshipCommandService commands;
    @MockitoBean StarshipQueryService queries;
    StarshipSoapPort client;

    @BeforeEach void connect() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(StarshipSoapPort.class);
        factory.setAddress("http://localhost:" + port + "/services/starships");
        client = (StarshipSoapPort) factory.create();
    }

    @Test void wsdlPublishesFourTypedOperations() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/services/starships?wsdl")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        for (String operation : List.of("loadSpaceMarine", "unloadAll", "getStarships", "getStarshipSpaceMarines", "StarshipFault"))
            assertTrue(response.body().contains(operation), operation);
    }

    @Test void allOperationsRoundTripWithEmptyPagesAndIsoDate() throws Exception {
        var marine = new SpaceMarine(5L, "Marine", new SpaceMarine.Coordinates(1.5, 2),
                OffsetDateTime.parse("2026-01-01T12:30:00Z"), 100, null, "COMBI_FLAMER", null,
                new SpaceMarine.Chapter("Chapter", null, "World"));
        when(commands.load(2L, 5L)).thenReturn(marine);
        assertEquals("2026-01-01T12:30:00Z", client.loadSpaceMarine(2, 5).getCreationDate());
        when(commands.unloadAll(2L)).thenReturn(1);
        assertEquals(1, client.unloadAll(2));
        when(queries.listStarships(null, null)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        var ships = client.getStarships(null, null);
        assertNotNull(ships.getContent());
        assertTrue(ships.getContent().isEmpty());
        assertEquals(10, ships.getSize());
        var filters = List.of("name,eq,a,b", "health,gt,10");
        when(queries.listCrew(2L, 0, 10, filters)).thenReturn(new PageImpl<>(List.of(marine), PageRequest.of(0, 10), 1));
        assertEquals(5L, client.getStarshipSpaceMarines(2, 0, 10, filters).getContent().getFirst().getId());
        verify(queries).listCrew(2L, 0, 10, filters);
    }

    @Test void soapValidatesIdentifiersWithoutInvokingBusinessLogic() {
        for (Integer id : new Integer[]{null, 0, -1}) {
            var fault = assertThrows(StarshipFault.class, () -> client.loadSpaceMarine(id, 5));
            assertEquals(400, fault.getFaultInfo().getStatus());
            assertEquals("starshipId", fault.getFaultInfo().getField());
        }
        verifyNoInteractions(commands);
    }

    @Test void preservesDeclaredErrorStatusesInSoapFaultDetails() {
        for (int status : new int[]{400, 404, 409, 502, 503}) {
            doThrow(new ResponseStatusException(HttpStatus.valueOf(status), "Upstream message")).when(commands).load(2L, 5L);
            var fault = assertThrows(StarshipFault.class, () -> client.loadSpaceMarine(2, 5));
            assertEquals(status, fault.getFaultInfo().getStatus());
            assertEquals("Upstream message", fault.getFaultInfo().getMessage());
        }
    }
}
