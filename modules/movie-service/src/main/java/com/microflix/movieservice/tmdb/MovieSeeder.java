package com.microflix.movieservice.tmdb;


import com.microflix.movieservice.movie.Movie;
import com.microflix.movieservice.movie.MovieRepository;
import com.microflix.movieservice.movie.MovieService;
import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.tmdb.dto.TmdbMovieListResponse;
import com.microflix.movieservice.tmdb.dto.TmdbMovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Seeds the local movie database with data fetched from TMDb.
 * Runs once on startup when movie.tmdb.seed.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "movie.tmdb.seed.enabled", havingValue = "true")  // When the app starts, Spring will only create this bean (and run it) if the property is enabled.
public class MovieSeeder implements CommandLineRunner {

    // Logging seeding progress and skips for easier debugging.
    private static final Logger log = LoggerFactory.getLogger(MovieSeeder.class);

    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;

    public MovieSeeder(MovieService movieService, MovieRepository movieRepository, TmdbClient tmdbClient) {
        this.movieService  = movieService;
        this.movieRepository  = movieRepository;
        this.tmdbClient = tmdbClient;
    }


    /**
     * Entry point for CommandLineRunner.
     * Called once after the Spring context is fully initialized.
     */
    @Override
    public void run(String... args) {

        log.info("MovieSeeder started");

        int inserted = 0;
        inserted += seedMovies(tmdbClient.fetchPopularMovies(), "Popular Movies");
        inserted += seedMovies(tmdbClient.fetchTopRatedMovies(), "Top Rated Movies");
        inserted += seedMovies(tmdbClient.fetchNowPlayingMovies(), "Now Playing Movies");
        inserted += seedMovies(tmdbClient.fetchUpcomingMovies(), "Upcoming Movies");


        log.info("Movie Seeder finished. Inserted {} total movies", inserted);

    }


    /// Helper

    /**
     * Takes the list of movies from API response and seeds into DB
     * Idempotent: skips movies that already exist (based on tmdbid)
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
                log.debug("Movie Skipped (missing tmdbId): {}", tmdbMovie.title());
                continue;
            }

            // If we already have this movie, skip (idempotent seeding)
            if (movieRepository.existsByTmdbId(tmdbId)) {
                skipped++;
                log.debug("Movie Skipped (already exists): {}", tmdbMovie.title());
                continue;
            }

            // Build full URLs from TMDb's relative paths
            String posterUrl = buildImageUrl(TMDB_POSTER_BASE_URL, tmdbMovie.poster_path());
            String backdropUrl = buildImageUrl(TMDB_BACKDROP_BASE_URL, tmdbMovie.backdrop_path());

            // Map TMDb genre IDs -> our human-readable genre names
            var genreNames = mapTmdbGenreIdsToNames(tmdbMovie.genre_ids());

            // Extract release year
            Integer releaseYear = extractYear(tmdbMovie.release_date());

            // Build the same DTO that the controller uses when creating movies manually
            var request = new CreateMovieRequest(
                    tmdbMovie.title(),
                    tmdbMovie.overview(),
                    releaseYear,
                    null,          // runtime not included in list responses; could be fetched via detail later
                    tmdbId,
                    posterUrl,
                    backdropUrl,
                    genreNames
            );

            // Delegate to MovieService so it can handle genres + MovieGenre join rows
            movieService.createMovie(request);

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



    /// Helpers


    // Base URLs for TMDb images. Will store the full URL on the Movie entity.
    private static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String TMDB_BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780";


    /**
     * Build a full image URL from a TMDb base URL and a relative path like "/abc123.jpg".
     * Returns null if the path is null/blank so we don't store junk values.
     */
    private String buildImageUrl(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return baseUrl + path;
    }


    /**
     * Convert TMDb genre IDs from a TmdbMovieResult into a list of human-readable names,
     * filtering out any IDs we don't know about.
     */
    private List<String> mapTmdbGenreIdsToNames(List<Integer> genreIds) {
        if (genreIds == null) {
            return List.of();
        }

        return genreIds.stream()
                .map(TMDB_GENRE_MAP::get)          // id -> name (may be null if unknown)
                .filter(Objects::nonNull)      // drop unknown IDs
                .distinct()                        // avoid duplicates
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }


    // Minimal TMDb genre ID -> Name map for seeding
    private static final Map<Integer, String> TMDB_GENRE_MAP = Map.ofEntries(
            Map.entry(28, "Action"),
            Map.entry(12, "Adventure"),
            Map.entry(16, "Animation"),
            Map.entry(35, "Comedy"),
            Map.entry(80, "Crime"),
            Map.entry(99, "Documentary"),
            Map.entry(18, "Drama"),
            Map.entry(10751, "Family"),
            Map.entry(14, "Fantasy"),
            Map.entry(36, "History"),
            Map.entry(27, "Horror"),
            Map.entry(10402, "Music"),
            Map.entry(9648, "Mystery"),
            Map.entry(10749, "Romance"),
            Map.entry(878, "Science Fiction"),
            Map.entry(10770, "TV Movie"),
            Map.entry(53, "Thriller"),
            Map.entry(10752, "War"),
            Map.entry(37, "Western")
    );




}
