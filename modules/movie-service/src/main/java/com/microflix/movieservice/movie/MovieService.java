package com.microflix.movieservice.movie;

import com.microflix.movieservice.common.errors.MovieNotFoundException;
import com.microflix.movieservice.genre.Genre;
import com.microflix.movieservice.genre.GenreRepository;
import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import com.microflix.movieservice.tmdb.MovieSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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



}
