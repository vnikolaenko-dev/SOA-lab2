package soa.lab2.collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import soa.lab2.collection.controller.CollectionApiDelegate;
import soa.lab2.collection.controller.generated.SpaceMarinesCollectionApiController;
import soa.lab2.collection.dto.SpaceMarineDTO;
import soa.lab2.collection.dto.SpaceMarinePatchInputDTO;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpaceMarinesCollectionApiController.class)
class CollectionHttpTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    CollectionApiDelegate service;

    @Test
    void generatedRouteUsesDelegate() throws Exception {
        when(service.getSpaceMarineById(1)).thenReturn(ResponseEntity.ok(new SpaceMarineDTO().id(1L).name("Marine")));
        mvc.perform(get("/api/v1/space-marines/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        verify(service).getSpaceMarineById(1);
    }

    @Test
    void invalidNestedDtoIsRejectedBeforeDelegate() throws Exception {
        mvc.perform(post("/api/v1/space-marines").contentType("application/json").content("""
                {"name":"Marine","coordinates":{"x":1,"y":-630},"health":0,"weaponType":"COMBI_FLAMER","chapter":{"name":"Chapter","world":"Earth"}}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verify(service, never()).addSpaceMarine(any());
    }

    @Test
    void patchRouteUsesDelegate() throws Exception {
        when(service.patchSpaceMarine(eq(1), any(SpaceMarinePatchInputDTO.class)))
                .thenReturn(ResponseEntity.ok(new SpaceMarineDTO().id(1L).name("Marine").health(80)));
        mvc.perform(patch("/api/v1/space-marines/1").contentType("application/json")
                        .content("{\"health\":80}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.health").value(80));
        verify(service).patchSpaceMarine(eq(1), any(SpaceMarinePatchInputDTO.class));
    }

    @Test
    void patchRejectsInvalidHealth() throws Exception {
        mvc.perform(patch("/api/v1/space-marines/1").contentType("application/json")
                        .content("{\"health\":0}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).patchSpaceMarine(any(), any());
    }

    @Test
    void malformedJsonUsesRussianMessage() throws Exception {
        mvc.perform(post("/api/v1/space-marines").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Некорректное тело запроса или параметры"));
    }

    @Test
    void domainExceptionUsesRussianMessage() throws Exception {
        when(service.getSpaceMarineById(1)).thenThrow(new IllegalArgumentException("Некорректное значение параметра"));
        mvc.perform(get("/api/v1/space-marines/1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Некорректное значение параметра"));
    }
}
