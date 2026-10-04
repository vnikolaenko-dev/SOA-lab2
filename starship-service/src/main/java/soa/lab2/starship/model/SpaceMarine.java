package soa.lab2.starship.model;

import java.time.OffsetDateTime;

// Снимок десантника, полученный из API коллекции; десантников хранит первый сервис.
public record SpaceMarine(Long id, String name, Coordinates coordinates, OffsetDateTime creationDate,
                          Integer health, String achievements, String weaponType,
                          String meleeWeapon, Chapter chapter) {
    public record Coordinates(Double x, Integer y) {}
    public record Chapter(String name, String parentLegion, String world) {}
}
