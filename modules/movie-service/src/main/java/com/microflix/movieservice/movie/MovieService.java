package com.microflix.movieservice.movie;

import com.microflix.movieservice.common.errors.MovieNotFoundException;
import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {         // Encapsulates business logic for movie operations.

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Returns all movies; mapped to response DTOs.
     */
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single movie by id or throws if not found.
     */
    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        return toResponse(movie);
    }

    /**
     * Creates a new movie from the request DTO and returns the saved movie.
     */
    public MovieResponse createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();

        movie.setTitle(request.title());
        movie.setOverview(request.overview());
        movie.setReleaseYear(request.releaseYear());
        movie.setRuntime(request.runtime());
        movie.setTmdbId(request.tmdbId());

        var newMovie = movieRepository.save(movie);

        return toResponse(newMovie);
    }




    ///  Helper Function

    /**
     * Maps a Movie entity to a MovieResponse DTO.
     */
    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseYear(),
                movie.getRuntime(),
                movie.getTmdbId(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }



}
