package soa.lab2.starship.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import soa.lab2.starship.controller.generated.StarshipApiDelegate;
import soa.lab2.starship.dto.SpaceMarineDTO;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.service.StarshipService;

@Component
public class StarshipControllerDelegate implements StarshipApiDelegate {
    private final StarshipService service;
    private final StarshipMapper mapper;

    public StarshipControllerDelegate(StarshipService service, StarshipMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> loadSpaceMarine(Integer starshipId, Integer spaceMarineId) {
        return ResponseEntity.ok(mapper.toDto(service.load(starshipId, spaceMarineId)));
    }

    @Override
    public ResponseEntity<Integer> unloadAll(Integer starshipId) {
        return ResponseEntity.ok(service.unloadAll(starshipId));
    }
}
