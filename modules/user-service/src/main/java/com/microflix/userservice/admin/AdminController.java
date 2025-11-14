package com.microflix.userservice.admin;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    // Only tokens with ROLE_ADMIN pass
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        // simple body so we can see it's authorized
        return Map.of("ok", true, "area", "admin");
    }
}
