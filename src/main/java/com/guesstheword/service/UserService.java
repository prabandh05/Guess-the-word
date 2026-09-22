package com.guesstheword.service;

import com.guesstheword.dto.AuthResponse;
import com.guesstheword.dto.RegisterRequest;
import com.guesstheword.entity.Role;
import com.guesstheword.entity.User;
import com.guesstheword.exception.UserAlreadyExistsException;
import com.guesstheword.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.PLAYER;

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .message("User registered successfully")
                .build();
    }
}
