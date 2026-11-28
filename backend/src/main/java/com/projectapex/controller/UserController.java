package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.AuditLog;
import com.projectapex.model.UserRecord;
import com.projectapex.service.FirebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final FirebaseService firebaseService;

    public UserController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    private String requireUid(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return auth.getName();
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserRecord>> getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.ok(ApiResponse.ok("Guest session", null));
        }

        String uid = auth.getName();
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserRecord>> updateCurrentUser(
            @RequestBody com.projectapex.dto.request.RegisterProfileRequest req,
            Authentication auth) {
        String uid = requireUid(auth);
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            throw new IllegalArgumentException("User profile not found: " + uid);
        }
        if (req.getFullName() != null && !req.getFullName().isBlank()) user.setFullName(req.getFullName().trim());
        if (req.getPhone() != null && !req.getPhone().isBlank()) user.setPhone(req.getPhone().trim());
        if (req.getCity() != null && !req.getCity().isBlank()) user.setCity(req.getCity().trim());
        if (req.getPreferredSports() != null) user.setPreferredSports(req.getPreferredSports());
        user.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistUser(user);
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_usr_upd_" + System.currentTimeMillis(), "USER_UPDATED", uid, "user", uid,
                Map.of("city", user.getCity())));
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser(Authentication auth) {
        String uid = requireUid(auth);
        UserRecord existing = firebaseService.getUsers().remove(uid);
        if (existing == null) {
            throw new IllegalArgumentException("User profile not found: " + uid);
        }
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_usr_del_" + System.currentTimeMillis(), "USER_DELETED", uid, "user", uid,
                Map.of("email", existing.getEmail() != null ? existing.getEmail() : "")));
        return ResponseEntity.ok(ApiResponse.ok("User profile deleted", null));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<UserRecord>> getUserById(@PathVariable String uid, Authentication auth) {
        String callerUid = requireUid(auth);
        if (!callerUid.equals(uid) && !isAdmin(auth)) {
            throw new AccessDeniedException("Unauthorized to view another user's profile");
        }
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
