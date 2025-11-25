package com.microflix.rating_service.engagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EngagementService.
 * We mock EngagementRepository so we only test service logic, not the DB.
 */
@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

    @Mock
    private EngagementRepository engagementRepository;

    @InjectMocks
    private EngagementService engagementService;

    @Test
    void addToWatchlist_whenNotExisting_savesNewEngagement() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 42L;

        // Simulate: not currently in watchlist
        when(engagementRepository.existsByUserIdAndMovieIdAndType(
                userId, movieId, EngagementType.WATCHLIST
        )).thenReturn(false);

        // We don't care about the exact return of save here
        when(engagementRepository.save(any(Engagement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        engagementService.addToWatchlist(userId, movieId);

        // assert
        ArgumentCaptor<Engagement> captor = ArgumentCaptor.forClass(Engagement.class);
        verify(engagementRepository).save(captor.capture());

        Engagement saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(movieId, saved.getMovieId());
        assertEquals(EngagementType.WATCHLIST, saved.getType());
        assertNotNull(saved.getCreatedAt()); // PrePersist or default should set this
    }

    @Test
    void addToWatchlist_whenAlreadyExists_doesNothing() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 42L;

        when(engagementRepository.existsByUserIdAndMovieIdAndType(
                userId, movieId, EngagementType.WATCHLIST
        )).thenReturn(true);

        // act
        engagementService.addToWatchlist(userId, movieId);

        // assert
        // If it already exists, we do NOT want to call save again
        verify(engagementRepository, never()).save(any(Engagement.class));
    }

    @Test
    void removeFromWatchlist_delegatesToRepositoryDelete() {
        // arrange
        UUID userId = UUID.randomUUID();
        Long movieId = 42L;

        // act
        engagementService.removeFromWatchlist(userId, movieId);

        // assert
        verify(engagementRepository)
                .deleteByUserIdAndMovieIdAndType(userId, movieId, EngagementType.WATCHLIST);
    }

    @Test
    void getWatchlist_mapsEntitiesToEngagementItemResponses() {
        // arrange
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Engagement e1 = new Engagement();
        e1.setId(1L);
        e1.setUserId(userId);
        e1.setMovieId(10L);
        e1.setType(EngagementType.WATCHLIST);
        e1.setCreatedAt(now.minusDays(1));

        Engagement e2 = new Engagement();
        e2.setId(2L);
        e2.setUserId(userId);
        e2.setMovieId(20L);
        e2.setType(EngagementType.WATCHLIST);
        e2.setCreatedAt(now);

        when(engagementRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId, EngagementType.WATCHLIST
        )).thenReturn(List.of(e2, e1)); // most recent first

        // act
        List<EngagementItemResponse> result = engagementService.getWatchlist(userId);

        // assert
        assertEquals(2, result.size());

        EngagementItemResponse first = result.get(0);
        EngagementItemResponse second = result.get(1);

        assertEquals(userId, first.userId());
        assertEquals(20L, first.movieId());
        assertEquals(EngagementType.WATCHLIST, first.type());
        assertEquals(e2.getCreatedAt(), first.addedAt());

        assertEquals(userId, second.userId());
        assertEquals(10L, second.movieId());
        assertEquals(EngagementType.WATCHLIST, second.type());
        assertEquals(e1.getCreatedAt(), second.addedAt());
    }
}
