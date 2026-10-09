package soa.lab2.collection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import soa.lab2.collection.dto.SpaceMarinePatchInputDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

@ControllerAdvice
public class PatchBodyFields extends RequestBodyAdviceAdapter {
    public static final String ATTRIBUTE = PatchBodyFields.class.getName() + ".fields";
    private final ObjectMapper objectMapper;

    public PatchBodyFields(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return parameter.getParameterType() == SpaceMarinePatchInputDTO.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] body = message.getBody().readAllBytes();
        Set<String> fields = new HashSet<>();
        var json = objectMapper.readTree(body);
        if (json != null && json.isObject()) json.fieldNames().forEachRemaining(fields::add);
        RequestContextHolder.currentRequestAttributes().setAttribute(ATTRIBUTE, fields, RequestAttributes.SCOPE_REQUEST);
        return new HttpInputMessage() {
            @Override
            public java.io.InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return message.getHeaders();
            }
        };
    }
}
