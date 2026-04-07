package com.imageflow.backend.domain.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.UnauthorizedException;
import com.imageflow.backend.domain.auth.dto.AuthResponse;
import com.imageflow.backend.domain.auth.dto.AuthUserResponse;
import com.imageflow.backend.domain.auth.dto.LoginRequest;
import com.imageflow.backend.domain.auth.dto.SignupRequest;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.domain.user.UserPlan;
import com.imageflow.backend.domain.user.UserRepository;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("email already exists");
        }

        User user = new User(email, passwordEncoder.encode(password), UserPlan.FREE, 20);
        User savedUser = userRepository.save(user);
        return new AuthResponse(jwtTokenProvider.createToken(savedUser), AuthUserResponse.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid email or password");
        }

        return new AuthResponse(jwtTokenProvider.createToken(user), AuthUserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(String authorizationHeader) {
        return AuthUserResponse.from(requireAuthenticatedUser(authorizationHeader));
    }

    @Transactional(readOnly = true)
    public User requireAuthenticatedUser(String authorizationHeader) {
        return userRepository.findById(jwtTokenProvider.parseUserId(authorizationHeader))
                .orElseThrow(() -> new UnauthorizedException("invalid access token"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("password is required");
        }
        if (password.length() < 8) {
            throw new BadRequestException("password must be at least 8 characters");
        }
        return password;
    }
}
