package com.microflix.rating_service.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository         // JPA repository for Rating entities.
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndMovieId(UUID userId, Long movieId);

    List<Rating> findByUserId(UUID userId);

    List<Rating> findByMovieId(Long movieId);


    // ---------- Summary projection + custom query ----------

    /**
     * This tiny interface describes the "shape" of one summary row
     * returned by our custom query below.
     *
     * Spring Data JPA will:
     *  - run the query
     *  - take each row
     *  - create an object that implements this interface
     *  - wire each column into the matching getter
     *
     * The method names MUST match the column aliases in the query:
     *  - getMovieId()         ← "movieId" alias
     *  - getAverageTimesTen() ← "averageTimesTen" alias
     *  - getCount()           ← "count" alias
     */
    interface RatingSummaryProjection {
        Long getMovieId();
        Double getAverageTimesTen();
        Long getCount();
    }

    /**
     * Returns one "summary row" for a given movie:
     *  - average rating (still in times-ten form)
     *  - how many ratings there are
     *
     * If there are no ratings for this movie, the Optional will be empty.
     *
     * movieId → RatingSummary.getMovieId()
     * averageTimesTen → RatingSummary.getAverageTimesTen()
     * count → RatingSummary.getCount()
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
