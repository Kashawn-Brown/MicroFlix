package com.microflix.rating_service.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndMovieId(UUID userId, Long movieId);

    List<Rating> findByUserId(UUID userId);

    List<Rating> findByMovieId(Long movieId);


}
