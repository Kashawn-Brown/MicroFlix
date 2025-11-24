package com.microflix.movieservice.movie;

import com.microflix.movieservice.genre.Genre;
import com.microflix.movieservice.genre.MovieGenre;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)           // control equality
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    private String overview;

    @Column(name = "release_year")
    private Integer releaseYear;

    private Integer runtime;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @OneToMany(
            mappedBy = "movie",
            cascade = CascadeType.ALL,      // Whenever you save/update/remove a Movie, JPA cascades those operations to its MovieGenre children
            orphanRemoval = true
    )
    private Set<MovieGenre> movieGenres = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);




    ///  Helper Methods


    // Helps attach a genre to a movie in a nice, readable way (keeps both sides in sync + hides complexity)
    public void addGenre(Genre genre) {
        var movieGenre = new MovieGenre(this, genre);
        movieGenres.add(movieGenre);
    }

    // Empties the set.
    // With orphanRemoval = true, this deletes all movie_genres entries linked to that movie on flush.
    public void clearGenres() {
        // Removing from the set + orphanRemoval = true will delete join rows
        movieGenres.clear();
    }



    // Initialize timestamps on insert.
    @PrePersist
    void onCreate() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);    // app-clock in UTC
        createdAt = now;
        updatedAt = now;
    }

    // Update timestamp on any update.
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);  // bump on any update
    }
}
