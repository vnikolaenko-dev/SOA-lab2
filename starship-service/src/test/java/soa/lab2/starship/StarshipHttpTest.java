package soa.lab2.starship;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import soa.lab2.starship.controller.StarshipControllerDelegate;
import soa.lab2.starship.controller.generated.StarshipApiController;
import soa.lab2.starship.dto.PageOfSpaceMarineDTO;
import soa.lab2.starship.dto.PageOfStarshipDTO;
import soa.lab2.starship.dto.StarshipDTO;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StarshipApiController.class)
class StarshipHttpTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    StarshipControllerDelegate delegate;

    @Test
    void crewRouteUsesDelegate() throws Exception {
        when(delegate.getStarshipSpaceMarines(eq(3), eq(0), eq(10), any()))
                .thenReturn(ResponseEntity.ok(new PageOfSpaceMarineDTO()
                        .content(List.of()).page(0).size(10).totalElements(0).totalPages(0)));
        mvc.perform(get("/api/v1/starship/3/space-marines"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        verify(delegate).getStarshipSpaceMarines(eq(3), eq(0), eq(10), any());
    }

    @Test
    void starshipsRouteUsesDelegate() throws Exception {
        when(delegate.getStarships(1, 2)).thenReturn(ResponseEntity.ok(new PageOfStarshipDTO()
                .content(List.of(new StarshipDTO().id(3L)))
                .page(1).size(2).totalElements(3).totalPages(2)));
        mvc.perform(get("/api/v1/starships?page=1&size=2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
        verify(delegate).getStarships(1, 2);
    }
}
