//package com.microflix.movieservice.movie;
//
//import com.microflix.movieservice.movie.dto.CreateMovieRequest;
//import com.microflix.movieservice.movie.dto.MovieResponse;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class MovieControllerTest {
//
//    @Mock
//    private MovieService movieService;                         // mock the service
//
//    @InjectMocks
//    private MovieController controller;                        // controller under test
//
//    @Test
//    void getAllMovies_returnsOkWithMovieList() {
//        // arrange
//        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//
//        MovieResponse movie1 = new MovieResponse(
//                1L, "Inception", "Dream heist.",
//                2010, 148, 27205L, now, now
//        );
//        MovieResponse movie2 = new MovieResponse(
//                2L, "Interstellar", "Space and time.",
//                2014, 169, 157336L, now, now
//        );
//
//        List<MovieResponse> movies = List.of(movie1, movie2);
//        when(movieService.getAllMovies()).thenReturn(movies);
//
//        // act
//        ResponseEntity<List<MovieResponse>> responseEntity = controller.getAllMovies();
//
//        // assert
//        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());          // Status 200 OK
//        List<MovieResponse> body = responseEntity.getBody();
//        assertNotNull(body);
//        assertEquals(2, body.size());
//        assertEquals("Inception", body.get(0).title());
//        assertEquals("Interstellar", body.get(1).title());
//
//        // verify the service was called
//        verify(movieService).getAllMovies();
//    }
//
//    @Test
//    void getMovie_returnsOkWithSingleMovie() {
//        // arrange
//        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//
//        MovieResponse movie = new MovieResponse(
//                10L, "The Dark Knight", "Batman vs Joker.",
//                2008, 152, 155L, now, now
//        );
//
//        when(movieService.getMovie(10L)).thenReturn(movie);
//
//        // act
//        ResponseEntity<MovieResponse> responseEntity = controller.getMovie(10L);
//
//        // assert
//        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
//        MovieResponse body = responseEntity.getBody();
//        assertNotNull(body);
//        assertEquals(10L, body.id());
//        assertEquals("The Dark Knight", body.title());
//        assertEquals(2008, body.releaseYear());
//
//        // verify the service was called with the same id
//        verify(movieService).getMovie(10L);
//    }
//
//    @Test
//    void createMovie_returnsCreatedWithResponseBody() {
//        // arrange
//        CreateMovieRequest request = new CreateMovieRequest(
//                "Tenet",
//                "Time inversion.",
//                2020,
//                150,
//                577922L
//        );
//
//        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//        MovieResponse response = new MovieResponse(
//                42L, "Tenet", "Time inversion.",
//                2020, 150, 577922L, now, now
//        );
//
//        when(movieService.createMovie(any(CreateMovieRequest.class))).thenReturn(response);
//
//        // act
//        ResponseEntity<MovieResponse> responseEntity = controller.createMovie(request);
//
//        // assert
//        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());     // 201 Created
//        MovieResponse body = responseEntity.getBody();
//        assertNotNull(body);
//        assertEquals(42L, body.id());
//        assertEquals("Tenet", body.title());
//        assertEquals(150, body.runtime());
//
//        // verify the service was called with the same request
//        verify(movieService).createMovie(eq(request));
//    }
//}
