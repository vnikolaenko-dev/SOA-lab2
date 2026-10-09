package soa.lab2.collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import soa.lab2.collection.service.CollectionCommandService;
import soa.lab2.collection.service.CollectionQueryService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class SwaggerUiTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    CollectionCommandService service;
    @MockitoBean
    CollectionQueryService queries;

    @Test
    void servesUiAndUsesContract() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/openapi.yaml"));
    }
}
