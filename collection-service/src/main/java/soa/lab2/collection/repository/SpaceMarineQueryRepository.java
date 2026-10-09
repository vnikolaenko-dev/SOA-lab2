package soa.lab2.collection.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import soa.lab2.collection.model.SpaceMarine;

import java.util.Optional;

/** Доступ к коллекции для запросов: операции изменения данных отсутствуют. */
@NoRepositoryBean
public interface SpaceMarineQueryRepository extends Repository<SpaceMarine, Long> {
    Optional<SpaceMarine> findById(Long id);

    Optional<SpaceMarine> findFirstByOrderByNameDescIdAsc();

    Page<SpaceMarine> findAll(Specification<SpaceMarine> specification, Pageable pageable);
}
