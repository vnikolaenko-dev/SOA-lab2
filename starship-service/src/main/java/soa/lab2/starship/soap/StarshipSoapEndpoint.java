package soa.lab2.starship.soap;

import jakarta.jws.WebService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.dto.*;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.service.StarshipCommandService;
import soa.lab2.starship.service.StarshipQueryService;
import java.util.List;

@Component
@WebService(endpointInterface = "soa.lab2.starship.soap.StarshipSoapPort",
        serviceName = "StarshipService", portName = "StarshipPort", targetNamespace = "urn:soa:starship:v1")
public class StarshipSoapEndpoint implements StarshipSoapPort {
    private final StarshipCommandService commands;
    private final StarshipQueryService queries;
    private final StarshipMapper mapper;

    public StarshipSoapEndpoint(StarshipCommandService commands, StarshipQueryService queries, StarshipMapper mapper) {
        this.commands = commands; this.queries = queries; this.mapper = mapper;
    }

    @Override
    public SpaceMarineDTO loadSpaceMarine(Integer starshipId, Integer spaceMarineId) throws StarshipFault {
        positive(starshipId, "starshipId"); positive(spaceMarineId, "spaceMarineId");
        return invoke(() -> mapper.toDto(commands.load(starshipId, spaceMarineId)));
    }

    @Override
    public Integer unloadAll(Integer starshipId) throws StarshipFault {
        positive(starshipId, "starshipId");
        return invoke(() -> commands.unloadAll(starshipId));
    }

    @Override
    public PageOfSpaceMarineDTO getStarshipSpaceMarines(Integer starshipId, Integer page, Integer size,
                                                       List<String> filter) throws StarshipFault {
        positive(starshipId, "starshipId");
        return invoke(() -> {
            var result = queries.listCrew(starshipId, page, size, filter);
            return new PageOfSpaceMarineDTO().content(result.getContent().stream().map(mapper::toDto).toList())
                    .page(result.getNumber()).size(result.getSize()).totalElements(Math.toIntExact(result.getTotalElements()))
                    .totalPages(result.getTotalPages());
        });
    }

    @Override
    public PageOfStarshipDTO getStarships(Integer page, Integer size) throws StarshipFault {
        return invoke(() -> {
            var result = queries.listStarships(page, size);
            return new PageOfStarshipDTO().content(result.getContent().stream().map(id -> new StarshipDTO().id(id)).toList())
                    .page(result.getNumber()).size(result.getSize()).totalElements(Math.toIntExact(result.getTotalElements()))
                    .totalPages(result.getTotalPages());
        });
    }

    private void positive(Integer value, String field) throws StarshipFault {
        if (value == null || value < 1)
            throw new StarshipFault("Identifier must be positive", new FaultDetail(400, "Identifier must be positive", field));
    }

    private <T> T invoke(java.util.function.Supplier<T> action) throws StarshipFault {
        try {
            return action.get();
        } catch (ResponseStatusException ex) {
            String message = ex.getReason() == null ? "Service error" : ex.getReason();
            throw new StarshipFault(message, new FaultDetail(ex.getStatusCode().value(), message, null), ex);
        } catch (IllegalArgumentException ex) {
            throw new StarshipFault(ex.getMessage(), new FaultDetail(400, ex.getMessage(), null), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new StarshipFault("Data integrity conflict", new FaultDetail(409, "Data integrity conflict", null), ex);
        } catch (RuntimeException ex) {
            throw new StarshipFault("Internal service error", new FaultDetail(500, "Internal service error", null), ex);
        }
    }
}
