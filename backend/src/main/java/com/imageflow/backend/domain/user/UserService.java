package com.imageflow.backend.domain.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.domain.user.dto.CreateUserRequest;
import com.imageflow.backend.domain.user.dto.UserResponse;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("email already exists");
        }

        int initialCredits = request.initialCredits() == null ? 0 : request.initialCredits();
        if (initialCredits < 0) {
            throw new BadRequestException("initialCredits must be zero or greater");
        }

        User user = new User(email, resolvePlan(request.plan()), initialCredits);
        return UserResponse.from(userRepository.save(user));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("email is required");
        }
        return email.trim().toLowerCase();
    }

    private UserPlan resolvePlan(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return UserPlan.FREE;
        }

        try {
            return UserPlan.valueOf(rawPlan.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("plan must be one of FREE, PRO");
        }
    }
}
