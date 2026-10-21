package com.kafe.auth;

import com.kafe.model.User;
import com.kafe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Authenticates requests carrying "Authorization: Bearer <Firebase ID token>"
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    // Set for every valid token, even before the user has a backend account,
    // so POST /api/auth/session can create one
    public static final String VERIFIED_TOKEN_ATTRIBUTE = "kafe.verifiedFirebaseToken";

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserRepository userRepository;

    public FirebaseAuthenticationFilter(FirebaseTokenVerifier tokenVerifier, UserRepository userRepository) {
        this.tokenVerifier = tokenVerifier;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idToken = getBearerToken(request);
        if (idToken != null) {
            try {
                VerifiedToken token = tokenVerifier.verify(idToken);
                request.setAttribute(VERIFIED_TOKEN_ATTRIBUTE, token);

                User user = userRepository.findByFirebaseUid(token.uid()).orElse(null);
                if (user != null && !user.isBanned()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (InvalidTokenException e) {
                // Leave the request unauthenticated; protected endpoints will refuse it
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String getBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
