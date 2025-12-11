package com.microflix.tmdb_ingestion_service.tmdb;

import com.microflix.tmdb_ingestion_service.movie.dto.CreateMovieRequest;
import com.microflix.tmdb_ingestion_service.movie.MovieServiceClient;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieListResponse;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CommandLineRunner that runs once when the app starts.
 *
 * Main entrypoint for the ingestion job.
 * When the app starts, Spring will call run() once, then the app exits.
 *
 *  - Fetches lists of movies from TMDb
 *      - Skips missing/duplicate tmdbIds
 *      - Sends a CreateMovieRequest to movie-service
 *  - Stops once we've inserted the requested number of movies (target count).
 */
@Component
public class TmdbIngestionJob implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TmdbIngestionJob.class);

    private final TmdbClient tmdbClient;
    private final MovieServiceClient movieServiceClient;

    public TmdbIngestionJob(TmdbClient tmdbClient, MovieServiceClient movieServiceClient) {
        this.tmdbClient = tmdbClient;
        this.movieServiceClient = movieServiceClient;
    }

    @Value("${ingestion.default-count:50}")
    private int defaultTargetCount;

    // Base URLs for TMDb images: store the full URL on the Movie entity.
    private static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String TMDB_BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780";


    /**
     * Entry Point
     */
    @Override
    public void run(String... args) {
        int targetCount = resolveTargetCount(args);

        log.info("Starting TMDb ingestion job with targetCount={}", targetCount);


        int insertedTotal = 0;
        int page = 1;

        // Keep going until we hit the target or decide to stop via break conditions.
        while(!reachedTarget(targetCount, insertedTotal)) {

            int added = 0;
            int insertedThisPage = 0;

            // --- Popular ---
            added += seedMovies(tmdbClient.fetchPopularMovies(page), "Popular Movies");
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Top Rated ---
            added += seedMovies(tmdbClient.fetchTopRatedMovies(page), "Top Rated Movies");
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Now Playing ---
            added += seedMovies(tmdbClient.fetchNowPlayingMovies(page), "Now Playing Movies");
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Upcoming ---
            added += seedMovies(tmdbClient.fetchUpcomingMovies(page), "Upcoming Movies");
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Discover (no early break needed here) ---
            added += seedMovies(tmdbClient.discoverPopularMovies(page), "Discover Movies");
            insertedThisPage += added;
            insertedTotal += added;


            // If nothing new was inserted from any endpoint for this page, there is no point continuing to the next page
            if (insertedThisPage == 0 && insertedTotal > 0) {
                log.info("No new movies inserted on page {} across any endpoint. Stopping.", page);
                break;
            }
            // Safety cap: avoid crawling too many TMDb pages in one run. Only want to go through a max of 50 pages
            if (page > 50) {
                log.warn("Reached max page {}, stopping to avoid infinite loop.", page);
                break;
            }

            page++;

        }

        log.info("TMDb ingestion job finished. Inserted {} total movies.", insertedTotal);

    }

    /**
     * Takes the list of movies from API response and seeds into DB
     * Idempotent: skips movies that already exist (based on tmdbid)
     */
    private int seedMovies(
            TmdbMovieListResponse movieList,
            String listName
    ) {
        if (movieList == null || movieList.results() == null || movieList.results().isEmpty()) {
            log.warn("No results returned from TMDb {} endpoint - skipping seeding", listName);
            return 0;
        }

        int inserted = 0;
        int skipped = 0;

        // for each movie in the list
        for (TmdbMovieResult tmdbMovie : movieList.results()) {

            Long tmdbId = tmdbMovie.id();

            // If tmdbId is missing, we can't dedupe properly, so we skip it.
            if (tmdbId == null) {
                skipped++;
                log.debug("Movie Skipped (missing tmdbId): {}", tmdbMovie.title());
                continue;
            }

            // See if the Tmdb id already exists in db.
            if (movieServiceClient.existsByTmdbId(tmdbId)) {
                skipped++;
                log.debug("Movie Skipped (already exists): {}", tmdbMovie.title());
                continue;
            }

            // Build full URLs from TMDb's relative paths.
            String posterUrl = buildImageUrl(TMDB_POSTER_BASE_URL, tmdbMovie.poster_path());
            String backdropUrl = buildImageUrl(TMDB_BACKDROP_BASE_URL, tmdbMovie.backdrop_path());

            // Map TMDb genre IDs -> our human-readable genre names.
            var genreNames = mapTmdbGenreIdsToNames(tmdbMovie.genre_ids());

            // Extract release year.
            Integer releaseYear = extractYear(tmdbMovie.release_date());

            // Build the same DTO that the movie-service controller expects.
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

            // Delegate creation to movie-service via HTTP.
            movieServiceClient.createMovie(request);

            inserted++;
        }

        log.info("Finished inserting {}. Inserted {} movies, skipped {}", listName, inserted, skipped);

        return inserted;
    }






    // ---------- helpers below are basically identical to your MovieSeeder ----------

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
                .map(TMDB_GENRE_MAP::get)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Reads an optional --count=NN argument from the command line.
     * Falls back to ingestion.default-count if not provided.
     */
    private int resolveTargetCount(String... args) {
        int target = defaultTargetCount;  // 0 = "no limit"

        if (args == null) {
            return target;
        }

        for (String arg : args) {
            if (arg != null && arg.startsWith("--count=")) {
                String value = arg.substring("--count=".length());
                try {
                    int parsed = Integer.parseInt(value);
                    if (parsed >= 0) {
                        target = parsed;
                    } else {
                        log.warn("Ignoring non-positive --count value: {}", value);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Failed to parse --count argument '{}', using default {}", value, target);
                }
            }
        }

        return target;
    }

    /**
     * If targetCount <= 0, treat it as "no limit".
     */
    private boolean reachedTarget(int targetCount, int insertedSoFar) {
        return targetCount > 0 && insertedSoFar >= targetCount;
    }


    // Minimal TMDb genre ID -> Name map for seeding.
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
