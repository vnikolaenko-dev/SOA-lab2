package soa.lab2.starship.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import soa.lab2.starship.model.Boarding;

import java.util.List;

/** Доступ к посадкам для запросов: операции изменения данных отсутствуют. */
@NoRepositoryBean
public interface BoardingQueryRepository extends Repository<Boarding, Long> {
    List<Boarding> findAllByStarshipIdOrderByIdAsc(Long starshipId);

    Page<Long> findStarshipIds(Pageable pageable);
}
