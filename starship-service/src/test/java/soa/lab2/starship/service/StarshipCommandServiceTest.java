package soa.lab2.starship.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import soa.lab2.starship.client.CollectionClient;
import soa.lab2.starship.mapper.StarshipMapper;
import soa.lab2.starship.model.Boarding;
import soa.lab2.starship.repository.BoardingRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StarshipCommandServiceTest {
    private final BoardingRepository repository = mock(BoardingRepository.class);
    private final StarshipMapper mapper = new StarshipMapper();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://collection/api/v1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final StarshipQueryService queries = new StarshipQueryService(repository, new CollectionClient(builder.build(), mapper));
    private final StarshipCommandService service = new StarshipCommandService(repository, new CollectionClient(builder.build(), mapper), mapper);

    @Test
    void loadingCallsCollectionAndPersistsBoarding() {
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

    @Test
    void missingMarineDoesNotCreateBoarding() {
        server.expect(requestTo("http://collection/api/v1/space-marines/7")).andRespond(withResourceNotFound());
        var ex = assertThrows(ResponseStatusException.class, () -> service.load(2, 7));
        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(repository);
        server.verify();
    }

    @Test
    void repeatedBoardingReturnsConflict() {
        server.expect(requestTo("http://collection/api/v1/space-marines/7"))
                .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));
        when(repository.existsByMarineId(7L)).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service.load(2, 7));
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
        server.verify();
    }

    @Test
    void unloadReturnsDeletedCount() {
        when(repository.deleteAllByStarshipId(5L)).thenReturn(3);
        assertEquals(3, service.unloadAll(5));
    }

    @Test
    void crewFiltersBeforePaginating() {
        when(repository.findAllByStarshipIdOrderByIdAsc(2L)).thenReturn(List.of(
                Boarding.builder().withMarineId(7L).withStarshipId(2L).build(),
                Boarding.builder().withMarineId(8L).withStarshipId(2L).build(),
                Boarding.builder().withMarineId(9L).withStarshipId(2L).build()));
        server.expect(requestTo("http://collection/api/v1/space-marines/7"))
                .andRespond(withSuccess("{\"id\":7,\"health\":50,\"weaponType\":\"COMBI_FLAMER\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://collection/api/v1/space-marines/8"))
                .andRespond(withSuccess("{\"id\":8,\"health\":90,\"weaponType\":\"GRENADE_LAUNCHER\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://collection/api/v1/space-marines/9"))
                .andRespond(withSuccess("{\"id\":9,\"health\":100,\"weaponType\":\"COMBI_FLAMER\"}", MediaType.APPLICATION_JSON));

        var result = queries.listCrew(2, 0, 1, List.of("health,gt,60", "weaponType,eq,COMBI_FLAMER"));
        assertEquals(1, result.getTotalElements());
        assertEquals(9L, result.getContent().getFirst().id());
        assertEquals(1, result.getTotalPages());
        server.verify();
    }

    @Test
    void invalidCrewFilterFailsBeforeRemoteCalls() {
        assertThrows(IllegalArgumentException.class, () -> queries.listCrew(2, 0, 10, List.of("health,gt,invalid")));
        verifyNoInteractions(repository);
    }

    @Test
    void listsStarshipsFromRepository() {
        var requestedPage = PageRequest.of(1, 2);
        when(repository.findStarshipIds(requestedPage))
                .thenReturn(new PageImpl<>(List.of(3L), requestedPage, 3));
        var result = queries.listStarships(1, 2);
        assertEquals(List.of(3L), result.getContent());
        assertEquals(3, result.getTotalElements());
        assertThrows(IllegalArgumentException.class, () -> queries.listStarships(0, 101));
    }
}
