package com.microflix.movieservice.genre;

import com.microflix.movieservice.movie.Movie;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)   // keep toString small to avoid recursion
@Entity
@Table(
        name = "movie_genres",      // Maps to the movie_genres table
        uniqueConstraints = {
                // Ensure we don't create duplicate links between the same movie and genre
                @UniqueConstraint(      // adds a DB constraint
                        name = "uk_movie_genre",
                        columnNames = {"movie_id", "genre_id"}
                )
        }
)
public class MovieGenre {       // Links a single Movie to a single Genre

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many MovieGenre rows can point to one Movie
    @ManyToOne(fetch = FetchType.LAZY, optional = false)    // LAZY: JPA loads the Movie only when you call movieGenre.getMovie()
    @JoinColumn(name = "movie_id", nullable = false)        // Use movie_id column in movie_genres table as the FK to the movies table’s primary key
    @EqualsAndHashCode.Include
    private Movie movie;

    // Many MovieGenre rows can point to one Genre
    @ManyToOne(fetch = FetchType.LAZY, optional = false)    // LAZY means: JPA will fetch only if/when you actually access the field at runtime
    @JoinColumn(name = "genre_id", nullable = false)
    @EqualsAndHashCode.Include
    private Genre genre;

    // Convenience constructor for linking a Movie and a Genre
    public MovieGenre(Movie movie, Genre genre) {
        this.movie = movie;
        this.genre = genre;
    }

    @ToString.Include
    public Long getId() {
        return id;
    }

    // Each MovieGenre row ties together:
        // A specific Movie (via movie_id)
        // A specific Genre (via genre_id)
}
