package soa.lab2.adapter;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import soa.lab2.starship.soap.StarshipSoapPort;

@Configuration
public class SoapClientConfiguration {
    @Bean
    StarshipSoapPort starshipSoapPort(@Value("${starship.soap-url}") String url) {
        var factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(StarshipSoapPort.class);
        factory.setAddress(url);
        var port = (StarshipSoapPort) factory.create();
        var conduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        var policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(3000);
        policy.setReceiveTimeout(15000);
        policy.setAutoRedirect(false);
        conduit.setClient(policy);
        return port;
    }
}
