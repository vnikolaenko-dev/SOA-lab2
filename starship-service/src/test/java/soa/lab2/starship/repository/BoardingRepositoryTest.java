package soa.lab2.starship.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
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
}
