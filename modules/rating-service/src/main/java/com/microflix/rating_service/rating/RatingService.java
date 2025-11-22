package com.microflix.rating_service.rating;

import com.microflix.rating_service.common.errors.RatingNotFoundException;
import com.microflix.rating_service.rating.dto.CreateRating;
import com.microflix.rating_service.rating.dto.MovieRatingSummaryResponse;
import com.microflix.rating_service.rating.dto.RatingResponse;
import com.microflix.rating_service.rating.dto.UpdateRating;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RatingService {        // Encapsulates business logic for rating operations.

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }


    /**
     * Creates a new rating or updates an existing one for the user + movie
     */
    public RatingResponse createRating(UUID userId, CreateRating request) {

        // Check if a rating already exists for this user + movie
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

    /**
     * Updates an existing rating for the given user and movie.
     */
    public RatingResponse updateRating(UUID userId, UpdateRating request) {
        Long movieId = request.movieId();

        var rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException("Rating cannot be found"));

        int rate  = toRatingTimesTen(request.rate());

        rating.setRatingTimesTen(rate);

        var updatedRating = ratingRepository.save(rating);

        return toResponse(updatedRating);
    }

    /**
     * Returns all ratings for a movie.
     */
    public List<RatingResponse> getAllMovieRatings(Long id) {

        return ratingRepository.findByMovieId(id)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all ratings created by a user.
     */
    public List<RatingResponse> getAllUserRatings(UUID userId) {

        return ratingRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a user's rating for a specific movie.
     */
    public RatingResponse getUserRatingForMovie(Long movieId, UUID userId) {
        var rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException("Rating for user " + userId + " and movie " + movieId + " was not found"));

        return toResponse(rating);
    }

    /**
     * Returns a rating by its ID.
     */
    public RatingResponse getRating(Long ratingId) {
        var rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException("Rating " + ratingId + " was not found"));

        return toResponse(rating);
    }


    /**
     * Returns summary stats for all ratings on a given movie.
     * If there are no ratings, returns count=0 and average=null.
     */
    public MovieRatingSummaryResponse getMovieRatingSummary(Long movieId) {
        var summaryOpt = ratingRepository.findSummaryByMovieId(movieId);

        if (summaryOpt.isPresent()) {
            var summary = summaryOpt.get();

            double average = summary.getAverageTimesTen() / 10.0;

            return new MovieRatingSummaryResponse(
                    summary.getMovieId(),
                    average,
                    summary.getCount()
            );
        }
        else {
            // No ratings found for this movie
            return new MovieRatingSummaryResponse(
                    movieId,
                    null,
                    0L
            );
        }
    }

    public void deleteRating(UUID userId, Long movieId) {

        var rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException("Rating for user " + userId + " and movie " + movieId + " was not found"));

        ratingRepository.delete(rating);

    }


    ///  Helper Functions


    /**
     * Maps Rating entity to response DTO.
     */
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
