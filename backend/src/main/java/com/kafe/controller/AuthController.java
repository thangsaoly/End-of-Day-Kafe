package com.kafe.controller;

import com.kafe.auth.AuthService;
import com.kafe.auth.FirebaseAuthenticationFilter;
import com.kafe.auth.VerifiedToken;
import com.kafe.dto.AuthResponse;
import com.kafe.dto.SessionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Sign-up, login and password reset happen in Firebase Authentication on the device.
// The app then calls this endpoint with its Firebase ID token.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/session")
    public ResponseEntity<?> startSession(@Valid @RequestBody(required = false) SessionRequest request,
                                          HttpServletRequest httpRequest) {
        VerifiedToken token = (VerifiedToken) httpRequest.getAttribute(FirebaseAuthenticationFilter.VERIFIED_TOKEN_ATTRIBUTE);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("A valid Firebase ID token is required.");
        }
        return ResponseEntity.ok(new AuthResponse(authService.startSession(token, request)));
    }
}
