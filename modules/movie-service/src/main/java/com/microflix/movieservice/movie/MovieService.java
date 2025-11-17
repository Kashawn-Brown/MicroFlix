package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movies;

    public MovieService(MovieRepository movies) {
        this.movies = movies;
    }

    public List<MovieResponse> getAllMovies() {
        return movies.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MovieResponse getMovie(Long id) {
        Movie movie = movies.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No movie with id: " + id + " found"));

        return toResponse(movie);
    }

    public MovieResponse createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();

        movie.setTitle(request.title());
        movie.setOverview(request.overview());
        movie.setReleaseYear(request.releaseYear());
        movie.setRuntime(request.runtime());
        movie.setTmdbId(request.tmdbId());

        var newMovie = movies.save(movie);

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
