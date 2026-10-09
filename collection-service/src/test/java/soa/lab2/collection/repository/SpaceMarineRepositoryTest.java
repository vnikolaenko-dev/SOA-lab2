package soa.lab2.collection.repository;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.collection.service.CollectionCommandService;
import soa.lab2.collection.service.CollectionQueryService;
import soa.lab2.collection.dto.*;
import soa.lab2.collection.mapper.SpaceMarineMapper;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.Weapon;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"})
@Import({CollectionCommandService.class, CollectionQueryService.class})
class SpaceMarineRepositoryTest {
    @Autowired
    CollectionCommandService commands;
    @Autowired
    CollectionQueryService queries;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void committedCommandsAreVisibleToQueries() {
        var created = commands.create(marine("CQRS", 100));
        try {
            assertEquals("CQRS", queries.get(created.getId()).getName());
            commands.update(created.getId(), marine("CQRS updated", 200));
            assertEquals(200, queries.get(created.getId()).getHealth());
            var page = queries.list(0, 10, null, List.of("id,eq," + created.getId()));
            assertEquals(1, page.getTotalElements());
            assertEquals("CQRS updated", page.getContent().getFirst().getName());
        } finally {
            commands.delete(created.getId());
        }
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> queries.get(created.getId())).getStatusCode().value());
    }
    @Autowired
    SpaceMarineRepository repository;
    @Autowired
    EntityManager entityManager;
    private final SpaceMarineMapper mapper = new SpaceMarineMapper();

    private SpaceMarine marine(String name, int health) {
        return mapper.toModel(new SpaceMarineInputDTO().name(name).health(health)
                .coordinates(new CoordinatesDTO().x(1.5).y(10)).weaponType(WeaponDTO.COMBI_FLAMER)
                .chapter(new ChapterDTO().name("ChapterDTO").world("Earth")));
    }

    @Test
    void persistsEmbeddablesAndMapsDtoAfterReload() {
        var saved = repository.saveAndFlush(marine("Alpha", 100));
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreationDate());
        entityManager.clear();
        var dto = mapper.toDto(repository.findById(saved.getId()).orElseThrow());
        assertEquals("Alpha", dto.getName());
        assertEquals(1.5, dto.getCoordinates().getX());
        assertEquals("Earth", dto.getChapter().getWorld());
        assertEquals(WeaponDTO.COMBI_FLAMER, dto.getWeaponType());
        assertNull(dto.getMeleeWeapon());
    }

    @Test
    void filtersNestedFieldsAndEnumsAndPaginates() {
        repository.save(marine("Alpha", 100));
        repository.save(marine("Bravo", 200));
        repository.saveAndFlush(marine("Charlie", 20));
        var result = repository.findAll(SpaceMarineSpecifications.filters(List.of(
                        "health,gt,50", "coordinates.y,eq,10", "chapter.world,eq,Earth", "weaponType,eq,COMBI_FLAMER")),
                PageRequest.of(0, 1, SpaceMarineSpecifications.sort(List.of("health,desc"))));
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals("Bravo", result.getContent().getFirst().getName());
    }

    @Test
    void orderedEnumFiltersUseNames() {
        repository.saveAndFlush(marine("Alpha", 100));
        assertEquals(1, repository.count(SpaceMarineSpecifications.filters(List.of("weaponType,lt,HEAVY_FLAMER"))));
    }

    @Test
    void builderUpdateAndWithCopiesPersistWithoutCreatingAnotherRow() {
        var saved = repository.saveAndFlush(marine("Original", 100));
        entityManager.refresh(saved);
        Long id = saved.getId();
        var date = saved.getCreationDate();
        var changed = saved.toBuilder().withName("Updated").withAchievements("Award").build()
                .withHealth(200).withCoordinates(saved.getCoordinates().withY(25));
        assertNotSame(saved, changed);
        assertEquals("Original", saved.getName());
        assertEquals(100, saved.getHealth());
        assertEquals(10, saved.getCoordinates().getY());
        repository.saveAndFlush(changed);
        entityManager.clear();
        var reloaded = repository.findById(id).orElseThrow();
        assertEquals(1, repository.count());
        assertEquals(date.toInstant(), reloaded.getCreationDate().toInstant());
        assertEquals("Updated", reloaded.getName());
        assertEquals("Award", reloaded.getAchievements());
        assertEquals(200, reloaded.getHealth());
        assertEquals(25, reloaded.getCoordinates().getY());
    }

    @Test
    void additionalOperationsUseRepository() {
        repository.save(marine("Alpha", 100));
        repository.saveAndFlush(marine("Zulu", 100));
        assertEquals("Zulu", repository.findFirstByOrderByNameDescIdAsc().orElseThrow().getName());
        var selected = repository.findFirstByWeaponTypeOrderByIdAsc(Weapon.COMBI_FLAMER).orElseThrow();
        repository.delete(selected);
        repository.flush();
        assertEquals(1, repository.deleteByHealth(100));
        repository.flush();
        assertEquals(0, repository.count());
        assertEquals(0, repository.deleteMarineById(999L));
    }

    @Test
    void invalidFilterAndSortFailBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> SpaceMarineSpecifications.filters(List.of("missing,eq,1")));
        assertThrows(IllegalArgumentException.class, () -> SpaceMarineSpecifications.filters(List.of("health,gt,no")));
        assertThrows(IllegalArgumentException.class, () -> SpaceMarineSpecifications.sort(List.of("name,invalid")));
    }

    @Test
    void generatedEnumAndFilterExceptionsAreRussian() {
        var enumError = assertThrows(IllegalArgumentException.class, () -> WeaponDTO.fromValue("UNKNOWN"));
        assertEquals("Недопустимое значение 'UNKNOWN'", enumError.getMessage());
        var filterError = assertThrows(IllegalArgumentException.class,
                () -> SpaceMarineSpecifications.filters(List.of("health,gt,no")));
        assertEquals("Некорректное значение фильтра для поля health: no", filterError.getMessage());
    }
}
