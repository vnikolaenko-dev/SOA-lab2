package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CoordinatesDTO", namespace = "urn:soa:starship:v1")
public class CoordinatesDTO {
    private Double x;
    private Integer y;
    public Double getX() { return x; }
    public void setX(Double value) { this.x = value; }
    public CoordinatesDTO x(Double value) { this.x = value; return this; }
    public Integer getY() { return y; }
    public void setY(Integer value) { this.y = value; }
    public CoordinatesDTO y(Integer value) { this.y = value; return this; }
}
