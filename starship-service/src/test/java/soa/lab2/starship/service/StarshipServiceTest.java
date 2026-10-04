package soa.lab2.starship.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.client.CollectionClient;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.model.Boarding;
import soa.lab2.starship.repository.BoardingRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StarshipServiceTest {
    private final BoardingRepository repository = mock(BoardingRepository.class);
    private final StarshipMapper mapper = new StarshipMapper();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://collection/api/v1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final StarshipService service = new StarshipService(repository, new CollectionClient(builder.build(), mapper), mapper);

    @Test void loadingCallsCollectionAndPersistsBoarding() {
        server.expect(requestTo("http://collection/api/v1/space-marines/7"))
                .andRespond(withSuccess("{\"id\":7,\"name\":\"Marine\"}", MediaType.APPLICATION_JSON));
        assertEquals(7L, service.load(2, 7).id());
        var captor = ArgumentCaptor.forClass(Boarding.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getMarineId());
        assertEquals(2L, captor.getValue().getStarshipId());
        assertNull(captor.getValue().getId());
        server.verify();
    }

    @Test void missingMarineDoesNotCreateBoarding() {
        server.expect(requestTo("http://collection/api/v1/space-marines/7")).andRespond(withResourceNotFound());
        var ex = assertThrows(ResponseStatusException.class, () -> service.load(2, 7));
        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(repository);
        server.verify();
    }

    @Test void repeatedBoardingReturnsConflict() {
        server.expect(requestTo("http://collection/api/v1/space-marines/7"))
                .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));
        when(repository.existsByMarineId(7L)).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service.load(2, 7));
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
        server.verify();
    }

    @Test void unloadReturnsDeletedCount() {
        when(repository.deleteAllByStarshipId(5L)).thenReturn(3);
        assertEquals(3, service.unloadAll(5));
    }
}
