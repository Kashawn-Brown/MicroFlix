package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.MovieResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test for the batch endpoint against H2 (PostgreSQL compat mode).
 * Proves the ordering contract survives a real JPA {@code WHERE id IN (...)} query, not
 * just the unit-level Mockito stub.
 *
 * Class is {@code @Transactional} so lazy genre collections on Movie entities resolve
 * during the service's toMovieResponse mapping — matches runtime behaviour under OSIV.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MovieServiceBatchIntegrationTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void getMoviesByIds_returnsMoviesInInputOrder_againstRealDatabase() {
        Long id1 = persistMovie("Inception");
        Long id2 = persistMovie("Interstellar");
        Long id3 = persistMovie("Tenet");
        Long id4 = persistMovie("Dunkirk");

        // Deliberately scrambled, skipping one id — exercises both ordering and the
        // silently-dropped-unknown-ids behaviour simultaneously.
        List<Long> requested = List.of(id3, id1, 9_999_999L, id4);

        List<MovieResponse> result = movieService.getMoviesByIds(requested);

        assertEquals(List.of(id3, id1, id4), result.stream().map(MovieResponse::id).toList(),
                "batch response must match input-id order with unknown ids dropped");
        assertEquals("Tenet", result.get(0).title());
        assertEquals("Inception", result.get(1).title());
        assertEquals("Dunkirk", result.get(2).title());
    }

    @Test
    void getMoviesByIds_emptyInput_returnsEmpty() {
        assertTrue(movieService.getMoviesByIds(List.of()).isEmpty());
    }

    private Long persistMovie(String title) {
        Movie m = new Movie();
        m.setTitle(title);
        return movieRepository.save(m).getId();
    }
}
