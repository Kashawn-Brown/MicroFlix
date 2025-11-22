package com.microflix.movieservice.movie;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

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
