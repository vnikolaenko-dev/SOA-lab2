package soa.lab2.collection.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.*;

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
    void initializeCreationDate() { if (creationDate == null) creationDate = OffsetDateTime.now(ZoneOffset.UTC); }

}
