package com.microflix.rating_service.engagement;

import com.microflix.rating_service.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller tests focus on:
 *  - HTTP status codes
 *  - Wiring from controller -> service
 *  - Response mapping for read endpoints
 */
@ExtendWith(MockitoExtension.class)
class EngagementControllerTest {

    @Mock
    private EngagementService engagementService;

    @InjectMocks
    private EngagementController controller;

    @Test
    void addToWatchlist_returnsNoContentAndCallsService() {
        // arrange
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", List.of("USER"));
        Long movieId = 42L;

        // act
        ResponseEntity<Void> response = controller.addToWatchlist(currentUser, movieId);

        // assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        // Service should be called with the userId from CurrentUser and movieId
        verify(engagementService).addToWatchlist(userId, movieId);
    }

    @Test
    void removeFromWatchlist_returnsNoContentAndCallsService() {
        // arrange
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", List.of("USER"));
        Long movieId = 42L;

        // act
        ResponseEntity<Void> response = controller.removeFromWatchlist(currentUser, movieId);

        // assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(engagementService).removeFromWatchlist(userId, movieId);
    }

    @Test
    void getWatchlist_returnsOkWithBody() {
        // arrange
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", List.of("USER"));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var item1 = new EngagementItemResponse(userId, 10L, EngagementType.WATCHLIST, now.minusDays(1));
        var item2 = new EngagementItemResponse(userId, 20L, EngagementType.WATCHLIST, now);

        when(engagementService.getWatchlist(userId)).thenReturn(List.of(item1, item2));

        // act
        ResponseEntity<List<EngagementItemResponse>> response = controller.getWatchlist(currentUser);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<EngagementItemResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(10L, body.get(0).movieId());
        assertEquals(20L, body.get(1).movieId());
    }
}
