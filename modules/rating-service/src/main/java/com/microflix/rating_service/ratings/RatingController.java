package com.microflix.rating_service.ratings;

import com.microflix.rating_service.ratings.dto.CreateRating;
import com.microflix.rating_service.ratings.dto.RatingResponse;
import com.microflix.rating_service.ratings.dto.UpdateRating;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(@RequestBody CreateRating request) {

        var response = service.createRating(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<RatingResponse> updateRating(@RequestBody UpdateRating request) {

        var response = service.updateRating(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<RatingResponse>> getAllMovieRatings(@PathVariable Long movieId) {

        var response = service.getAllMovieRatings(movieId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingResponse>> getAllUserRatings(@PathVariable UUID userId) {

        var response = service.getAllUserRatings(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/movie/{movieId}/user/{userId}")
    public ResponseEntity<RatingResponse> getUserRatingForMovie(@PathVariable Long movieId, @PathVariable UUID userId) {

        var response = service.getUserRatingForMovie(movieId, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<RatingResponse> getRating(@PathVariable Long ratingId) {

        var response = service.getRating(ratingId);

        return ResponseEntity.ok(response);
    }









}
