package soa.lab2.collection.mapper;

import org.springframework.stereotype.Component;
import soa.lab2.collection.dto.ChapterDTO;
import soa.lab2.collection.dto.CoordinatesDTO;
import soa.lab2.collection.dto.MeleeWeaponDTO;
import soa.lab2.collection.dto.SpaceMarineDTO;
import soa.lab2.collection.dto.SpaceMarineInputDTO;
import soa.lab2.collection.dto.SpaceMarinePatchInputDTO;
import soa.lab2.collection.dto.WeaponDTO;
import soa.lab2.collection.model.Chapter;
import soa.lab2.collection.model.Coordinates;
import soa.lab2.collection.model.MeleeWeapon;
import soa.lab2.collection.model.SpaceMarine;
import soa.lab2.collection.model.SpaceMarinePatch;
import soa.lab2.collection.model.Weapon;

import java.util.Set;

@Component
public class SpaceMarineMapper {
    public SpaceMarine toModel(SpaceMarineInputDTO input) {
        return update(input, SpaceMarine.builder().build());
    }

    public SpaceMarine update(SpaceMarineInputDTO input, SpaceMarine model) {
        if (!Double.isFinite(input.getCoordinates().getX())) {
            throw new IllegalArgumentException("Координата coordinates.x должна быть конечным числом");
        }
        var coordinates = Coordinates.builder()
                .withX(input.getCoordinates().getX())
                .withY(input.getCoordinates().getY()).build();
        var chapter = Chapter.builder()
                .withName(input.getChapter().getName())
                .withParentLegion(input.getChapter().getParentLegion())
                .withWorld(input.getChapter().getWorld()).build();
        return model.toBuilder()
                .withName(input.getName())
                .withCoordinates(coordinates)
                .withHealth(input.getHealth())
                .withAchievements(input.getAchievements())
                .withWeaponType(Weapon.valueOf(input.getWeaponType().getValue()))
                .withMeleeWeapon(input.getMeleeWeapon() == null ? null :
                        MeleeWeapon.valueOf(input.getMeleeWeapon().getValue()))
                .withChapter(chapter).build();
    }

    public SpaceMarinePatch toPatch(SpaceMarinePatchInputDTO input, Set<String> providedFields) {
        for (String field : Set.of("coordinates", "health", "weaponType")) {
            if (providedFields.contains(field) && switch (field) {
                case "coordinates" -> input.getCoordinates() == null;
                case "health" -> input.getHealth() == null;
                default -> input.getWeaponType() == null;
            }) {
                throw new IllegalArgumentException("Поле " + field + " не может быть null");
            }
        }
        Coordinates coordinates = null;
        if (input.getCoordinates() != null) {
            if (!Double.isFinite(input.getCoordinates().getX()))
                throw new IllegalArgumentException("Координата coordinates.x должна быть конечным числом");
            coordinates = Coordinates.builder().withX(input.getCoordinates().getX())
                    .withY(input.getCoordinates().getY()).build();
        }
        return new SpaceMarinePatch(coordinates, input.getHealth(),
                input.getWeaponType() == null ? null : Weapon.valueOf(input.getWeaponType().getValue()),
                input.getMeleeWeapon() == null ? null : MeleeWeapon.valueOf(input.getMeleeWeapon().getValue()),
                providedFields.contains("meleeWeapon"));
    }

    public SpaceMarineDTO toDto(SpaceMarine model) {
        return new SpaceMarineDTO()
                .id(model.getId()).creationDate(model.getCreationDate()).name(model.getName())
                .coordinates(new CoordinatesDTO().x(model.getCoordinates().getX()).y(model.getCoordinates().getY()))
                .health(model.getHealth()).achievements(model.getAchievements())
                .weaponType(WeaponDTO.fromValue(model.getWeaponType().name()))
                .meleeWeapon(model.getMeleeWeapon() == null ? null : MeleeWeaponDTO.fromValue(model.getMeleeWeapon().name()))
                .chapter(new ChapterDTO().name(model.getChapter().getName())
                        .parentLegion(model.getChapter().getParentLegion()).world(model.getChapter().getWorld()));
    }
}
