package soa.lab2.starship.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import soa.lab2.starship.model.Boarding;

public interface BoardingRepository extends JpaRepository<Boarding, Long> {
    boolean existsByMarineId(Long marineId);

    @Modifying
    @Query("delete from Boarding b where b.starshipId = :starshipId")
    int deleteAllByStarshipId(@Param("starshipId") Long starshipId);
}
