package soa.lab2.collection.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Embeddable
@Getter
@With
@Builder(toBuilder = true, setterPrefix = "with")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Coordinates {
    @Column(name = "coordinate_x", nullable = false)
    private Double x;
    @Column(name = "coordinate_y", nullable = false)
    private Integer y;

}
