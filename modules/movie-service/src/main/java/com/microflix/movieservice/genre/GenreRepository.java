package com.microflix.movieservice.genre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// JPA repository for Genre entities (can look up/create genres)
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    // Case-insensitive lookup by name for reuse
    Optional<Genre> findByNameIgnoreCase(String name);
}
