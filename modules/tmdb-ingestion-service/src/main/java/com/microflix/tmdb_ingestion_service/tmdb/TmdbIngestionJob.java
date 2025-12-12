package com.microflix.tmdb_ingestion_service.tmdb;

import com.microflix.tmdb_ingestion_service.movie.dto.CreateMovieRequest;
import com.microflix.tmdb_ingestion_service.movie.MovieServiceClient;
import com.microflix.tmdb_ingestion_service.movie.dto.MovieSummary;
import com.microflix.tmdb_ingestion_service.movie.dto.TmdbMovieDetailResponse;
import com.microflix.tmdb_ingestion_service.movie.dto.UpdateMovieRequest;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieListResponse;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CommandLineRunner that runs once when the app starts.
 *
 * When the app starts, Spring will call run() once, then the app exits.
 *
 * - Default: seed new movies into movie-service from TMDb list endpoints.
 * - With --enrich: seed, then enrich only the movies seeded in this run.
 * - With --enrich-runtime: update missing runtimes on existing movies from TMDb detail.
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

    private static final int DEFAULT_UPDATE_LIMIT = 100;

    // Base URLs for TMDb images: store the full URL on the Movie entity.
    private static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String TMDB_BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780";


    /**
     * Entry Point
     */
    @Override
    public void run(String... args) {
        // Check for flags
        boolean enrichNew = hasFlag(args, "--enrich");
        boolean enrichRuntimeOnly = hasFlag(args, "--enrich-runtime");

        // If flag for runtime-only enrichment, ignore seeding.
        if (enrichRuntimeOnly) {
            int updateLimit = resolveUpdateLimit(args);
            runRuntimeEnrichment(updateLimit);
            return;
        }

        // Normal seeding path (with optional enrichment of newly seeded movies).
        int targetCount = resolveTargetCount(args);

        // Track which tmdbIds we actually inserted during this run.
        Set<Long> newlySeededTmdbIds = new LinkedHashSet<>();

        runSeeding(targetCount, newlySeededTmdbIds);

        if (enrichNew && !newlySeededTmdbIds.isEmpty()) {
            enrichSeededMovies(newlySeededTmdbIds);
        }

    }

    // ---------------- SEEDING ----------------

    /**
     * Seeding mode:
     * - Fetches lists of movies from TMDb
     * - Inserts new movies into movie-service until targetCount or limits are reached.
     * - Records tmdbIds of movies that were created in this run.
    */
    public void runSeeding(int targetCount, Set<Long> newlySeededTmdbIds) {

        log.info("Starting TMDb ingestion job with targetCount={}", targetCount);


        int insertedTotal = 0;
        int page = 1;

        // Keep going until we hit the target or decide to stop via break conditions.
        while(!reachedTarget(targetCount, insertedTotal)) {

            int added = 0;
            int insertedThisPage = 0;

            // --- Popular ---
            added = seedMovies(tmdbClient.fetchPopularMovies(page), "Popular Movies", newlySeededTmdbIds);
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Top Rated ---
            added = seedMovies(tmdbClient.fetchTopRatedMovies(page), "Top Rated Movies", newlySeededTmdbIds);
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Now Playing ---
            added = seedMovies(tmdbClient.fetchNowPlayingMovies(page), "Now Playing Movies", newlySeededTmdbIds);
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Upcoming ---
            added = seedMovies(tmdbClient.fetchUpcomingMovies(page), "Upcoming Movies", newlySeededTmdbIds);
            insertedThisPage += added;
            insertedTotal += added;
            if(reachedTarget(targetCount, insertedTotal+insertedThisPage)) break;

            // --- Discover (no early break needed here) ---
            added = seedMovies(tmdbClient.discoverPopularMovies(page), "Discover Movies", newlySeededTmdbIds);
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
     * Takes the list of movies from a TMDb API response and seeds them into movie-service.
     * Idempotent: skips movies that already exist (based on tmdbid)
     * Records tmdbIds that were newly created in {@code newlySeededTmdbIds}.
     */
    private int seedMovies(
            TmdbMovieListResponse movieList,
            String listName,
            Set<Long> newlySeededTmdbIds
    ) {
        // Return if there were no results in the movie list
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
                    null,          // runtime not included in list responses; could be fetched later
                    tmdbId,
                    posterUrl,
                    backdropUrl,
                    genreNames
            );

            // Delegate creation to movie-service via HTTP.
            movieServiceClient.createMovie(request);
            inserted++;

            // Add Tmdb id to set for each movie
            if (newlySeededTmdbIds != null) {
                newlySeededTmdbIds.add(tmdbId);
            }
        }

        log.info("Finished inserting {}. Inserted {} movies, skipped {}", listName, inserted, skipped);

        return inserted;
    }


    /**
     * Enriches movies that were seeded in this run (by tmdbId).
     * For now, it updates runtime from TMDb detail (later can add more ~ credits)
     */
    private void enrichSeededMovies(Set<Long> seededTmdbIds) {

        if (seededTmdbIds.isEmpty()) {
            log.info("No newly seeded movies to enrich.");
            return;
        }

        // Number of movies seeded in this run that we are trying to add runtimes to
        int numOfMovies = seededTmdbIds.size();

        log.info("Starting enrichment for {} newly seeded movies", numOfMovies);

        int updated = 0;

        for (Long tmdbId : seededTmdbIds) {

            try {
                // Call to Tmdb API to get more detailed individual movie info
                var detail = tmdbClient.fetchMovieDetail(tmdbId);

                // Get runtime
                Integer runtime = detail.runtime();
                if (runtime == null || runtime <= 0) {
                    log.debug("TMDb detail has no valid runtime for tmdbId={}", tmdbId);
                    continue;
                }

                // Build request to update with runtime
                var updateRequest = UpdateMovieRequest.builder()
                        .runtime(runtime)
                        .build();

                movieServiceClient.updateMovieByTmdbId(tmdbId, updateRequest);
                updated++;

                log.info("Enriched runtime for tmdbId={} updated {}/{}", tmdbId, updated, numOfMovies);

            } catch (Exception ex) {
                log.warn("Failed to enrich movie for tmdbId={} (skipping)", tmdbId, ex);
            }
        }

        log.info("Enrichment of newly seeded movies finished. Updated {} movies.", updated);

    }


    // ---------------- RUNTIME ENRICHMENT (Movie Updating) ----------------

    /**
     * Runtime enrichment mode:
     *  - Asks movie-service for movies with tmdbId but no runtime.
     *  - Fetches TMDb detail for each and patches only the runtime field.
     *  - Stops after updateLimit successful updates or when no candidates remain.
     */
    private void runRuntimeEnrichment(int updateLimit) {
        log.info("Starting TMDb runtime enrichment job with updateLimit={}", updateLimit);

        int updated = 0;
        int page = 0;
        int pageSize = 50; // how many candidates to ask movie-service for at a time

        while (updated < updateLimit) {
            List<MovieSummary> candidates =
                    movieServiceClient.findMoviesNeedingRuntime(page, pageSize);

            if (candidates.isEmpty()) {
                log.info("No more movies needing runtime enrichment. Stopping.");
                break;
            }

            for (MovieSummary movie : candidates) {
                if (updated >= updateLimit) {
                    break;
                }

                Long tmdbId = movie.tmdbId();
                if (tmdbId == null) {
                    log.warn("Skipping movie id={} (no tmdbId)", movie.id());
                    continue;
                }

                TmdbMovieDetailResponse detail;
                try {
                    detail = tmdbClient.fetchMovieDetail(tmdbId);
                } catch (Exception ex) {
                    log.warn(
                            "Failed to fetch TMDb detail for tmdbId={} (movie id={}, title='{}')",
                            tmdbId, movie.id(), movie.title(), ex
                    );
                    continue;
                }

                Integer runtime = detail.runtime();
                if (runtime == null || runtime <= 0) {
                    log.debug(
                            "TMDb detail has no runtime for tmdbId={} (movie id={}, title='{}')",
                            tmdbId, movie.id(), movie.title()
                    );
                    continue;
                }

                var updateRequest = UpdateMovieRequest.builder()
                        .runtime(runtime)
                        .build();

                movieServiceClient.updateMovie(movie.id(), updateRequest);
                updated++;

                log.info(
                        "Updated runtime for movie id={} (tmdbId={}, title='{}'); updated {}/{}",
                        movie.id(), tmdbId, movie.title(), updated, updateLimit
                );
            }

            page++;
        }

        log.info("Runtime enrichment job finished. Updated {} movies.", updated);
    }







    // ---------- Helpers ----------

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

    private boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads an optional --update-limit=NN argument from the command line.
     * Falls back to DEFAULT_UPDATE_LIMIT if not provided or invalid.
     */
    private int resolveUpdateLimit(String... args) {
        int limit = DEFAULT_UPDATE_LIMIT;

        if (args == null) {
            return limit;
        }

        for (String arg : args) {
            if (arg != null && arg.startsWith("--update-limit=")) {
                String value = arg.substring("--update-limit=".length());
                try {
                    int parsed = Integer.parseInt(value);
                    if (parsed > 0) {
                        limit = parsed;
                    } else {
                        log.warn("Ignoring non-positive --update-limit value: {}", value);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Failed to parse --update-limit argument '{}', using default {}", value, limit);
                }
            }
        }
        return limit;
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
