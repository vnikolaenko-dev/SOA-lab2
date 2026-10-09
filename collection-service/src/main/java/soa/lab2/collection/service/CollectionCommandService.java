package soa.lab2.collection.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.SpaceMarinePatch;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.repository.SpaceMarineRepository;


@Service
@Transactional
public class CollectionCommandService {
    private final SpaceMarineRepository repository;

    public CollectionCommandService(SpaceMarineRepository repository) {
        this.repository = repository;
    }

    public SpaceMarine create(SpaceMarine marine) {
        return repository.save(marine);
    }

    private SpaceMarine findMarineById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Десантник не найден"));
    }

    public SpaceMarine update(long id, SpaceMarine changes) {
        SpaceMarine marine = findMarineById(id);
        SpaceMarine updated = marine.toBuilder()
                .withName(changes.getName())
                .withCoordinates(changes.getCoordinates())
                .withHealth(changes.getHealth())
                .withAchievements(changes.getAchievements())
                .withWeaponType(changes.getWeaponType())
                .withMeleeWeapon(changes.getMeleeWeapon())
                .withChapter(changes.getChapter())
                .build();
        return repository.save(updated);
    }

    public SpaceMarine patch(long id, SpaceMarinePatch changes) {
        SpaceMarine marine = findMarineById(id);
        SpaceMarine updated = marine.toBuilder()
                .withCoordinates(changes.coordinates() == null ? marine.getCoordinates() : changes.coordinates())
                .withHealth(changes.health() == null ? marine.getHealth() : changes.health())
                .withWeaponType(changes.weaponType() == null ? marine.getWeaponType() : changes.weaponType())
                .withMeleeWeapon(changes.meleeWeaponProvided() ? changes.meleeWeapon() : marine.getMeleeWeapon())
                .build();
        return repository.save(updated);
    }

    public void delete(long id) {
        repository.deleteMarineById(id);
    }

    public int deleteByHealth(int health) {
        return Math.toIntExact(repository.deleteByHealth(health));
    }

    public void deleteOneByWeapon(Weapon weapon) {
        repository.findFirstByWeaponTypeOrderByIdAsc(weapon).ifPresent(repository::delete);
    }

}
