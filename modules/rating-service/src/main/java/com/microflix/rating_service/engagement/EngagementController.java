package com.microflix.rating_service.engagement;

import com.microflix.rating_service.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engagements")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    /**
     * Add a movie to the current user's watchlist.
     * Idempotent: calling this multiple times has the same effect as calling it once.
     */
    @PutMapping("/watchlist/{movieId}")
    public ResponseEntity<Void> addToWatchlist(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable Long movieId
    ) {

        engagementService.addToWatchlist(user.id(), movieId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Remove a movie from the current user's watchlist.
     * Idempotent: if it's not on the watchlist, this is a no-op.
     */
    @DeleteMapping("/watchlist/{movieId}")
    public ResponseEntity<Void> removeFromWatchlist(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable Long movieId
    ) {

        engagementService.removeFromWatchlist(user.id(), movieId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get the current user's watchlist.
     */
    @GetMapping("/watchlist")
    public ResponseEntity<List<EngagementItemResponse>> getWatchlist(@AuthenticationPrincipal CurrentUser user) {

        var response = engagementService.getWatchlist(user.id());

        return ResponseEntity.ok(response);
    }

}
