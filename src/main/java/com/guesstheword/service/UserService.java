package com.guesstheword.service;

import com.guesstheword.dto.AuthResponse;
import com.guesstheword.dto.LoginRequest;
import com.guesstheword.dto.RegisterRequest;
import com.guesstheword.entity.Role;
import com.guesstheword.entity.User;
import com.guesstheword.exception.UserAlreadyExistsException;
import com.guesstheword.repository.UserRepository;
import com.guesstheword.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

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

        // Generate token for instant onboarding
        String token = tokenProvider.generateTokenForUser(savedUser.getUsername(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .message("User registered successfully")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .message("Login successful")
                .build();
    }
}
