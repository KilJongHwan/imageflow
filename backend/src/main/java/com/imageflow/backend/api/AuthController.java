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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.me(authorizationHeader);
    }
}
