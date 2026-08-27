package com.kafe.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Hardcoded 256-bit key for local development
    private static final String SECRET_STRING = "9a4f2c8d3b7a1e5c8f3d6b9e2a5c8f1d4b7a0e3c6d9a2b5c8e1f4a7b0c3d6e9f";
    private final Key key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());
    
    // Token validity: 7 days (604800000 ms)
    private final long jwtExpirationInMs = 604800000L;

    public String generateToken(String phoneNumber) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(phoneNumber)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getPhoneNumberFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Log or ignore
        }
        return false;
    }
}
