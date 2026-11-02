package soa.lab2.starship.client;

import com.netflix.loadbalancer.BaseLoadBalancer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RibbonRequestInterceptorTest {
    private final BaseLoadBalancer balancer = new BaseLoadBalancer();
    private final DiscoveryClient discovery = mock(DiscoveryClient.class);
    private final RibbonRequestInterceptor interceptor =
            new RibbonRequestInterceptor(discovery, balancer, "collection-service");
    private final DefaultServiceInstance first =
            new DefaultServiceInstance("one", "collection-service", "localhost", 8081, false);
    private final DefaultServiceInstance second =
            new DefaultServiceInstance("two", "collection-service", "localhost", 8083, false);

    @AfterEach
    void close() {
        balancer.shutdown();
    }

    @Test
    void alternatesInstancesAndPreservesEncodedPathAndQuery() throws Exception {
        when(discovery.getInstances("collection-service")).thenReturn(List.of(first, second));
        var destinations = new ArrayList<URI>();
        ClientHttpRequestExecution execution = (request, body) -> {
            destinations.add(request.getURI());
            return null;
        };
        for (int i = 0; i < 4; i++) {
            interceptor.intercept(request(), new byte[0], execution);
        }
        assertThat(destinations.get(0).getPort()).isNotEqualTo(destinations.get(1).getPort());
        assertThat(destinations.get(0)).isEqualTo(destinations.get(2));
        assertThat(destinations.get(1)).isEqualTo(destinations.get(3));
        assertThat(destinations).allSatisfy(uri -> {
            assertThat(uri.getHost()).isEqualTo("localhost");
            assertThat(uri.getRawPath()).isEqualTo("/api/v1/space-marines/a%20b");
            assertThat(uri.getRawQuery()).isEqualTo("filter=a%2Fb");
        });
        when(discovery.getInstances("collection-service")).thenReturn(List.of(first));
        interceptor.intercept(request(), new byte[0], execution);
        assertThat(destinations.getLast().getPort()).isEqualTo(8081);
    }

    @Test
    void emptyRegistryDoesNotCallAnOldInstance() throws Exception {
        var execution = mock(ClientHttpRequestExecution.class);
        when(discovery.getInstances("collection-service")).thenReturn(List.of(first));
        interceptor.intercept(request(), new byte[0], execution);
        clearInvocations(execution);
        when(discovery.getInstances("collection-service")).thenReturn(List.of());
        assertThatThrownBy(() -> interceptor.intercept(request(), new byte[0], execution))
                .isInstanceOf(ResourceAccessException.class);
        verifyNoInteractions(execution);
    }

    private MockClientHttpRequest request() {
        return new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://collection-service/api/v1/space-marines/a%20b?filter=a%2Fb"));
    }
}
