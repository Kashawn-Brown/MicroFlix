package com.microflix.rating_service.rating;

import com.microflix.rating_service.rating.dto.CreateRating;
import com.microflix.rating_service.rating.dto.RatingResponse;
import com.microflix.rating_service.rating.dto.UpdateRating;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    @Mock
    private RatingService ratingService;          // mock the service layer

    @InjectMocks
    private RatingController controller;          // controller under test

    @Test
    void createRating_returnsCreatedResponse() {
        // arrange
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", List.of("USER"));

        Long movieId = 10L;
        double score = 8.1;

        var request = new CreateRating(movieId, score);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var responseDto = new RatingResponse(
                1L, userId, movieId, score, now, now
        );

        // service should be called with userId from JWT + request DTO
        when(ratingService.createRating(userId, request)).thenReturn(responseDto);

        // act
        ResponseEntity<RatingResponse> response = controller.createRating(currentUser, request);

        // assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(score, response.getBody().rate(), 0.0001);
    }

    @Test
    void updateRating_returnsOkWithUpdatedRating() {
        // arrange
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", List.of("USER"));

        Long movieId = 10L;
        double newScore = 9.0;

        var request = new UpdateRating(movieId, newScore);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var responseDto = new RatingResponse(
                5L, userId, movieId, newScore, now, now
        );

        when(ratingService.updateRating(userId, request)).thenReturn(responseDto);

        // act
        ResponseEntity<RatingResponse> response = controller.updateRating(currentUser, request);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().id());
        assertEquals(newScore, response.getBody().rate(), 0.0001);
    }

    @Test
    void getAllMovieRatings_returnsOkWithList() {
        // arrange
        Long movieId = 10L;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        var user1 = UUID.randomUUID();
        var user2 = UUID.randomUUID();

        var r1 = new RatingResponse(1L, user1, movieId, 7.5, now, now);
        var r2 = new RatingResponse(2L, user2, movieId, 9.0, now, now);

        when(ratingService.getAllMovieRatings(movieId)).thenReturn(List.of(r1, r2));

        // act
        ResponseEntity<List<RatingResponse>> response = controller.getAllMovieRatings(movieId);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<RatingResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(7.5, body.get(0).rate(), 0.0001);
        assertEquals(9.0, body.get(1).rate(), 0.0001);
    }

    @Test
    void getAllUserRatings_returnsOkWithList() {
        // arrange
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        var r1 = new RatingResponse(1L, userId, 10L, 8.0, now, now);
        var r2 = new RatingResponse(2L, userId, 11L, 9.5, now, now);

        when(ratingService.getAllUserRatings(userId)).thenReturn(List.of(r1, r2));

        // act
        ResponseEntity<List<RatingResponse>> response = controller.getAllUserRatings(userId);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<RatingResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(8.0, body.get(0).rate(), 0.0001);
        assertEquals(9.5, body.get(1).rate(), 0.0001);
    }

    @Test
    void getUserRatingForMovie_returnsOkWithSingleRating() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        var dto = new RatingResponse(1L, userId, movieId, 8.5, now, now);

        when(ratingService.getUserRatingForMovie(movieId, userId)).thenReturn(dto);

        // act
        ResponseEntity<RatingResponse> response = controller.getUserRatingForMovie(movieId, userId);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(8.5, response.getBody().rate(), 0.0001);
    }

    @Test
    void getRating_returnsOkWithSingleRating() {
        // arrange
        Long ratingId = 99L;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        var dto = new RatingResponse(ratingId, userId, movieId, 9.0, now, now);

        when(ratingService.getRating(ratingId)).thenReturn(dto);

        // act
        ResponseEntity<RatingResponse> response = controller.getRating(ratingId);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ratingId, response.getBody().id());
        assertEquals(9.0, response.getBody().rate(), 0.0001);
    }
}
