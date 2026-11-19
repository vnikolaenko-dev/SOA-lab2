package soa.lab2.adapter;

import java.util.Arrays;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import soa.lab2.starship.dto.PageOfSpaceMarineDTO;
import soa.lab2.starship.dto.PageOfStarshipDTO;
import soa.lab2.starship.dto.SpaceMarineDTO;
import soa.lab2.starship.soap.StarshipFault;
import soa.lab2.starship.soap.StarshipSoapPort;

/** Protocol adapter only: all domain decisions belong to the SOAP service. */
@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class StarshipRestController {
    private final StarshipSoapPort soap;

    public StarshipRestController(StarshipSoapPort soap) {
        this.soap = soap;
    }

    @GetMapping("/starships")
    public PageOfStarshipDTO getStarships(@RequestParam(defaultValue = "0") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) throws StarshipFault {
        return soap.getStarships(page, size);
    }

    @GetMapping("/starship/{starshipId}/space-marines")
    public PageOfSpaceMarineDTO getCrew(@PathVariable Integer starshipId,
                                        @RequestParam(defaultValue = "0") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        HttpServletRequest request) throws StarshipFault {
        // Each repeated filter is one expression; commas inside it must survive intact.
        String[] filters = request.getParameterValues("filter");
        return soap.getStarshipSpaceMarines(starshipId, page, size,
                filters == null ? null : Arrays.asList(filters));
    }

    @PostMapping("/starship/{starshipId}/load/{spaceMarineId}")
    public SpaceMarineDTO load(@PathVariable Integer starshipId,
                                @PathVariable Integer spaceMarineId) throws StarshipFault {
        return soap.loadSpaceMarine(starshipId, spaceMarineId);
    }

    @PostMapping("/starship/{starshipId}/unload-all")
    public Integer unloadAll(@PathVariable Integer starshipId) throws StarshipFault {
        return soap.unloadAll(starshipId);
    }
}
