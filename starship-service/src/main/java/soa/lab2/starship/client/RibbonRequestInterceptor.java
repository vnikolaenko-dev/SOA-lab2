package soa.lab2.starship.client;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.Server;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/** Adapts Eureka discovery to Netflix Ribbon's round-robin load balancer. */
public class RibbonRequestInterceptor implements ClientHttpRequestInterceptor {
    private final DiscoveryClient discovery;
    private final BaseLoadBalancer loadBalancer;
    private final String serviceId;

    public RibbonRequestInterceptor(DiscoveryClient discovery, BaseLoadBalancer loadBalancer, String serviceId) {
        this.discovery = discovery;
        this.loadBalancer = loadBalancer;
        this.serviceId = serviceId;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        URI original = request.getURI();
        if (!serviceId.equalsIgnoreCase(original.getHost())) {
            throw new IllegalArgumentException("Expected logical service host: " + serviceId);
        }
        Server server;
        synchronized (loadBalancer) {
            // DiscoveryClient reads Eureka's local registry; no extra discovery HTTP call per request.
            var servers = discovery.getInstances(serviceId).stream()
                    .map(instance -> new Server(instance.isSecure() ? "https" : "http",
                            instance.getHost(), instance.getPort()))
                    .distinct().toList();
            loadBalancer.setServersList(servers);
            server = servers.isEmpty() ? null : loadBalancer.chooseServer(null);
        }
        if (server == null) {
            throw new ResourceAccessException("No instances available for " + serviceId);
        }
        URI resolved = UriComponentsBuilder.fromUri(original)
                .scheme(server.getScheme()).host(server.getHost()).port(server.getPort())
                .build(true).toUri();
        return execution.execute(new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                return resolved;
            }
        }, body);
    }
}
