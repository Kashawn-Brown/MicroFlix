package com.microflix.rating_service.engagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EngagementService {

    private final EngagementRepository engagementRepository;

    public EngagementService(EngagementRepository engagementRepository) {
        this.engagementRepository = engagementRepository;
    }

    /**
     * Add the given movie to the user's watchlist.
     * If it's already there, do nothing (idempotent).
     */
    @Transactional
    public void addToWatchlist(UUID userId, Long movieId) {

        // Check if already in Watchlist
        boolean exists = engagementRepository.existsByUserIdAndMovieIdAndType(userId, movieId, EngagementType.WATCHLIST);
        if (exists) {
            return; // already in watchlist
        }

        var engagement = new Engagement();

        engagement.setUserId(userId);
        engagement.setMovieId(movieId);
        engagement.setType(EngagementType.WATCHLIST);

        engagementRepository.save(engagement);

    }

    /**
     * Remove the given movie from the user's watchlist.
     * If it's not there, this is a no-op.
     */
    @Transactional
    public void removeFromWatchlist(UUID userId, Long movieId) {
        engagementRepository.deleteByUserIdAndMovieIdAndType(userId, movieId, EngagementType.WATCHLIST);
    }

    /**
     * List the current user's watchlist as movie IDs + addedAt timestamps.
     */
    @Transactional(readOnly = true)
    public List<EngagementItemResponse> getWatchlist(UUID userId) {

        return engagementRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, EngagementType.WATCHLIST)
                .stream()
                .map(e -> new EngagementItemResponse(userId, e.getMovieId(), EngagementType.WATCHLIST, e.getCreatedAt()))
                .toList();
    }
}
