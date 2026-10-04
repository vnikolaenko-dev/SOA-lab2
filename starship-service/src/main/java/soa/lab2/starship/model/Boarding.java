package soa.lab2.starship.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.*;

@Entity
@Table(name = "boarding", uniqueConstraints = @UniqueConstraint(name = "boarding_marine_unique", columnNames = "marine_id"))
@Getter
@With
@Builder(toBuilder = true, setterPrefix = "with")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Boarding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "marine_id", nullable = false)
    private Long marineId;
    @Column(name = "starship_id", nullable = false)
    private Long starshipId;
    @Column(name = "loaded_at", nullable = false, updatable = false)
    private OffsetDateTime loadedAt;

    @PrePersist
    void initializeLoadedAt() { if (loadedAt == null) loadedAt = OffsetDateTime.now(ZoneOffset.UTC); }

}
