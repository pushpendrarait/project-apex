package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Project Apex Java Middleware is healthy and operational", Map.of(
                "status", "UP",
                "service", "project-apex-backend",
                "runtime", "Java 21 LTS + Spring Boot 3.2.x",
                "port", 8080,
                "timestamp", System.currentTimeMillis()
        )));
    }
}
