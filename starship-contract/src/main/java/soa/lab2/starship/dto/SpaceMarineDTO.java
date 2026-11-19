package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpaceMarineDTO", namespace = "urn:soa:starship:v1")
public class SpaceMarineDTO {
    private Long id;
    private String name;
    private CoordinatesDTO coordinates;
    private String creationDate;
    private Integer health;
    private String achievements;
    private String weaponType;
    private String meleeWeapon;
    private ChapterDTO chapter;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public SpaceMarineDTO id(Long value) { this.id = value; return this; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public SpaceMarineDTO name(String value) { this.name = value; return this; }
    public CoordinatesDTO getCoordinates() { return coordinates; }
    public void setCoordinates(CoordinatesDTO value) { this.coordinates = value; }
    public SpaceMarineDTO coordinates(CoordinatesDTO value) { this.coordinates = value; return this; }
    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String value) { this.creationDate = value; }
    public SpaceMarineDTO creationDate(String value) { this.creationDate = value; return this; }
    public Integer getHealth() { return health; }
    public void setHealth(Integer value) { this.health = value; }
    public SpaceMarineDTO health(Integer value) { this.health = value; return this; }
    public String getAchievements() { return achievements; }
    public void setAchievements(String value) { this.achievements = value; }
    public SpaceMarineDTO achievements(String value) { this.achievements = value; return this; }
    public String getWeaponType() { return weaponType; }
    public void setWeaponType(String value) { this.weaponType = value; }
    public SpaceMarineDTO weaponType(String value) { this.weaponType = value; return this; }
    public String getMeleeWeapon() { return meleeWeapon; }
    public void setMeleeWeapon(String value) { this.meleeWeapon = value; }
    public SpaceMarineDTO meleeWeapon(String value) { this.meleeWeapon = value; return this; }
    public ChapterDTO getChapter() { return chapter; }
    public void setChapter(ChapterDTO value) { this.chapter = value; }
    public SpaceMarineDTO chapter(ChapterDTO value) { this.chapter = value; return this; }
}
