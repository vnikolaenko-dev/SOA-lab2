package soa.lab2.collection.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import soa.lab2.collection.controller.generated.SpaceMarinesCollectionApiDelegate;
import soa.lab2.collection.dto.*;
import soa.lab2.collection.mapper.SpaceMarineMapper;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.service.CollectionService;

@Component
public class CollectionApiDelegate implements SpaceMarinesCollectionApiDelegate {
    private final CollectionService service;
    private final SpaceMarineMapper mapper;
    private final HttpServletRequest request;

    public CollectionApiDelegate(CollectionService service, SpaceMarineMapper mapper, HttpServletRequest request) {
        this.service = service;
        this.mapper = mapper;
        this.request = request;
    }

    @Override public ResponseEntity<SpaceMarineDTO> addSpaceMarine(SpaceMarineInputDTO input) {
        SpaceMarineDTO dto = mapper.toDto(service.create(mapper.toModel(input)));
        return ResponseEntity.created(URI.create("/api/v1/space-marines/" + dto.getId())).body(dto);
    }
    @Override public ResponseEntity<SpaceMarineDTO> getSpaceMarineById(Integer id) {
        return ResponseEntity.ok(mapper.toDto(service.get(id)));
    }
    @Override public ResponseEntity<SpaceMarineDTO> updateSpaceMarine(Integer id, SpaceMarineInputDTO input) {
        return ResponseEntity.ok(mapper.toDto(service.update(id, mapper.toModel(input))));
    }
    @Override public ResponseEntity<Void> deleteSpaceMarine(Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @Override public ResponseEntity<Integer> deleteByHealth(Integer health) {
        return ResponseEntity.ok(service.deleteByHealth(health));
    }
    @Override public ResponseEntity<Void> deleteOneByWeapon(WeaponDTO weaponType) {
        service.deleteOneByWeapon(Weapon.valueOf(weaponType.getValue()));
        return ResponseEntity.noContent().build();
    }
    @Override public ResponseEntity<SpaceMarineDTO> getMaxName() {
        return ResponseEntity.ok(mapper.toDto(service.getMaxName()));
    }
    @Override public ResponseEntity<PageOfSpaceMarineDTO> getSpaceMarines(Integer page, Integer size,
                                                                     List<String> sort, List<String> filter) {
        // Конвертер List в Spring разделяет значения по запятым; form/explode требует исходные значения параметров.
        String[] rawSort = request.getParameterValues("sort");
        String[] rawFilter = request.getParameterValues("filter");
        var result = service.list(page, size, rawSort == null ? null : Arrays.asList(rawSort),
                rawFilter == null ? null : Arrays.asList(rawFilter));
        return ResponseEntity.ok(new PageOfSpaceMarineDTO()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }
}
