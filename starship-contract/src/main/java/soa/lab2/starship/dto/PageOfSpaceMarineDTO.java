package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PageOfSpaceMarineDTO", namespace = "urn:soa:starship:v1")
public class PageOfSpaceMarineDTO {
    private java.util.List<SpaceMarineDTO> content = new java.util.ArrayList<>();
    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    public java.util.List<SpaceMarineDTO> getContent() { return content; }
    public void setContent(java.util.List<SpaceMarineDTO> value) { this.content = value; }
    public PageOfSpaceMarineDTO content(java.util.List<SpaceMarineDTO> value) { this.content = value; return this; }
    public Integer getPage() { return page; }
    public void setPage(Integer value) { this.page = value; }
    public PageOfSpaceMarineDTO page(Integer value) { this.page = value; return this; }
    public Integer getSize() { return size; }
    public void setSize(Integer value) { this.size = value; }
    public PageOfSpaceMarineDTO size(Integer value) { this.size = value; return this; }
    public Integer getTotalElements() { return totalElements; }
    public void setTotalElements(Integer value) { this.totalElements = value; }
    public PageOfSpaceMarineDTO totalElements(Integer value) { this.totalElements = value; return this; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer value) { this.totalPages = value; }
    public PageOfSpaceMarineDTO totalPages(Integer value) { this.totalPages = value; return this; }
}
