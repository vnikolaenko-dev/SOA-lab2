package soa.lab2.starship.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaultDetail", namespace = "urn:soa:starship:v1")
public class FaultDetail {
    private Integer status;
    private String message;
    private String field;
    public FaultDetail() {}
    public FaultDetail(Integer status, String message, String field) {
        this.status = status; this.message = message; this.field = field;
    }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
}
