package com.microflix.movieservice.movie;

import com.microflix.movieservice.common.errors.MovieNotFoundException;
import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        return toResponse(movie);
    }

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
