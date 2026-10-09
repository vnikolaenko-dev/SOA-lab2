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
import soa.lab2.starship.service.StarshipCommandService;
import soa.lab2.starship.service.StarshipQueryService;

import java.util.Arrays;
import java.util.List;

@Component
public class StarshipControllerDelegate implements StarshipApiDelegate {
    private final StarshipCommandService commands;
    private final StarshipQueryService queries;
    private final StarshipMapper mapper;
    private final HttpServletRequest request;

    public StarshipControllerDelegate(StarshipCommandService commands, StarshipQueryService queries, StarshipMapper mapper, HttpServletRequest request) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> loadSpaceMarine(Integer starshipId, Integer spaceMarineId) {
        return ResponseEntity.ok(mapper.toDto(commands.load(starshipId, spaceMarineId)));
    }

    @Override
    public ResponseEntity<Integer> unloadAll(Integer starshipId) {
        return ResponseEntity.ok(commands.unloadAll(starshipId));
    }

    @Override
    public ResponseEntity<PageOfSpaceMarineDTO> getStarshipSpaceMarines(Integer starshipId, Integer page,
                                                                         Integer size, List<String> filter) {
        String[] rawFilter = request.getParameterValues("filter");
        var result = queries.listCrew(starshipId, page, size,
                rawFilter == null ? null : Arrays.asList(rawFilter));
        return ResponseEntity.ok(new PageOfSpaceMarineDTO()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }

    @Override
    public ResponseEntity<PageOfStarshipDTO> getStarships(Integer page, Integer size) {
        var result = queries.listStarships(page, size);
        return ResponseEntity.ok(new PageOfStarshipDTO()
                .content(result.getContent().stream().map(id -> new StarshipDTO().id(id)).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }
}
