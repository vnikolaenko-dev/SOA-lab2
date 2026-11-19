package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StarshipDTO", namespace = "urn:soa:starship:v1")
public class StarshipDTO {
    private Long id;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public StarshipDTO id(Long value) { this.id = value; return this; }
}
