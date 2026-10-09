package soa.lab2.collection.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.SpaceMarinePatch;
import soa.lab2.collection.model.MeleeWeapon;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.repository.SpaceMarineRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectionServiceTest {
    @Test
    void updatePreservesIdentityAndCreationDate() {
        var repository = mock(SpaceMarineRepository.class);
        var date = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var existing = SpaceMarine.builder().withId(7L).withCreationDate(date).withName("Original").build();
        var changes = SpaceMarine.builder().withId(99L).withCreationDate(date.plusDays(1))
                .withName("Updated").withHealth(100).build();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SpaceMarine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = new CollectionService(repository).update(7, changes);
        assertEquals(7L, result.getId());
        assertEquals(date, result.getCreationDate());
        assertEquals("Updated", result.getName());
        assertEquals(100, result.getHealth());
        assertNotSame(existing, result);
        assertEquals("Original", existing.getName());
    }

    @Test
    void patchChangesOnlyProvidedFieldsAndCanClearMeleeWeapon() {
        var repository = mock(SpaceMarineRepository.class);
        var date = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var existing = SpaceMarine.builder().withId(7L).withCreationDate(date).withName("Original")
                .withHealth(100).withWeaponType(Weapon.COMBI_FLAMER)
                .withMeleeWeapon(MeleeWeapon.CHAIN_SWORD).build();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SpaceMarine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new CollectionService(repository);
        var changed = service.patch(7, new SpaceMarinePatch(null, 80, null, null, false));
        assertEquals(7L, changed.getId());
        assertEquals(date, changed.getCreationDate());
        assertEquals("Original", changed.getName());
        assertEquals(80, changed.getHealth());
        assertEquals(Weapon.COMBI_FLAMER, changed.getWeaponType());
        assertEquals(MeleeWeapon.CHAIN_SWORD, changed.getMeleeWeapon());

        var cleared = service.patch(7, new SpaceMarinePatch(null, null, null, null, true));
        assertNull(cleared.getMeleeWeapon());
    }
}
