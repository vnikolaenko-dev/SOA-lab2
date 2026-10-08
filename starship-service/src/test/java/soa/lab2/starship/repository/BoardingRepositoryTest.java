package soa.lab2.starship.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import soa.lab2.starship.mapper.StarshipMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"})
class BoardingRepositoryTest {
    private final StarshipMapper mapper = new StarshipMapper();
    @Autowired
    BoardingRepository repository;

    @Test
    void persistsBoardingAndUnloadsOnlyRequestedShip() {
        var boarding = repository.saveAndFlush(mapper.toBoarding(2, 7));
        assertNotNull(boarding.getId());
        assertNotNull(boarding.getLoadedAt());
        repository.saveAndFlush(mapper.toBoarding(3, 8));
        assertTrue(repository.existsByMarineId(7L));
        assertEquals(1, repository.findAllByStarshipIdOrderByIdAsc(2L).size());
        assertEquals(7L, repository.findAllByStarshipIdOrderByIdAsc(2L).getFirst().getMarineId());
        assertEquals(1, repository.deleteAllByStarshipId(2L));
        assertFalse(repository.existsByMarineId(7L));
        assertTrue(repository.existsByMarineId(8L));
        assertEquals(0, repository.deleteAllByStarshipId(99L));
    }

    @Test
    void databaseConstraintPreventsDoubleBoarding() {
        repository.saveAndFlush(mapper.toBoarding(2, 7));
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapper.toBoarding(3, 7)));
    }

    @Test
    void listsDistinctStarshipIdsWithPagination() {
        repository.saveAndFlush(mapper.toBoarding(3, 7));
        repository.saveAndFlush(mapper.toBoarding(1, 8));
        repository.saveAndFlush(mapper.toBoarding(3, 9));
        repository.saveAndFlush(mapper.toBoarding(2, 10));

        var first = repository.findStarshipIds(PageRequest.of(0, 2));
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(java.util.List.of(1L, 2L), first.getContent());
        assertEquals(java.util.List.of(3L), repository.findStarshipIds(PageRequest.of(1, 2)).getContent());
    }
}
