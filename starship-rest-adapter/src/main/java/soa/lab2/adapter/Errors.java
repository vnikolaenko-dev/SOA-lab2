package soa.lab2.adapter;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import soa.lab2.starship.soap.StarshipFault;

@RestControllerAdvice
public class Errors extends ResponseEntityExceptionHandler {
    public record ApiError(Integer status, String message, String field) {}

    @ExceptionHandler(StarshipFault.class)
    ResponseEntity<Object> soapFault(StarshipFault fault) {
        var detail = fault.getFaultInfo();
        return ResponseEntity.status(detail.getStatus())
                .body(new ApiError(detail.getStatus(), detail.getMessage(), detail.getField()));
    }

    @ExceptionHandler(SOAPFaultException.class)
    ResponseEntity<Object> unexpectedFault(SOAPFaultException fault) {
        return ResponseEntity.status(502)
                .body(new ApiError(502, "SOAP-сервис вернул некорректный ответ", null));
    }

    @ExceptionHandler(WebServiceException.class)
    ResponseEntity<Object> transportFailure(WebServiceException failure) {
        return ResponseEntity.status(503)
                .body(new ApiError(503, "SOAP-сервис временно недоступен", null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "Некорректное тело запроса или параметры";
            case 404 -> "Запрашиваемый ресурс не найден";
            case 405 -> "HTTP-метод не поддерживается для данного адреса";
            case 406 -> "Запрошенный формат ответа не поддерживается";
            case 415 -> "Тип содержимого не поддерживается; ожидается application/json";
            default -> "Не удалось обработать запрос";
        };
        return new ResponseEntity<>(new ApiError(status.value(), message, null), headers, status);
    }
}
