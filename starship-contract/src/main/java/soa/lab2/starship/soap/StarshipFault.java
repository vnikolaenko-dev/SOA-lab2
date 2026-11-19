package soa.lab2.starship.soap;

import jakarta.xml.ws.WebFault;

@WebFault(name = "StarshipFault", targetNamespace = "urn:soa:starship:v1")
public class StarshipFault extends Exception {
    private final FaultDetail faultInfo;

    public StarshipFault(String message, FaultDetail faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public StarshipFault(String message, FaultDetail faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public FaultDetail getFaultInfo() { return faultInfo; }
}
