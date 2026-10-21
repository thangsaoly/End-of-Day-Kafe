package com.kafe.controller;

import com.kafe.config.JwtTokenProvider;
import com.kafe.dto.*;
import com.kafe.model.User;
import com.kafe.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone number is already registered!");
        }

        String role = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole() : "CUSTOMER";
        User user = new User(
                request.getName(),
                request.getPhoneNumber(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getPhoneNumber());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.isEmailConfirmed(),
                user.getRole(),
                user.getCafeId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(request.getPhoneNumber());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password!");
        }

        User user = userOpt.get();
        if (user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account has been banned due to multiple no-shows!");
        }

        String token = tokenProvider.generateToken(user.getPhoneNumber());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.isEmailConfirmed(),
                user.getRole(),
                user.getCafeId()
        ));
    }

    // Link and generate mock verification code for email
    @PostMapping("/link-email")
    public ResponseEntity<?> linkEmail(@RequestParam String email) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        user.setEmail(email);
        user.setEmailConfirmed(false);
        // Verification code: "123456"
        user.setEmailVerificationCode("123456");
        userRepository.save(user);

        return ResponseEntity.ok("Verification code '123456' sent to: " + email);
    }

    // Verify email with code
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            return ResponseEntity.badRequest().body("Invalid email linking status!");
        }

        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(request.getCode())) {
            return ResponseEntity.badRequest().body("Incorrect verification code!");
        }

        user.setEmailConfirmed(true);
        user.setEmailVerificationCode(null);
        userRepository.save(user);

        return ResponseEntity.ok("Email successfully verified and linked!");
    }

    // Initiate password reset
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No user registered with this email!");
        }

        User user = userOpt.get();
        if (!user.isEmailConfirmed()) {
            return ResponseEntity.badRequest().body("Please verify your email address before resetting the password!");
        }

        // Mock verification code for password reset: "654321"
        user.setEmailVerificationCode("654321");
        userRepository.save(user);

        return ResponseEntity.ok("Password reset code '654321' sent to email: " + request.getEmail());
    }

    // Complete password reset
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No user registered with this email!");
        }

        User user = userOpt.get();
        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(request.getCode())) {
            return ResponseEntity.badRequest().body("Incorrect reset code!");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerificationCode(null);
        userRepository.save(user);

        return ResponseEntity.ok("Password successfully reset! Please login with your new password.");
    }
}
