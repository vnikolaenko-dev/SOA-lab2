package soa.lab2.starship.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.client.CollectionClient;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.model.SpaceMarine;
import soa.lab2.starship.repository.BoardingRepository;

@Service
public class StarshipService {
    private final BoardingRepository repository;
    private final CollectionClient collection;
    private final StarshipMapper mapper;

    public StarshipService(BoardingRepository repository, CollectionClient collection, StarshipMapper mapper) {
        this.repository = repository;
        this.collection = collection;
        this.mapper = mapper;
    }

    @Transactional
    public SpaceMarine load(int starshipId, int marineId) {
        SpaceMarine marine = collection.getMarine(marineId);
        if (repository.existsByMarineId((long) marineId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Десантник уже находится на корабле");
        }
        // Ограничение уникальности в БД также предотвращает одновременную посадку на разные корабли.
        repository.saveAndFlush(mapper.toBoarding(starshipId, marineId));
        return marine;
    }

    @Transactional
    public int unloadAll(long starshipId) { return repository.deleteAllByStarshipId(starshipId); }
}
