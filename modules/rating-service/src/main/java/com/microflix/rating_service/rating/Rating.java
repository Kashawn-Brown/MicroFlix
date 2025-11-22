package com.microflix.rating_service.rating;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)           // can control equality
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long movieId;

    // Rating stored as an integer (1.0–10.0 ⇒ 10–100).
    @Column(nullable = false)
    private int ratingTimesTen;

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
