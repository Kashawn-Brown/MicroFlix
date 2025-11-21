package com.microflix.rating_service.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndMovieId(UUID userId, Long movieId);

    List<Rating> findByUserId(UUID userId);

    List<Rating> findByMovieId(Long movieId);

    // Projection interface for summary query
    interface RatingSummaryProjection {
        Long getMovieId();
        Double getAverageTimesTen();
        Long getCount();
    }

    /**
     * Returns a single row with avg(rating_times_ten) and count for a movie, if any ratings exist.
     */
    @Query("""
           select r.movieId as movieId,
                  avg(r.ratingTimesTen) as averageTimesTen,
                  count(r.id) as count
           from Rating r
           where r.movieId = :movieId
           group by r.movieId
           """)
    Optional<RatingSummaryProjection> findSummaryByMovieId(Long movieId);

}
