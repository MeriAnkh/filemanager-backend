package com.filemanager.service;

import com.filemanager.dto.AuthResponse;
import com.filemanager.dto.LoginRequest;
import com.filemanager.dto.RegisterRequest;
import com.filemanager.dto.UserResponse;
import com.filemanager.entity.Role;
import com.filemanager.entity.User;
import com.filemanager.exception.EmailAlreadyExistsException;
import com.filemanager.exception.PasswordMismatchException;
import com.filemanager.repository.UserRepository;
import com.filemanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Inscription d'un nouvel utilisateur.
     * - Vérifie que les mots de passe correspondent
     * - Vérifie que l'email n'est pas déjà utilisé
     * - Hache le mot de passe avec BCrypt
     * - Génère et retourne un JWT
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validation métier : les mots de passe doivent correspondre
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas");
        }

        // Unicité de l'email
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return new AuthResponse(token, toUserResponse(saved));
    }

    /**
     * Connexion d'un utilisateur existant.
     * Spring Security vérifie email + mot de passe via AuthenticationManager.
     */
    public AuthResponse login(LoginRequest request) {
        // Lance BadCredentialsException si les credentials sont invalides
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow();

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toUserResponse(user));
    }


    /**
     * Récupère les infos d'un utilisateur par son email.
     */
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return toUserResponse(user);
    }


    /** Convertit un User en UserResponse (DTO exposé au frontend) */
    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
