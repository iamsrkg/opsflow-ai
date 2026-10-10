package com.opsflow.identity;

import com.opsflow.identity.dto.AuthDTOs.*;
import com.opsflow.shared.ApiResponse;
import com.opsflow.shared.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtAuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtAuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        UserDTO userProfile = authService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.success(userProfile));
    }
}
