package com.imageflow.backend.domain.auth;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.UnauthorizedException;
import com.imageflow.backend.domain.auth.dto.AuthResponse;
import com.imageflow.backend.domain.auth.dto.AuthUserResponse;
import com.imageflow.backend.domain.auth.dto.LoginRequest;
import com.imageflow.backend.domain.auth.dto.SignupRequest;
import com.imageflow.backend.domain.auth.dto.SocialProviderResponse;
import com.imageflow.backend.domain.user.AuthProvider;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.domain.user.UserPlan;
import com.imageflow.backend.domain.user.UserRepository;
import com.imageflow.backend.domain.user.UserRole;

@Service
@Transactional
public class AuthService {

    private static final int INITIAL_FREE_CREDITS = 20;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final String googleAuthUrl;
    private final String naverAuthUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @org.springframework.beans.factory.annotation.Value("${app.auth.social.google.auth-url:}") String googleAuthUrl,
            @org.springframework.beans.factory.annotation.Value("${app.auth.social.naver.auth-url:}") String naverAuthUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleAuthUrl = googleAuthUrl;
        this.naverAuthUrl = naverAuthUrl;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.isEmailVerified()) {
            throw new BadRequestException("email already exists");
        }

        if (user == null) {
            user = new User(
                    email,
                    passwordEncoder.encode(password),
                    UserPlan.FREE,
                    INITIAL_FREE_CREDITS,
                    true,
                    UserRole.USER,
                    AuthProvider.LOCAL
            );
        } else {
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setAuthProvider(AuthProvider.LOCAL);
            user.markEmailVerified();
        }

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
    public List<SocialProviderResponse> socialProviders() {
        return List.of(
                new SocialProviderResponse("GOOGLE", hasText(googleAuthUrl), normalizeOptionalUrl(googleAuthUrl)),
                new SocialProviderResponse("NAVER", hasText(naverAuthUrl), normalizeOptionalUrl(naverAuthUrl))
        );
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeOptionalUrl(String value) {
        return hasText(value) ? value.trim() : "";
    }
}
