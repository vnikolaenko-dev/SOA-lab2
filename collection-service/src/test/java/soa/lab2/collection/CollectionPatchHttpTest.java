package soa.lab2.collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import soa.lab2.collection.model.Chapter;
import soa.lab2.collection.model.Coordinates;
import soa.lab2.collection.model.MeleeWeapon;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.SpaceMarinePatch;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.service.CollectionCommandService;
import soa.lab2.collection.service.CollectionQueryService;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class CollectionPatchHttpTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    CollectionCommandService service;
    @MockitoBean
    CollectionQueryService queries;

    @Test
    void explicitNullClearsMeleeWeaponButOmittedFieldPreservesIt() throws Exception {
        var marine = SpaceMarine.builder().withId(7L).withName("Marine")
                .withCreationDate(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .withCoordinates(Coordinates.builder().withX(1.0).withY(2).build())
                .withHealth(100).withWeaponType(Weapon.COMBI_FLAMER)
                .withMeleeWeapon(MeleeWeapon.CHAIN_SWORD)
                .withChapter(Chapter.builder().withName("Chapter").withWorld("Earth").build()).build();
        when(service.patch(eq(7L), any(SpaceMarinePatch.class))).thenReturn(marine);

        mvc.perform(patch("/api/v1/space-marines/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"health\":80}"))
                .andExpect(status().isOk());
        var first = org.mockito.ArgumentCaptor.forClass(SpaceMarinePatch.class);
        verify(service).patch(eq(7L), first.capture());
        assertFalse(first.getValue().meleeWeaponProvided());
        assertEquals(80, first.getValue().health());

        mvc.perform(patch("/api/v1/space-marines/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meleeWeapon\":null}"))
                .andExpect(status().isOk());
        var second = org.mockito.ArgumentCaptor.forClass(SpaceMarinePatch.class);
        verify(service, times(2)).patch(eq(7L), second.capture());
        assertTrue(second.getAllValues().getLast().meleeWeaponProvided());
        assertNull(second.getAllValues().getLast().meleeWeapon());
    }

    @Test
    void patchRejectsForbiddenAndNullRequiredFields() throws Exception {
        mvc.perform(patch("/api/v1/space-marines/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Changed\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/space-marines/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"health\":null}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/space-marines/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
