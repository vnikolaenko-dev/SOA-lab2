package soa.lab2.starship.controller;

import soa.lab2.starship.dto.ApiErrorDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class Errors extends ResponseEntityExceptionHandler {
    private ResponseEntity<Object> error(HttpStatusCode status, String message, String field, HttpHeaders headers) {
        return new ResponseEntity<>(new ApiErrorDTO().status(status.value()).message(message).field(field), headers, status);
    }

    // Сообщения инфраструктурных исключений заменяются русскими описаниями.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                              HttpStatusCode status, WebRequest request) {
        return error(status, description(status), null, headers);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError == null ? null : fieldError.getField();
        return error(status, field == null ? "Данные запроса не прошли проверку" :
                "Некорректное значение поля: " + field, field, headers);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Object> status(ResponseStatusException ex) {
        return error(ex.getStatusCode(), ex.getReason() == null ? description(ex.getStatusCode()) :
                ex.getReason(), null, new HttpHeaders());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> bad(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage() == null ? "Некорректные параметры запроса" :
                ex.getMessage(), null, new HttpHeaders());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> validation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Параметры запроса нарушают ограничения", null, new HttpHeaders());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> conflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "Операция нарушает ограничения целостности данных", null, new HttpHeaders());
    }

    private String description(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Некорректное тело запроса или параметры";
            case 404 -> "Запрашиваемый ресурс не найден";
            case 405 -> "HTTP-метод не поддерживается для данного адреса";
            case 406 -> "Запрошенный формат ответа не поддерживается";
            case 409 -> "Операция конфликтует с текущим состоянием данных";
            case 415 -> "Тип содержимого не поддерживается; ожидается application/json";
            case 422 -> "Данные нарушают ограничения";
            case 500 -> "Внутренняя ошибка сервера";
            case 502 -> "Связанный сервис вернул некорректный ответ";
            case 503 -> "Сервис временно недоступен";
            default -> "Не удалось обработать запрос";
        };
    }
}
