package com.kafe.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAdminTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public VerifiedToken verify(String idToken) {
        try {
            // Checks signature (Google's public keys), expiry, audience and issuer
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new VerifiedToken(token.getUid(), token.getEmail(), token.isEmailVerified(), token.getName());
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid Firebase ID token", e);
        }
    }
}
