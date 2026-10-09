package soa.lab2.starship.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import soa.lab2.starship.controller.generated.StarshipApiDelegate;
import soa.lab2.starship.dto.PageOfSpaceMarineDTO;
import soa.lab2.starship.dto.PageOfStarshipDTO;
import soa.lab2.starship.dto.SpaceMarineDTO;
import soa.lab2.starship.dto.StarshipDTO;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.service.StarshipService;

import java.util.Arrays;
import java.util.List;

@Component
public class StarshipControllerDelegate implements StarshipApiDelegate {
    private final StarshipService service;
    private final StarshipMapper mapper;
    private final HttpServletRequest request;

    public StarshipControllerDelegate(StarshipService service, StarshipMapper mapper, HttpServletRequest request) {
        this.service = service;
        this.mapper = mapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> loadSpaceMarine(Integer starshipId, Integer spaceMarineId) {
        return ResponseEntity.ok(mapper.toDto(service.load(starshipId, spaceMarineId)));
    }

    @Override
    public ResponseEntity<Integer> unloadAll(Integer starshipId) {
        return ResponseEntity.ok(service.unloadAll(starshipId));
    }

    @Override
    public ResponseEntity<PageOfSpaceMarineDTO> getStarshipSpaceMarines(Integer starshipId, Integer page,
                                                                         Integer size, List<String> filter) {
        String[] rawFilter = request.getParameterValues("filter");
        var result = service.listCrew(starshipId, page, size,
                rawFilter == null ? null : Arrays.asList(rawFilter));
        return ResponseEntity.ok(new PageOfSpaceMarineDTO()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }

    @Override
    public ResponseEntity<PageOfStarshipDTO> getStarships(Integer page, Integer size) {
        var result = service.listStarships(page, size);
        return ResponseEntity.ok(new PageOfStarshipDTO()
                .content(result.getContent().stream().map(id -> new StarshipDTO().id(id)).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }
}
