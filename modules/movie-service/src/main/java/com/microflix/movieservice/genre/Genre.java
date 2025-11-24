package com.microflix.movieservice.genre;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Genres can be linked to many movies via the movie_genres join table
    @OneToMany(mappedBy = "genre")
    private Set<MovieGenre> movieGenres = new HashSet<>();      // One Genre can be associated with many MovieGenre row


    // Convenience constructor to create a genre with a name directly
    public Genre(String name) {
        this.name = name;
    }


}
