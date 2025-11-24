//package com.microflix.movieservice.movie;
//
//import com.microflix.movieservice.movie.dto.CreateMovieRequest;
//import com.microflix.movieservice.movie.dto.MovieResponse;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class MovieServiceTest {
//
//    @Mock
//    private MovieRepository movies;                         // mock the repository so we don't hit a real DB
//
//    @InjectMocks
//    private MovieService movieService;                      // service under test
//
//    @Test
//    void getAllMovies_returnsMappedMovieResponses() {
//        // arrange
//        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//
//        Movie movie1 = new Movie();
//        movie1.setId(1L);
//        movie1.setTitle("Inception");
//        movie1.setOverview("Dream heist.");
//        movie1.setReleaseYear(2010);
//        movie1.setRuntime(148);
//        movie1.setTmdbId(27205L);
//        movie1.setCreatedAt(now);
//        movie1.setUpdatedAt(now);
//
//        Movie movie2 = new Movie();
//        movie2.setId(2L);
//        movie2.setTitle("Interstellar");
//        movie2.setOverview("Space and time.");
//        movie2.setReleaseYear(2014);
//        movie2.setRuntime(169);
//        movie2.setTmdbId(157336L);
//        movie2.setCreatedAt(now);
//        movie2.setUpdatedAt(now);
//
//        when(movies.findAll()).thenReturn(List.of(movie1, movie2));
//
//        // act
//        List<MovieResponse> responses = movieService.getAllMovies();
//
//        // assert
//        assertEquals(2, responses.size());
//
//        MovieResponse first = responses.get(0);
//        assertEquals(1L, first.id());
//        assertEquals("Inception", first.title());
//        assertEquals("Dream heist.", first.overview());
//        assertEquals(2010, first.releaseYear());
//        assertEquals(148, first.runtime());
//        assertEquals(27205L, first.tmdbId());
//        assertEquals(now, first.createdAt());
//        assertEquals(now, first.updatedAt());
//    }
//
//    @Test
//    void getMovie_whenMovieExists_returnsMappedResponse() {
//        // arrange
//        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
//
//        Movie movie = new Movie();
//        movie.setId(10L);
//        movie.setTitle("The Dark Knight");
//        movie.setOverview("Batman vs Joker.");
//        movie.setReleaseYear(2008);
//        movie.setRuntime(152);
//        movie.setTmdbId(155L);
//        movie.setCreatedAt(now);
//        movie.setUpdatedAt(now);
//
//        when(movies.findById(10L)).thenReturn(Optional.of(movie));
//
//        // act
//        MovieResponse response = movieService.getMovie(10L);
//
//        // assert
//        assertNotNull(response);
//        assertEquals(10L, response.id());
//        assertEquals("The Dark Knight", response.title());
//        assertEquals("Batman vs Joker.", response.overview());
//        assertEquals(2008, response.releaseYear());
//        assertEquals(152, response.runtime());
//        assertEquals(155L, response.tmdbId());
//        assertEquals(now, response.createdAt());
//        assertEquals(now, response.updatedAt());
//    }
//
//    @Test
//    void getMovie_whenMovieDoesNotExist_throwsIllegalArgumentException() {
//        // arrange
//        when(movies.findById(999L)).thenReturn(Optional.empty());
//
//        // act + assert
//        var ex = assertThrows(IllegalArgumentException.class,
//                () -> movieService.getMovie(999L));
//
//        assertEquals("No movie with id: 999 found", ex.getMessage());
//    }
//
//    @Test
//    void createMovie_savesMovieAndReturnsMappedResponse() {
//        // arrange
//        CreateMovieRequest request = new CreateMovieRequest(
//                "Tenet",
//                "Time inversion.",
//                2020,
//                150,
//                577922L
//        );
//
//        // When repository.save(...) is called, we simulate the DB generating an id
//        when(movies.save(any(Movie.class))).thenAnswer(invocation -> {
//            Movie m = invocation.getArgument(0);
//            m.setId(42L);                                              // simulate generated ID
//            return m;
//        });
//
//        // act
//        MovieResponse response = movieService.createMovie(request);
//
//        // assert: response is mapped correctly
//        assertNotNull(response);
//        assertEquals(42L, response.id());
//        assertEquals("Tenet", response.title());
//        assertEquals("Time inversion.", response.overview());
//        assertEquals(2020, response.releaseYear());
//        assertEquals(150, response.runtime());
//        assertEquals(577922L, response.tmdbId());
//        assertNotNull(response.createdAt());
//        assertNotNull(response.updatedAt());
//
//        // verify: repository.save was called with a Movie built from the request
//        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
//        verify(movies).save(movieCaptor.capture());
//        Movie saved = movieCaptor.getValue();
//
//        assertEquals("Tenet", saved.getTitle());
//        assertEquals("Time inversion.", saved.getOverview());
//        assertEquals(2020, saved.getReleaseYear());
//        assertEquals(150, saved.getRuntime());
//        assertEquals(577922L, saved.getTmdbId());
//        assertNotNull(saved.getCreatedAt());
//        assertNotNull(saved.getUpdatedAt());
//    }
//}
