package soa.lab2.collection.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import soa.lab2.collection.controller.generated.SpaceMarinesCollectionApiDelegate;
import soa.lab2.collection.dto.PageOfSpaceMarineDTO;
import soa.lab2.collection.dto.SpaceMarineDTO;
import soa.lab2.collection.dto.SpaceMarineInputDTO;
import soa.lab2.collection.dto.SpaceMarinePatchInputDTO;
import soa.lab2.collection.dto.WeaponDTO;
import soa.lab2.collection.mapper.SpaceMarineMapper;
import soa.lab2.collection.model.Weapon;
import soa.lab2.collection.service.CollectionCommandService;
import soa.lab2.collection.service.CollectionQueryService;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class CollectionApiDelegate implements SpaceMarinesCollectionApiDelegate {
    private final CollectionCommandService commands;
    private final CollectionQueryService queries;
    private final SpaceMarineMapper mapper;
    private final HttpServletRequest request;

    public CollectionApiDelegate(CollectionCommandService commands, CollectionQueryService queries, SpaceMarineMapper mapper, HttpServletRequest request) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> addSpaceMarine(SpaceMarineInputDTO input) {
        SpaceMarineDTO dto = mapper.toDto(commands.create(mapper.toModel(input)));
        return ResponseEntity.created(URI.create("/api/v1/space-marines/" + dto.getId())).body(dto);
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> getSpaceMarineById(Integer id) {
        return ResponseEntity.ok(mapper.toDto(queries.get(id)));
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> updateSpaceMarine(Integer id, SpaceMarineInputDTO input) {
        return ResponseEntity.ok(mapper.toDto(commands.update(id, mapper.toModel(input))));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<SpaceMarineDTO> patchSpaceMarine(Integer id, SpaceMarinePatchInputDTO input) {
        Object fields = RequestContextHolder.currentRequestAttributes()
                .getAttribute(PatchBodyFields.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        Set<String> providedFields = fields instanceof Set<?> ? (Set<String>) fields : Set.of();
        return ResponseEntity.ok(mapper.toDto(commands.patch(id, mapper.toPatch(input, providedFields))));
    }

    @Override
    public ResponseEntity<Void> deleteSpaceMarine(Integer id) {
        commands.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Integer> deleteByHealth(Integer health) {
        return ResponseEntity.ok(commands.deleteByHealth(health));
    }

    @Override
    public ResponseEntity<Void> deleteOneByWeapon(WeaponDTO weaponType) {
        commands.deleteOneByWeapon(Weapon.valueOf(weaponType.getValue()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SpaceMarineDTO> getMaxName() {
        return ResponseEntity.ok(mapper.toDto(queries.getMaxName()));
    }

    @Override
    public ResponseEntity<PageOfSpaceMarineDTO> getSpaceMarines(Integer page, Integer size,
                                                                List<String> sort, List<String> filter) {
        // Конвертер List в Spring разделяет значения по запятым; form/explode требует исходные значения параметров.
        String[] rawSort = request.getParameterValues("sort");
        String[] rawFilter = request.getParameterValues("filter");
        var result = queries.list(page, size, rawSort == null ? null : Arrays.asList(rawSort),
                rawFilter == null ? null : Arrays.asList(rawFilter));
        return ResponseEntity.ok(new PageOfSpaceMarineDTO()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(Math.toIntExact(result.getTotalElements())).totalPages(result.getTotalPages()));
    }
}
