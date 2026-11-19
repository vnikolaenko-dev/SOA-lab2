package soa.lab2.starship.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.dto.SpaceMarineDTO;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.model.SpaceMarine;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class CollectionClient {
    private final RestClient client;
    private final StarshipMapper mapper;

    @Autowired
    public CollectionClient(@Value("${collection.base-url}") String baseUrl, StarshipMapper mapper) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.mapper = mapper;
    }

    public CollectionClient(RestClient client, StarshipMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public SpaceMarine getMarine(long id) {
        try {
            var dto = client.get().uri("/space-marines/{id}", id).retrieve().body(SpaceMarineDTO.class);
            if (dto == null)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Сервис коллекции вернул пустой ответ");
            return mapper.toModel(dto);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Десантник не найден");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Сервис коллекции вернул ошибку");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис коллекции недоступен");
        }
    }
}
