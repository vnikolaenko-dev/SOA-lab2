package soa.lab2.collection.model;

public record SpaceMarinePatch(Coordinates coordinates, Integer health, Weapon weaponType,
                               MeleeWeapon meleeWeapon, boolean meleeWeaponProvided) {
}
