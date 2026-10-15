package com.kafe.auth;

// The parts of a verified Firebase ID token the backend uses
public record VerifiedToken(String uid, String email, boolean emailVerified, String name) {
}
