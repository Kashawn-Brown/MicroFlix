package com.microflix.rating_service.rating;

import com.microflix.rating_service.rating.dto.CreateRating;
import com.microflix.rating_service.rating.dto.MovieRatingSummaryResponse;
import com.microflix.rating_service.rating.dto.RatingResponse;
import com.microflix.rating_service.rating.dto.UpdateRating;
import com.microflix.rating_service.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {         // Handles HTTP requests related to rating operations.

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    /**
     * Creates or updates a rating for the current user.
     */
    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal CurrentUser user,  // @AuthenticationPrincipal -> says take the current Authentication, then give me its .getPrincipal() cast to this type (CurrentUser)
            @RequestBody CreateRating request
    ) {

        var response = service.createRating(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing rating for the current user.
     */
    @PatchMapping
    public ResponseEntity<RatingResponse> updateRating(
            @AuthenticationPrincipal CurrentUser user,
            @RequestBody UpdateRating request
    ) {

        var response = service.updateRating(user.id(), request);

        return ResponseEntity.ok(response);
    }

//    /**
//     * Full update to an existing rating for the current user.
//     */
//    @PutMapping
//    public ResponseEntity<RatingResponse> fullUpdateRating(
//            @AuthenticationPrincipal CurrentUser user,
//            @RequestBody FullUpdateRating request
//    ) {
//
//        var response = service.fullUpdateRating(user.id(), request);
//
//        return ResponseEntity.ok(response);
//    }

    /**
     * Returns all ratings for a given movie.
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<RatingResponse>> getAllMovieRatings(@PathVariable Long movieId) {

        var response = service.getAllMovieRatings(movieId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns all a users ratings they made. (uses Token)
     */
    @GetMapping("/user")
    public ResponseEntity<List<RatingResponse>> getUsersRatings(@AuthenticationPrincipal CurrentUser user) {

        var response = service.getAllUserRatings(user.id());

        return ResponseEntity.ok(response);
    }

    /**
     * Returns all ratings created by a given user. (Uses user id in URL)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingResponse>> getAllUserRatings(@PathVariable UUID userId) {

        var response = service.getAllUserRatings(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns a specific user's rating for a given movie.
     */
    @GetMapping("/movie/{movieId}/user/{userId}")
    public ResponseEntity<RatingResponse> getUserRatingForMovie(
            @PathVariable Long movieId,
            @PathVariable UUID userId
    ) {

        var response = service.getUserRatingForMovie(movieId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns a rating by its ID.
     */
    @GetMapping("/{ratingId}")
    public ResponseEntity<RatingResponse> getRating(@PathVariable Long ratingId) {

        var response = service.getRating(ratingId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the average rating and count for a given movie.
     * Public: no authentication required.
     */
    @GetMapping("/movie/{movieId}/summary")
    public ResponseEntity<MovieRatingSummaryResponse> getMovieRatingSummary(@PathVariable Long movieId) {

        var response = service.getMovieRatingSummary(movieId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> deleteRating(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable Long movieId
    ) {

        service.deleteRating(user.id(), movieId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }









}
