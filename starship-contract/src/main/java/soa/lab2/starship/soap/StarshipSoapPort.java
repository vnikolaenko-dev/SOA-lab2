package soa.lab2.starship.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import soa.lab2.starship.dto.*;
import java.util.List;

@WebService(name = "StarshipSoapPort", targetNamespace = "urn:soa:starship:v1")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface StarshipSoapPort {
    @WebMethod
    SpaceMarineDTO loadSpaceMarine(@WebParam(name = "starshipId") Integer starshipId,
                                  @WebParam(name = "spaceMarineId") Integer spaceMarineId) throws StarshipFault;
    @WebMethod
    Integer unloadAll(@WebParam(name = "starshipId") Integer starshipId) throws StarshipFault;
    @WebMethod
    PageOfSpaceMarineDTO getStarshipSpaceMarines(@WebParam(name = "starshipId") Integer starshipId,
            @WebParam(name = "page") Integer page, @WebParam(name = "size") Integer size,
            @WebParam(name = "filter") List<String> filter) throws StarshipFault;
    @WebMethod
    PageOfStarshipDTO getStarships(@WebParam(name = "page") Integer page,
                                 @WebParam(name = "size") Integer size) throws StarshipFault;
}
