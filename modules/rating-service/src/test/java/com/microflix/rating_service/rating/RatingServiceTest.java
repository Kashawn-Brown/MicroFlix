package com.microflix.rating_service.rating;

import com.microflix.rating_service.common.errors.RatingNotFoundException;
import com.microflix.rating_service.rating.dto.CreateRating;
import com.microflix.rating_service.rating.dto.RatingResponse;
import com.microflix.rating_service.rating.dto.UpdateRating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    RatingRepository ratings;          // fake repository, so we control DB behavior

    @InjectMocks
    RatingService ratingService;       // class under test

    @Test
    void createRating_whenNoExistingRating_createsNewAndReturnsResponse() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;
        double score = 8.1;

        // DTO no longer carries userId; only movieId + rate
        var request = new CreateRating(movieId, score);

        when(ratings.findByUserIdAndMovieId(userId, movieId))
                .thenReturn(Optional.empty());   // no existing rating for this pair

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // simulate JPA assigning id + timestamps
        when(ratings.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(1L);
            }
            if (r.getCreatedAt() == null) {
                r.setCreatedAt(now);
            }
            if (r.getUpdatedAt() == null) {
                r.setUpdatedAt(now);
            }
            return r;
        });

        // act - service now takes userId explicitly
        RatingResponse response = ratingService.createRating(userId, request);

        // assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(userId, response.userId());
        assertEquals(movieId, response.movieId());
        assertEquals(8.1, response.rate(), 0.0001);   // 81 -> 8.1

        // capture the entity that was saved to verify internal state
        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);
        verify(ratings).save(captor.capture());
        Rating saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals(movieId, saved.getMovieId());
        assertEquals(81, saved.getRatingTimesTen());    // 8.1 * 10 => 81
    }

    @Test
    void createRating_whenExistingRating_updatesScore() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        Rating existing = new Rating();
        existing.setId(5L);
        existing.setUserId(userId);
        existing.setMovieId(movieId);
        existing.setRatingTimesTen(70);                 // old score 7.0
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setUpdatedAt(existing.getCreatedAt());

        when(ratings.findByUserIdAndMovieId(userId, movieId))
                .thenReturn(Optional.of(existing));      // rating already exists

        when(ratings.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // DTO now only has movieId + rate
        var request = new CreateRating(movieId, 9.5);

        // act
        RatingResponse response = ratingService.createRating(userId, request);

        // assert
        assertEquals(5L, response.id());                // should reuse existing id
        assertEquals(9.5, response.rate(), 0.0001);

        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);
        verify(ratings).save(captor.capture());
        Rating saved = captor.getValue();

        assertEquals(95, saved.getRatingTimesTen());    // 9.5 * 10 => 95
    }

    @Test
    void createRating_withInvalidScore_throwsIllegalArgumentException() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        var request = new CreateRating(movieId, 0.5); // below 1.0

        // act + assert
        assertThrows(IllegalArgumentException.class,
                () -> ratingService.createRating(userId, request));
    }

    @Test
    void updateRating_whenRatingExists_updatesScore() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        Rating existing = new Rating();
        existing.setId(3L);
        existing.setUserId(userId);
        existing.setMovieId(movieId);
        existing.setRatingTimesTen(80);                 // 8.0

        when(ratings.findByUserIdAndMovieId(userId, movieId))
                .thenReturn(Optional.of(existing));

        when(ratings.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // DTO now only has movieId + new score
        var request = new UpdateRating(movieId, 9.0);

        // act
        RatingResponse response = ratingService.updateRating(userId, request);

        // assert
        assertEquals(3L, response.id());
        assertEquals(9.0, response.rate(), 0.0001);

        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);
        verify(ratings).save(captor.capture());
        Rating saved = captor.getValue();

        assertEquals(90, saved.getRatingTimesTen());    // 9.0 * 10 => 90
    }

    @Test
    void updateRating_whenRatingMissing_throwsRatingNotFoundException() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        var request = new UpdateRating(movieId, 8.0);

        when(ratings.findByUserIdAndMovieId(userId, movieId))
                .thenReturn(Optional.empty());

        // act + assert
        assertThrows(RatingNotFoundException.class,
                () -> ratingService.updateRating(userId, request));
    }

    @Test
    void getAllMovieRatings_mapsEntitiesToResponses() {
        // arrange
        Long movieId = 10L;
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Rating r1 = new Rating();
        r1.setId(1L);
        r1.setUserId(user1);
        r1.setMovieId(movieId);
        r1.setRatingTimesTen(75);
        r1.setCreatedAt(now);
        r1.setUpdatedAt(now);

        Rating r2 = new Rating();
        r2.setId(2L);
        r2.setUserId(user2);
        r2.setMovieId(movieId);
        r2.setRatingTimesTen(90);
        r2.setCreatedAt(now);
        r2.setUpdatedAt(now);

        when(ratings.findByMovieId(movieId)).thenReturn(List.of(r1, r2));

        // act
        List<RatingResponse> responses = ratingService.getAllMovieRatings(movieId);

        // assert
        assertEquals(2, responses.size());
        assertEquals(7.5, responses.get(0).rate(), 0.0001);
        assertEquals(9.0, responses.get(1).rate(), 0.0001);
    }

    @Test
    void getUserRatingForMovie_whenNotFound_throwsRatingNotFoundException() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 10L;

        when(ratings.findByUserIdAndMovieId(userId, movieId))
                .thenReturn(Optional.empty());

        // act + assert
        assertThrows(RatingNotFoundException.class,
                () -> ratingService.getUserRatingForMovie(movieId, userId));
    }

    @Test
    void getRating_whenNotFound_throwsRatingNotFoundException() {
        // arrange
        when(ratings.findById(99L)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RatingNotFoundException.class,
                () -> ratingService.getRating(99L));
    }
}
