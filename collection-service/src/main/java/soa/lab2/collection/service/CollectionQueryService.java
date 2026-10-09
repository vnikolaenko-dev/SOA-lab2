package soa.lab2.collection.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.repository.SpaceMarineQueryRepository;
import soa.lab2.collection.repository.SpaceMarineSpecifications;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CollectionQueryService {
    private final SpaceMarineQueryRepository repository;

    public CollectionQueryService(SpaceMarineQueryRepository repository) {
        this.repository = repository;
    }

    public SpaceMarine get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Десантник не найден"));
    }

    public SpaceMarine getMaxName() {
        return repository.findFirstByOrderByNameDescIdAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Коллекция пуста"));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<SpaceMarine> list(Integer page, Integer size, List<String> sort, List<String> filter) {
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        if (p < 0 || s < 1 || s > 100)
            throw new IllegalArgumentException("Номер страницы должен быть неотрицательным, размер - от 1 до 100");
        return repository.findAll(SpaceMarineSpecifications.filters(filter),
                PageRequest.of(p, s, SpaceMarineSpecifications.sort(sort)));
    }
}
