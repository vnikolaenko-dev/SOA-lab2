package soa.lab2.starship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soa.lab2.starship.model.Boarding;

import java.util.List;

public interface BoardingRepository extends JpaRepository<Boarding, Long> {
    boolean existsByMarineId(Long marineId);

    List<Boarding> findAllByStarshipIdOrderByIdAsc(Long starshipId);

    @Query(value = "select distinct b.starshipId from Boarding b order by b.starshipId",
            countQuery = "select count(distinct b.starshipId) from Boarding b")
    Page<Long> findStarshipIds(Pageable pageable);

    @Modifying
    @Query("delete from Boarding b where b.starshipId = :starshipId")
    int deleteAllByStarshipId(@Param("starshipId") Long starshipId);
}
