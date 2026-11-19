package soa.lab2.starship.dto;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PageOfStarshipDTO", namespace = "urn:soa:starship:v1")
public class PageOfStarshipDTO {
    private java.util.List<StarshipDTO> content = new java.util.ArrayList<>();
    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    public java.util.List<StarshipDTO> getContent() { return content; }
    public void setContent(java.util.List<StarshipDTO> value) { this.content = value; }
    public PageOfStarshipDTO content(java.util.List<StarshipDTO> value) { this.content = value; return this; }
    public Integer getPage() { return page; }
    public void setPage(Integer value) { this.page = value; }
    public PageOfStarshipDTO page(Integer value) { this.page = value; return this; }
    public Integer getSize() { return size; }
    public void setSize(Integer value) { this.size = value; }
    public PageOfStarshipDTO size(Integer value) { this.size = value; return this; }
    public Integer getTotalElements() { return totalElements; }
    public void setTotalElements(Integer value) { this.totalElements = value; }
    public PageOfStarshipDTO totalElements(Integer value) { this.totalElements = value; return this; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer value) { this.totalPages = value; }
    public PageOfStarshipDTO totalPages(Integer value) { this.totalPages = value; return this; }
}
