package com.filemanager.controller;

import com.filemanager.dto.AuthResponse;
import com.filemanager.dto.LoginRequest;
import com.filemanager.dto.RegisterRequest;
import com.filemanager.dto.UserResponse;
import com.filemanager.repository.UserRepository;
import com.filemanager.security.CustomUserDetailsService;
import com.filemanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body : { email, firstName, lastName, password, confirmPassword }
     * Retourne : { token, user }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(java.security.Principal principal) {
        // principal.getName() retourne l'email défini lors de l'authentification dans le JWT
        String email = principal.getName();
        // Appel au service
        UserResponse userResponse = authService.getCurrentUser(email);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * POST /api/auth/login
     * Body : { email, password }
     * Retourne : { token, user }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
