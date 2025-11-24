package com.microflix.movieservice.genre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JPA repository for MovieGenre entities
@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {


}
