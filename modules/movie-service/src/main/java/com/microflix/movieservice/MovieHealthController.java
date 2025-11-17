package com.microflix.movieservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieHealthController {

    @GetMapping("/api/v1/movies/health")
    public String health() {
        return "movie-service: OK";
    }
}
