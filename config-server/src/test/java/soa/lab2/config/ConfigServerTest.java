package soa.lab2.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.config.environment.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerTest {
    @Autowired
    TestRestTemplate http;

    @Test
    void servesSharedAndServiceSpecificConfiguration() {
        for (String service : new String[]{"collection-service", "starship-service"}) {
            Environment environment = http.getForObject("/" + service + "/default", Environment.class);
            assertThat(environment).isNotNull();
            assertThat(environment.getName()).isEqualTo(service);
            assertThat(environment.getPropertySources()).anySatisfy(source ->
                    assertThat(source.getSource().get("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate"));
            assertThat(environment.getPropertySources()).anySatisfy(source ->
                    assertThat(source.getSource().get("spring.flyway.default-schema")).isEqualTo(
                            service.equals("collection-service") ? "collection" : "starship"));
            assertThat(environment.getPropertySources()).anySatisfy(source ->
                    assertThat(source.getSource().get("spring.datasource.password")).isEqualTo("${DB_PASSWORD}"));
        }
    }
}
