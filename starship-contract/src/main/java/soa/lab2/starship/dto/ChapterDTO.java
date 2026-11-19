package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChapterDTO", namespace = "urn:soa:starship:v1")
public class ChapterDTO {
    private String name;
    private String parentLegion;
    private String world;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public ChapterDTO name(String value) { this.name = value; return this; }
    public String getParentLegion() { return parentLegion; }
    public void setParentLegion(String value) { this.parentLegion = value; }
    public ChapterDTO parentLegion(String value) { this.parentLegion = value; return this; }
    public String getWorld() { return world; }
    public void setWorld(String value) { this.world = value; }
    public ChapterDTO world(String value) { this.world = value; return this; }
}
