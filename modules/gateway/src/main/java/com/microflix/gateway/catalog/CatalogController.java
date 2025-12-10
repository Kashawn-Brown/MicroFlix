package com.microflix.gateway.catalog;

import com.microflix.gateway.catalog.dto.CatalogMovieDetailsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Aggregated catalog endpoints exposed at the gateway.
 *
 * Takes a single request from the frontend and fans out to downstream
 * microservices (movie-service, rating-service), then stitches the
 * results into a single response shape.
 */
@RestController
@RequestMapping("api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Get everything the movie detail page needs in one hit.
     *
     * The Authorization header is optional. If present, it is forwarded
     * to rating-service so it can resolve "me" (current user) as usual.
     */
    @GetMapping("/movies/{id}")
    public Mono<CatalogMovieDetailsResponse> getMovieDetails(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader        // not doing auth, just passing along header
    ) {
        var response = catalogService.getMovieDetails(id, authHeader);

        return response;
    }

}
