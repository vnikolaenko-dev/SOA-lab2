package soa.lab2.collection.service;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.repository.SpaceMarineRepository;
import soa.lab2.collection.repository.SpaceMarineSpecifications;

@Service
@Transactional
public class CollectionService {
    private final SpaceMarineRepository repository;

    public CollectionService(SpaceMarineRepository repository) { this.repository = repository; }

    public SpaceMarine create(SpaceMarine marine) { return repository.save(marine); }

    @Transactional(readOnly = true)
    public SpaceMarine get(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Десантник не найден"));
    }

    public SpaceMarine update(long id, SpaceMarine changes) {
        SpaceMarine marine = get(id);
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

    public void delete(long id) { repository.deleteMarineById(id); }
    public int deleteByHealth(int health) { return Math.toIntExact(repository.deleteByHealth(health)); }
    public void deleteOneByWeapon(Weapon weapon) {
        repository.findFirstByWeaponTypeOrderByIdAsc(weapon).ifPresent(repository::delete);
    }

    @Transactional(readOnly = true)
    public SpaceMarine getMaxName() {
        return repository.findFirstByOrderByNameDescIdAsc().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Коллекция пуста"));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<SpaceMarine> list(Integer page, Integer size, List<String> sort, List<String> filter) {
        int p = page == null ? 0 : page, s = size == null ? 10 : size;
        if (p < 0 || s < 1 || s > 100) throw new IllegalArgumentException("Номер страницы должен быть неотрицательным, размер — от 1 до 100");
        return repository.findAll(SpaceMarineSpecifications.filters(filter),
                PageRequest.of(p, s, SpaceMarineSpecifications.sort(sort)));
    }
}
