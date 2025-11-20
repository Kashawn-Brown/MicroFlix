package com.microflix.rating_service.rating;

import com.microflix.rating_service.common.errors.RatingNotFoundException;
import com.microflix.rating_service.rating.dto.CreateRating;
import com.microflix.rating_service.rating.dto.RatingResponse;
import com.microflix.rating_service.rating.dto.UpdateRating;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public RatingResponse createRating(UUID userId, CreateRating request) {

        // 1) Check if a rating already exists for this user + movie
        var existing = ratingRepository.findByUserIdAndMovieId(
                userId,
                request.movieId()
        );

        // 2) If it exists update it; otherwise create a new one
        var rating = existing.orElseGet(Rating::new);           // Rating::new is a method reference, just a shorter way of writing: () -> new Rating()

        // compute rating out of 100 to store as an integer
        int rate  = toRatingTimesTen(request.rate());

        rating.setUserId(userId);
        rating.setMovieId(request.movieId());
        rating.setRatingTimesTen(rate);

        var newRating = ratingRepository.save(rating);

        return toResponse(newRating);
    }

    public RatingResponse updateRating(UUID userId, UpdateRating request) {
        Long movieId = request.movieId();

        var rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException("Rating cannot be found"));

        // compute rating out of 100 to store as an integer
        int rate  = toRatingTimesTen(request.rate());

        // Update rating
        rating.setRatingTimesTen(rate);

        var updatedRating = ratingRepository.save(rating);

        return toResponse(updatedRating);
    }

    public List<RatingResponse> getAllMovieRatings(Long id) {

        return ratingRepository.findByMovieId(id)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    public List<RatingResponse> getAllUserRatings(UUID userId) {

        return ratingRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    public RatingResponse getUserRatingForMovie(Long movieId, UUID userId) {
        var rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException("Rating for user " + userId + " and movie " + movieId + " was not found"));

        return toResponse(rating);
    }


    public RatingResponse getRating(Long ratingId) {
        var rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException("Rating " + ratingId + " was not found"));

        return toResponse(rating);
    }


    ///  Helper Functions


    private RatingResponse toResponse(Rating rating) {

        double rate = ratingToDouble(rating.getRatingTimesTen());

        return new RatingResponse(
                rating.getId(),
                rating.getUserId(),
                rating.getMovieId(),
                rate,
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }

    private int toRatingTimesTen(double rating) {
        // enforce 1.0–10.0 range
        if (rating < 1.0 || rating > 10.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 10.0");
        }

        // convert to integer, rounding to nearest 0.1 step
        long scaled = Math.round(rating * 10.0);
        return (int) scaled;
    }

    private double ratingToDouble(int ratingTimesTen) {
        return ratingTimesTen / 10.0;
    }



}
