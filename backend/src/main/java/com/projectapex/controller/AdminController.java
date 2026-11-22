package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.UserRecord;
import com.projectapex.model.VenueRecord;
import com.projectapex.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/owners/pending")
    public ResponseEntity<ApiResponse<List<UserRecord>>> getPendingOwners() {
        List<UserRecord> list = adminService.getPendingOwners();
        return ResponseEntity.ok(ApiResponse.ok("Pending owners retrieved", list));
    }

    @PatchMapping("/owners/{uid}/verify")
    public ResponseEntity<ApiResponse<UserRecord>> verifyOwner(
            @PathVariable String uid,
            @RequestBody Map<String, String> body
    ) {
        String status = body.getOrDefault("status", "APPROVED");
        UserRecord user = adminService.verifyOwner(uid, status);
        return ResponseEntity.ok(ApiResponse.ok("Owner verification status updated to " + status, user));
    }

    @PatchMapping("/venues/{venueId}/status")
    public ResponseEntity<ApiResponse<VenueRecord>> updateVenueStatus(
            @PathVariable String venueId,
            @RequestBody Map<String, String> body
    ) {
        String status = body.getOrDefault("status", "ACTIVE");
        VenueRecord venue = adminService.updateVenueStatus(venueId, status);
        return ResponseEntity.ok(ApiResponse.ok("Venue status updated to " + status, venue));
    }
}
