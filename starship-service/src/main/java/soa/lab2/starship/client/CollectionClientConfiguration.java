package soa.lab2.starship.client;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class CollectionClientConfiguration {
    @Bean(destroyMethod = "shutdown")
    BaseLoadBalancer collectionRibbonLoadBalancer() {
        var balancer = new BaseLoadBalancer();
        balancer.setRule(new RoundRobinRule());
        return balancer;
    }

    @Bean
    RestClient.Builder collectionRestClientBuilder(DiscoveryClient discovery,
            BaseLoadBalancer collectionRibbonLoadBalancer,
            @Value("${collection.base-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory)
                .requestInterceptor(new RibbonRequestInterceptor(discovery, collectionRibbonLoadBalancer,
                        java.net.URI.create(baseUrl).getHost()));
    }
}
