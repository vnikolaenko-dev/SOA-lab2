package soa.lab2.starship.dto;

public enum WeaponDTO {
    COMBI_FLAMER, GRENADE_LAUNCHER, HEAVY_FLAMER;
    public String getValue() { return name(); }
    public static WeaponDTO fromValue(String value) { return valueOf(value); }
}
