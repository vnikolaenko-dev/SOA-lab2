package soa.lab2.starship.soap;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SoapConfiguration {
    @Bean
    public Endpoint starshipEndpoint(Bus bus, StarshipSoapEndpoint service) {
        EndpointImpl endpoint = new EndpointImpl(bus, service);
        endpoint.publish("/starships");
        return endpoint;
    }
}
