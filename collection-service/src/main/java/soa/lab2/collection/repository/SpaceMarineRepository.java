package soa.lab2.collection.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.Weapon;

import java.util.Optional;

public interface SpaceMarineRepository extends JpaRepository<SpaceMarine, Long>, JpaSpecificationExecutor<SpaceMarine>, SpaceMarineQueryRepository {
    @Override
    Optional<SpaceMarine> findById(Long id);

    long deleteByHealth(Integer health);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SpaceMarine> findFirstByWeaponTypeOrderByIdAsc(Weapon weaponType);

    @Modifying
    @Query("delete from SpaceMarine m where m.id = :id")
    int deleteMarineById(@Param("id") Long id);

    Optional<SpaceMarine> findFirstByOrderByNameDescIdAsc();
}
