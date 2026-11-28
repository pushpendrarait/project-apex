package com.projectapex.controller;

import com.projectapex.config.NeonDataSourceConfig;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.service.FirebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final NeonDataSourceConfig dataSourceConfig;
    private final FirebaseService firebaseService;

    public HealthController(JdbcTemplate jdbcTemplate,
                            NeonDataSourceConfig dataSourceConfig,
                            FirebaseService firebaseService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSourceConfig = dataSourceConfig;
        this.firebaseService = firebaseService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        boolean dbConnected = false;
        try {
            Integer ping = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbConnected = (ping != null && ping == 1);
        } catch (Exception ignored) {
            dbConnected = false;
        }

        long usersCount = dbConnected
                ? safeQueryCount("SELECT COUNT(*) FROM apex_users", firebaseService.getUsers().size())
                : firebaseService.getUsers().size();
        long venuesCount = dbConnected
                ? safeQueryCount("SELECT COUNT(*) FROM apex_venues", firebaseService.getVenues().size())
                : firebaseService.getVenues().size();
        long bookingsCount = dbConnected
                ? safeQueryCount("SELECT COUNT(*) FROM apex_bookings", firebaseService.getBookings().size())
                : firebaseService.getBookings().size();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("service", "project-apex-backend");
        payload.put("hosting", "Render");
        payload.put("runtime", "Java 21 LTS + Spring Boot 3.2.x");
        payload.put("database", dataSourceConfig.getDatabaseEngine());
        payload.put("dbConnected", dbConnected);
        payload.put("usersCount", usersCount);
        payload.put("venuesCount", venuesCount);
        payload.put("bookingsCount", bookingsCount);
        payload.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.ok("Project Apex Java Middleware is healthy and operational", payload));
    }

    private long safeQueryCount(String sql, long fallback) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
