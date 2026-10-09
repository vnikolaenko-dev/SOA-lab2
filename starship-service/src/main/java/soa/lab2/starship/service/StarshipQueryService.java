package soa.lab2.starship.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soa.lab2.starship.client.CollectionClient;
import soa.lab2.starship.model.SpaceMarine;
import soa.lab2.starship.repository.BoardingQueryRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StarshipQueryService {
    private final BoardingQueryRepository repository;
    private final CollectionClient collection;

    public StarshipQueryService(BoardingQueryRepository repository, CollectionClient collection) {
        this.repository = repository;
        this.collection = collection;
    }

    public Page<SpaceMarine> listCrew(long starshipId, Integer page, Integer size, List<String> filters) {
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        if (p < 0 || s < 1 || s > 100)
            throw new IllegalArgumentException("Номер страницы должен быть неотрицательным, размер - от 1 до 100");
        var conditions = CrewFilters.parse(filters);
        var marines = repository.findAllByStarshipIdOrderByIdAsc(starshipId).stream()
                .map(boarding -> collection.getMarine(boarding.getMarineId()))
                .filter(marine -> CrewFilters.matches(marine, conditions))
                .toList();
        int from = (int) Math.min((long) p * s, marines.size());
        int to = Math.min(from + s, marines.size());
        return new PageImpl<>(marines.subList(from, to), PageRequest.of(p, s), marines.size());
    }

    public Page<Long> listStarships(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        if (p < 0 || s < 1 || s > 100)
            throw new IllegalArgumentException("Номер страницы должен быть неотрицательным, размер - от 1 до 100");
        return repository.findStarshipIds(PageRequest.of(p, s));
    }
}
