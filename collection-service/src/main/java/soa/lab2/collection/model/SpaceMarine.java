package soa.lab2.collection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "space_marine")
@Getter
@With
@Builder(toBuilder = true, setterPrefix = "with")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SpaceMarine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "creation_date", nullable = false, updatable = false)
    private OffsetDateTime creationDate;
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    @Embedded
    private Coordinates coordinates;
    @Column(nullable = false)
    private Integer health;
    @Column(columnDefinition = "text")
    private String achievements;
    @Enumerated(EnumType.STRING)
    @Column(name = "weapon_type", nullable = false, columnDefinition = "text")
    private Weapon weaponType;
    @Enumerated(EnumType.STRING)
    @Column(name = "melee_weapon", columnDefinition = "text")
    private MeleeWeapon meleeWeapon;
    @Embedded
    private Chapter chapter;

    @PrePersist
    void initializeCreationDate() {
        if (creationDate == null) creationDate = OffsetDateTime.now(ZoneOffset.UTC);
    }

}
