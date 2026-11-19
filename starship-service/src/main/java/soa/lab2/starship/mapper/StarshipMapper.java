package soa.lab2.starship.mapper;

import org.springframework.stereotype.Component;
import soa.lab2.starship.dto.ChapterDTO;
import soa.lab2.starship.dto.CoordinatesDTO;
import soa.lab2.starship.dto.SpaceMarineDTO;
import soa.lab2.starship.model.Boarding;
import soa.lab2.starship.model.SpaceMarine;
import soa.lab2.starship.model.SpaceMarine.Chapter;
import soa.lab2.starship.model.SpaceMarine.Coordinates;

@Component
public class StarshipMapper {
    public Boarding toBoarding(long starshipId, long marineId) {
        return Boarding.builder().withMarineId(marineId).withStarshipId(starshipId).build();
    }

    public SpaceMarine toModel(SpaceMarineDTO dto) {
        return new SpaceMarine(dto.getId(), dto.getName(),
                dto.getCoordinates() == null ? null : new Coordinates(dto.getCoordinates().getX(), dto.getCoordinates().getY()),
                dto.getCreationDate() == null ? null : java.time.OffsetDateTime.parse(dto.getCreationDate()), dto.getHealth(), dto.getAchievements(),
                dto.getWeaponType(),
                dto.getMeleeWeapon(),
                dto.getChapter() == null ? null : new Chapter(dto.getChapter().getName(), dto.getChapter().getParentLegion(), dto.getChapter().getWorld()));
    }

    public SpaceMarineDTO toDto(SpaceMarine model) {
        return new SpaceMarineDTO().id(model.id()).name(model.name())
                .creationDate(model.creationDate() == null ? null : java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(model.creationDate())).health(model.health()).achievements(model.achievements())
                .coordinates(model.coordinates() == null ? null : new CoordinatesDTO().x(model.coordinates().x()).y(model.coordinates().y()))
                .weaponType(model.weaponType())
                .meleeWeapon(model.meleeWeapon())
                .chapter(model.chapter() == null ? null : new ChapterDTO().name(model.chapter().name())
                        .parentLegion(model.chapter().parentLegion()).world(model.chapter().world()));
    }
}
