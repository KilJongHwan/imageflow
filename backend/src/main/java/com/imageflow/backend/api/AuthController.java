package com.imageflow.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.imageflow.backend.domain.auth.AuthService;
import com.imageflow.backend.domain.auth.dto.AuthResponse;
import com.imageflow.backend.domain.auth.dto.AuthUserResponse;
import com.imageflow.backend.domain.auth.dto.LoginRequest;
import com.imageflow.backend.domain.auth.dto.SignupRequest;
import com.imageflow.backend.domain.auth.dto.SocialProviderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication and session endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up", description = "Creates a local account and returns a JWT token.")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token.")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/providers")
    @Operation(summary = "List social providers", description = "Returns configured Google/Naver social login entry points.")
    public java.util.List<SocialProviderResponse> providers() {
        return authService.socialProviders();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns the authenticated user derived from the Authorization header.")
    public AuthUserResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.me(authorizationHeader);
    }
}
