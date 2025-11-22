package com.microflix.rating_service;

import jakarta.ws.rs.GET;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RatingHealthController {       // Simple health check endpoint for the rating service.

    @GetMapping("/api/v1/ratings/health")
    public String health() {
        return "rating-service: OK";
    }
}
