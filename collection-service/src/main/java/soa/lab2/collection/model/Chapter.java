package soa.lab2.collection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Embeddable
@Getter
@With
@Builder(toBuilder = true, setterPrefix = "with")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chapter {
    @Column(name = "chapter_name", nullable = false, columnDefinition = "text")
    private String name;
    @Column(name = "chapter_parent_legion", columnDefinition = "text")
    private String parentLegion;
    @Column(name = "chapter_world", nullable = false, columnDefinition = "text")
    private String world;

}
