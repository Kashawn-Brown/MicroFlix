package com.microflix.movieservice.movie;

import com.microflix.movieservice.common.errors.MovieNotFoundException;
import com.microflix.movieservice.genre.Genre;
import com.microflix.movieservice.genre.GenreRepository;
import com.microflix.movieservice.genre.MovieGenre;
import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import com.microflix.movieservice.tmdb.MovieSeeder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {         // Encapsulates business logic for movie operations.

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * Returns all movies; mapped to response DTOs.
     */
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toMovieResponse)
                .toList();
    }

    /**
     * Returns a single movie by id or throws if not found.
     */
    public MovieResponse getMovie(Long id) {
        var movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        return toMovieResponse(movie);
    }

    /**
     * Creates a new movie from the request DTO and returns the saved movie.
     */
    public MovieResponse createMovie(CreateMovieRequest request) {
        var movie = new Movie();

        movie.setTitle(request.title());
        movie.setOverview(request.overview());
        movie.setReleaseYear(request.releaseYear());
        movie.setRuntime(request.runtime());
        movie.setTmdbId(request.tmdbId());
        movie.setPosterUrl(request.posterUrl());
        movie.setBackdropUrl(request.backdropUrl());

        // Apply genres from the request
        applyGenresToMovie(movie, request.genres());

        var newMovie = movieRepository.save(movie);

        return toMovieResponse(newMovie);
    }

    public List<MovieResponse> searchMovies(
            String query,
            String genre,
            Integer year,
            String sort
    ) {
        // Normalize input: treat blank strings as null
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();  // cond ? true : false
        String normalizedGenre = (genre == null || genre.isBlank()) ? null : genre.trim();
        String normalizedSort = (sort == null || sort.isBlank()) ? "created_desc" : sort.trim(); // default to created_desc if empty


        // Start with an "always true" specification (cb.conjunction()) -> a conjunction with zero conjuncts (A conjunction with zero conjuncts is true)
        Specification<Movie> mainSpecification = (root, cq, cb) -> cb.conjunction();

        // Filter: title contains query (case-insensitive)
        if (normalizedQuery != null) {

            // Building title Specification
            Specification<Movie> titleSpecification = (root, cq, cb) ->
                    cb.like(
                            cb.lower(root.get("title")),
                            "%" + normalizedQuery.toLowerCase() + "%"
                    );

            // Adding to main specification
            mainSpecification = mainSpecification.and(titleSpecification);
        }

        // Filter: exact release year
        if (year != null) {

            // Building year specification
            Specification<Movie> yearSpec = (root, cq, cb) ->
                    cb.equal(root.get("releaseYear"), year);

            // Adding to main specification
            mainSpecification = mainSpecification.and(yearSpec);
        }

        // Filter: genre name (joining Movie -> MovieGenre -> Genre)
        if (normalizedGenre != null) {

            // BUilding genre specification
            Specification<Movie> genreSpec = (root, cq, cb) -> {
                // join movies.movieGenres as mg
                Join<Movie, MovieGenre> mg = root.join("movieGenres", JoinType.LEFT);
                // then join mg.genre as g
                Join<MovieGenre, Genre> g = mg.join("genre", JoinType.LEFT);

                // prevent duplicates when a movie has multiple genres
                assert cq != null;
                cq.distinct(true);

                return cb.equal(cb.lower(g.get("name")), normalizedGenre.toLowerCase());
            };

            // Adding to main specification
            mainSpecification = mainSpecification.and(genreSpec);
        }

        // Decide how to sort the results based on the sortKey
        Sort sorting = mapSort(normalizedSort);  // mapSort will return a Sort object that Spring Data uses to generate ORDER BY in SQL


        // "Give me all movies that match the rules in spec, sorted according to sort"
        var movies = movieRepository.findAll(mainSpecification, sorting);

        // Map each movie in list of movies to a movie response and return list
        return movies.stream()
                .map(this::toMovieResponse)
                .toList();
    }




    ///  Helper Function

    /**
     * Maps a Movie entity to a MovieResponse DTO.
     */
    private MovieResponse toMovieResponse(Movie movie) {

        // Extract genre names from the movieGenres join rows
        List<String> genreNames = movie.getMovieGenres().stream()
                .map(movieGenre -> movieGenre.getGenre().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        log.info("Movie: {} DTO Genres: {}, genreNames: {}", movie.getTitle(), movie.getMovieGenres(), genreNames);
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseYear(),
                movie.getRuntime(),
                movie.getTmdbId(),
                movie.getPosterUrl(),
                movie.getBackdropUrl(),
                genreNames,
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }


    /**
     * Given a Movie and a list of genre names (e.g. ["Action", "Sci-Fi"]),
     * find or create the corresponding Genre entities and attach them to the Movie
     * via MovieGenre join rows.
     */
    private void applyGenresToMovie(Movie movie, List<String> genreNames) {
        // Clear existing links first, so this method can be used for create or update
        movie.clearGenres();

        if (genreNames == null) {
            return; // nothing to do
        }

        genreNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct() // avoid duplicates like ["Action", "Action"]
                .forEach(name -> {
                    // Try to find an existing Genre by name (case-insensitive)
                    var genre = genreRepository.findByNameIgnoreCase(name)
                            .orElseGet(() -> {
                                // If it doesn't exist, create and save a new Genre
                                var newGenre = new Genre(name);
                                return genreRepository.save(newGenre);
                            });

                    // Link this movie to the genre via a MovieGenre join entity
                    movie.addGenre(genre);
                });
    }



    /**
     * Map a simple sort key string into a Spring Sort.
     * Supported keys:
     *  - created_desc (default)
     *  - created_asc
     *  - title_asc / title_desc
     *  - year_asc / year_desc
     */
    private Sort mapSort(String sortKey) {

        return switch (sortKey) {
            case "created_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "title_asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "title_desc" -> Sort.by(Sort.Direction.DESC, "title");
            case "year_asc" -> Sort.by(Sort.Direction.ASC, "releaseYear");
            case "year_desc" -> Sort.by(Sort.Direction.DESC, "releaseYear");
            case "created_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // fallback
        };
    }



}
