package com.microflix.movieservice.tmdb;


import com.microflix.movieservice.movie.Movie;
import com.microflix.movieservice.movie.MovieRepository;
import com.microflix.movieservice.tmdb.dto.TmdbMovieListResponse;
import com.microflix.movieservice.tmdb.dto.TmdbMovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Seeds the local movie database with data fetched from TMDb.
 * Runs once on startup when movie.tmdb.seed.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "movie.tmdb.seed.enabled", havingValue = "true")
public class MovieSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MovieSeeder.class);

    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;

    public MovieSeeder(MovieRepository movieRepository, TmdbClient tmdbClient) {
        this.movieRepository = movieRepository;
        this.tmdbClient = tmdbClient;
    }


    @Override
    public void run(String... args) {

        log.info("MovieSeeder started");

        int inserted = seedMovies(tmdbClient.fetchPopularMovies(), "Popular Movies");
        inserted += seedMovies(tmdbClient.fetchTopRatedMovies(), "Top Rated Movies");
        inserted += seedMovies(tmdbClient.fetchNowPlayingMovies(), "Now Playing Movies");
        inserted += seedMovies(tmdbClient.fetchUpcomingMovies(), "Upcoming Movies");


        log.info("Movie Seeder finished. Inserted {} total movies", inserted);

    }


    /// Helper

    /**
     * Takes the list of movies of API response and seeds into db
     */
    private int seedMovies(TmdbMovieListResponse movieList, String listName) {
        if (movieList == null || movieList.results() == null || movieList.results().isEmpty()) {
            log.warn("No results returned from TMDb {} endpoint - skipping seeding", listName);
            return 0;
        }

        int inserted = 0;
        int skipped = 0;

        for (TmdbMovieResult tmdbMovie : movieList.results()) {
            Long tmdbId = tmdbMovie.id();

            // If tmdbId is missing, we can't dedupe properly, so we skip it
            if (tmdbId == null) {
                skipped++;
                log.debug("Movie Skipped: {}", tmdbMovie.title());
                continue;
            }

            // If we already have this movie, skip (idempotent seeding)
            if (movieRepository.existsByTmdbId(tmdbId)) {
                skipped++;
                log.debug("Movie Skipped: {}", tmdbMovie.title());
                continue;
            }

            Movie movie = new Movie();
            movie.setTitle(tmdbMovie.title());
            movie.setOverview(tmdbMovie.overview());
            movie.setReleaseYear(extractYear(tmdbMovie.release_date()));
            movie.setRuntime(null);         // runtime is not included in the movie list response
            movie.setTmdbId(tmdbId);

            // createdAt/updatedAt will be set by @PrePersist, but setting them here is also safe
            movie.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            movie.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            movieRepository.save(movie);
            inserted++;
        }

        log.info("Finished inserting {}. Inserted {} movies, skipped {}", listName, inserted, skipped);

        return inserted;
    }

    /**
     * Extracts the year (e.g. 2010) from a TMDb release_date string like "2010-07-15".
     */
    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            log.warn("Could not parse release year from release_date='{}'", releaseDate);
            return null;
        }
    }

}
