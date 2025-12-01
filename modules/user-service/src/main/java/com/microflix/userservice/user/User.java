package com.microflix.userservice.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)           // control equality - Entity equality based on id now
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class User {
    @Id @EqualsAndHashCode.Include @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Include
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    // Comma-separated roles (e.g. "USER", "ADMIN").
    @Column(nullable = false, length = 100)
    private String roles = "USER";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // Normalize email + initialize timestamps on insert.
    @PrePersist
    void onCreate() {
        if (email != null) email = email.toLowerCase();  // normalize

        var now = OffsetDateTime.now(ZoneOffset.UTC);    // app-clock in UTC
        createdAt = now;
        updatedAt = now;
    }

    // Update timestamp on any update.
    @PreUpdate
    void onUpdate() {
        if (displayName == null) displayName = email;

        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);  // bump on any update
    }


}
