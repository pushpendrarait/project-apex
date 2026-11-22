package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.UserRecord;
import com.projectapex.service.FirebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final FirebaseService firebaseService;

    public UserController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserRecord>> getCurrentUser(Authentication auth,
                                                                 @RequestParam(required = false) String fallbackUid) {
        String uid = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : fallbackUid;

        if (uid == null || "guest".equalsIgnoreCase(uid)) {
            return ResponseEntity.ok(ApiResponse.ok("Guest session", null));
        }

        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            user = firebaseService.getUsers().get("demo_player");
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserRecord>> updateCurrentUser(@RequestBody com.projectapex.dto.request.RegisterProfileRequest req,
                                                                     Authentication auth) {
        String uid = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : "demo_player";
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user != null) {
            if (req.getFullName() != null) user.setFullName(req.getFullName());
            if (req.getPhone() != null) user.setPhone(req.getPhone());
            if (req.getCity() != null) user.setCity(req.getCity());
            if (req.getPreferredSports() != null) user.setPreferredSports(req.getPreferredSports());
            user.setUpdatedAt(System.currentTimeMillis());
        }
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", user));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<UserRecord>> getUserById(@PathVariable String uid) {
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
