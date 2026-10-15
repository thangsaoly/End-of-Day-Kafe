package com.kafe.android.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Firebase Authentication, behind an interface so the ViewModel can be tested without it. */
interface AuthClient {
    val isSignedIn: Boolean
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String, displayName: String)
    suspend fun signInWithGoogle(googleIdToken: String)
    suspend fun sendPasswordReset(email: String)

    /** Short-lived (1 hour) token the backend verifies; Firebase refreshes it as needed. */
    suspend fun idToken(): String?
    fun signOut()
}

class FirebaseAuthClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthClient {

    override val isSignedIn: Boolean
        get() = auth.currentUser != null

    override suspend fun signInWithEmail(email: String, password: String) = friendlyErrors {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String) = friendlyErrors {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        if (displayName.isNotBlank()) {
            val profile = UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()
            result.user?.updateProfile(profile)?.await()
        }
        Unit
    }

    override suspend fun signInWithGoogle(googleIdToken: String) = friendlyErrors {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
        Unit
    }

    override suspend fun sendPasswordReset(email: String) = friendlyErrors {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun idToken(): String? =
        auth.currentUser?.getIdToken(false)?.await()?.token

    override fun signOut() = auth.signOut()

    private suspend fun <T> friendlyErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw Exception("Password must be at least 6 characters.")
    } catch (e: FirebaseAuthUserCollisionException) {
        throw Exception("An account with this email already exists. Try logging in instead.")
    } catch (e: FirebaseAuthInvalidUserException) {
        throw Exception("Incorrect email or password.")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        // Also thrown for a malformed email address
        throw Exception("Incorrect email or password.")
    } catch (e: FirebaseNetworkException) {
        throw Exception("No internet connection. Please try again.")
    }
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
