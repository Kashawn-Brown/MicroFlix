package com.microflix.movieservice.movie;

import com.microflix.movieservice.genre.GenreRepository;
import com.microflix.movieservice.movie.dto.MovieResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MovieService#getMoviesByIds(List)}. The non-obvious correctness
 * property is input-order preservation: {@code findAllById} returns rows in whatever
 * order the DB hands them back, and the watchlist aggregation on the gateway depends
 * on the service restoring caller order.
 */
@ExtendWith(MockitoExtension.class)
class MovieServiceBatchTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private MovieService movieService;

    @Test
    void getMoviesByIds_preservesInputOrder_whenRepositoryReturnsOutOfOrder() {
        Movie movie1 = movieWithId(1L, "Inception");
        Movie movie2 = movieWithId(2L, "Interstellar");
        Movie movie3 = movieWithId(3L, "Tenet");

        List<Long> requested = List.of(3L, 1L, 2L);

        // Deliberately scrambled — simulates the DB returning rows by primary-key insert order,
        // not the order of the ids in the IN-list.
        when(movieRepository.findAllById(requested))
                .thenReturn(List.of(movie1, movie2, movie3));

        List<MovieResponse> result = movieService.getMoviesByIds(requested);

        assertEquals(List.of(3L, 1L, 2L), result.stream().map(MovieResponse::id).toList(),
                "response must follow the id order the caller supplied, not DB-natural order");
    }

    @Test
    void getMoviesByIds_dropsUnknownIdsSilently() {
        Movie movie1 = movieWithId(1L, "Inception");
        Movie movie3 = movieWithId(3L, "Tenet");

        // id 2 does not exist; repo only returns 1 and 3.
        when(movieRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(movie3, movie1));

        List<MovieResponse> result = movieService.getMoviesByIds(List.of(1L, 2L, 3L));

        assertEquals(List.of(1L, 3L), result.stream().map(MovieResponse::id).toList(),
                "missing ids should be dropped, surviving ids stay in caller order");
    }

    @Test
    void getMoviesByIds_emptyInput_returnsEmptyListWithoutHittingRepository() {
        List<MovieResponse> result = movieService.getMoviesByIds(List.of());

        assertTrue(result.isEmpty());
        verify(movieRepository, never()).findAllById(anyIterable());
    }

    @Test
    void getMoviesByIds_nullInput_returnsEmptyListWithoutHittingRepository() {
        List<MovieResponse> result = movieService.getMoviesByIds(null);

        assertTrue(result.isEmpty());
        verify(movieRepository, never()).findAllById(anyIterable());
    }

    @Test
    void getMoviesByIds_overCap_throwsIllegalArgumentException() {
        List<Long> tooMany = IntStream.rangeClosed(1, MovieService.MAX_BATCH_SIZE + 1)
                .mapToObj(Long::valueOf)
                .toList();

        var ex = assertThrows(IllegalArgumentException.class,
                () -> movieService.getMoviesByIds(tooMany));

        assertTrue(ex.getMessage().contains(String.valueOf(MovieService.MAX_BATCH_SIZE)));
        verify(movieRepository, never()).findAllById(anyIterable());
    }

    @Test
    void getMoviesByIds_exactCap_isAccepted() {
        List<Long> atCap = IntStream.rangeClosed(1, MovieService.MAX_BATCH_SIZE)
                .mapToObj(Long::valueOf)
                .toList();

        // Repo returns nothing — we only care that validation accepted the call through.
        when(movieRepository.findAllById(atCap)).thenReturn(new ArrayList<>());

        List<MovieResponse> result = movieService.getMoviesByIds(atCap);

        assertTrue(result.isEmpty());
    }

    private static Movie movieWithId(Long id, String title) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle(title);
        // movieGenres defaults to empty HashSet on the entity, so toMovieResponse won't NPE.
        return m;
    }
}
