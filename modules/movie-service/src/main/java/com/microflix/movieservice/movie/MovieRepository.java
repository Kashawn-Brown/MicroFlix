package com.microflix.movieservice.movie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository         // JPA repository for Movie entities.
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    // Checks if a movie exists with the given TMDB id.
    boolean existsByTmdbId(Long tmdbId);

    // Finds a specific movie by its Tmdb id
    Optional<Movie> findByTmdbId(Long tmdbId);

    // Finds movies that have a TMDB id, but no runtime
    Page<Movie> findByRuntimeIsNullAndTmdbIdIsNotNull(Pageable pageable);

}
