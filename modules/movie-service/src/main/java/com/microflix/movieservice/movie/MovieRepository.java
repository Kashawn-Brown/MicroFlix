package com.microflix.movieservice.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository         // JPA repository for Movie entities.
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Checks if a movie exists with the given TMDB id.
    boolean existsByTmdbId(Long tmdbId);

}
