package com.kafe.auth;

// An interface so tests can stand in for Firebase
public interface FirebaseTokenVerifier {

    /** @throws InvalidTokenException if the token is malformed, expired, or for another project */
    VerifiedToken verify(String idToken);
}
