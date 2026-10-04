package soa.lab2.collection.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.Weapon;

public interface SpaceMarineRepository extends JpaRepository<SpaceMarine, Long>, JpaSpecificationExecutor<SpaceMarine> {
    long deleteByHealth(Integer health);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SpaceMarine> findFirstByWeaponTypeOrderByIdAsc(Weapon weaponType);

    @Modifying
    @Query("delete from SpaceMarine m where m.id = :id")
    int deleteMarineById(@Param("id") Long id);

    Optional<SpaceMarine> findFirstByOrderByNameDescIdAsc();
}
