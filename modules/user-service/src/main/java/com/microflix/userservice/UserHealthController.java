package com.microflix.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserHealthController {

    @GetMapping("api/v1/users/health")
    public String health() {
        // Simple custom endpoint for quick checks
        return "user-service: OK";
    }
}
