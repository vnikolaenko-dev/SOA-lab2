package soa.lab2.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP through Zuul and Ribbon; upstream stand-ins record what arrives. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false", "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false", "ribbon.eureka.enabled=false"
})
class GatewayRoutingTest {
    private static final HttpServer COLLECTION_A = upstream("collection-a");
    private static final HttpServer COLLECTION_B = upstream("collection-b");
    private static final HttpServer STARSHIP = upstream("starship");
    private final HttpClient client = HttpClient.newHttpClient();
    private static volatile int gatewayPort;

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("collection-service.ribbon.listOfServers",
                () -> address(COLLECTION_A) + "," + address(COLLECTION_B));
        registry.add("starship-service.ribbon.listOfServers", () -> address(STARSHIP));
        registry.add("collection-service.ribbon.NFLoadBalancerRuleClassName",
                () -> "com.netflix.loadbalancer.RoundRobinRule");
    }

    @Test
    void preservesMethodsBodyQueryAndHeadersForBothApis() throws Exception {
        for (String service : new String[]{"collection-service", "starship-service"}) {
            for (String method : new String[]{"GET", "POST", "PATCH", "DELETE"}) {
                String body = "GET".equals(method) ? "" : "{\"health\":80}";
                HttpResponse<String> response = send("/api/" + service + "/space-marines/7?filter=a%2Fb",
                        method, body);
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo(method + " /api/v1/space-marines/7?filter=a%2Fb\n" + body);
                assertThat(response.headers().firstValue("X-Request-Token")).contains("test-token");
                assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
            }
        }
    }

    @Test
    void ribbonUsesBothCollectionInstances() throws Exception {
        Set<String> instances = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            instances.add(send("/api/collection-service/space-marines", "GET", "")
                    .headers().firstValue("X-Upstream").orElseThrow());
        }
        assertThat(instances).containsExactlyInAnyOrder("collection-a", "collection-b");
    }

    @Test
    void preservesUpstreamErrorsAndRejectsUnknownRoutes() throws Exception {
        assertThat(send("/api/collection-service/missing", "GET", "").statusCode()).isEqualTo(404);
        assertThat(send("/api/starship-service/conflict", "POST", "{}").statusCode()).isEqualTo(409);
        assertThat(send("/api/unknown/space-marines", "GET", "").statusCode()).isEqualTo(404);
    }

    @Test
    void resourceRoutePreservesFullUpstreamPath() throws Exception {
        assertThat(send("/collection-service/openapi.yaml", "GET", "").body())
                .isEqualTo("GET /openapi.yaml\n");
        assertThat(send("/starship-service/api/v1/starships", "GET", "").body())
                .isEqualTo("GET /api/v1/starships\n");
    }

    @Test
    void rewritesCreatedResourceLocationToPublicApi() throws Exception {
        HttpResponse<String> response = send("/api/collection-service/created", "POST", "{}");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location"))
                .contains("/api/collection-service/space-marines/7");
    }

    @Test
    void starshipCanCallCollectionThroughTheSameGateway() throws Exception {
        gatewayPort = port;
        HttpResponse<String> response = send("/api/starship-service/nested", "GET", "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("GET /api/v1/space-marines/7\n");
    }

    private HttpResponse<String> send(String path, String method, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").header("X-Request-Token", "test-token")
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer upstream(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                if ("starship".equals(name) && "/api/v1/nested".equals(exchange.getRequestURI().getPath())) {
                    try {
                        HttpResponse<byte[]> nested = HttpClient.newHttpClient().send(
                                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort
                                        + "/api/collection-service/space-marines/7")).GET().build(),
                                HttpResponse.BodyHandlers.ofByteArray());
                        exchange.sendResponseHeaders(nested.statusCode(), nested.body().length);
                        exchange.getResponseBody().write(nested.body());
                        exchange.close();
                        return;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException(ex);
                    }
                }
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] result = (exchange.getRequestMethod() + " " + exchange.getRequestURI() + "\n" + body)
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("X-Upstream", name);
                String token = exchange.getRequestHeaders().getFirst("X-Request-Token");
                if (token != null) exchange.getResponseHeaders().set("X-Request-Token", token);
                String path = exchange.getRequestURI().getPath();
                if (path.endsWith("created")) {
                    exchange.getResponseHeaders().set("Location", "/api/v1/space-marines/7");
                }
                exchange.sendResponseHeaders(path.endsWith("created") ? 201
                                : path.endsWith("missing") ? 404 : path.endsWith("conflict") ? 409 : 200,
                        result.length);
                exchange.getResponseBody().write(result);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String address(HttpServer server) {
        return "127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopUpstreams() {
        COLLECTION_A.stop(0);
        COLLECTION_B.stop(0);
        STARSHIP.stop(0);
    }
}
