package com.microflix.rating_service.engagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementRepository extends JpaRepository<Engagement, Long> {

    Optional<Engagement> findByUserIdAndMovieIdAndType(UUID userId, Long movieId, EngagementType type);

    List<Engagement> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, EngagementType type);

    boolean existsByUserIdAndMovieIdAndType(UUID userId, Long movieId, EngagementType type);

    void deleteByUserIdAndMovieIdAndType(UUID userId, Long movieId, EngagementType type);
}
