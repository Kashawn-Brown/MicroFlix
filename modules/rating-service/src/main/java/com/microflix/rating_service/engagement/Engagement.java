package com.microflix.rating_service.engagement;


import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Entity
@Table(
        name = "engagements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_engagement_user_movie_type",
                        columnNames = {"user_id", "movie_id", "type"}
                )
        }
)
public class Engagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the user who engaged with the movie (from JWT)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ID of the movie the user engaged with
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    // Type of engagement (WATCHLIST for now)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private EngagementType type;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);


    @PrePersist
    void onCreate() {
        // app-clock in UTC
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

}
