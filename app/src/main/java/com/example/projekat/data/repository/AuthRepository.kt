package com.example.projekat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for auth operations
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Repository for Firebase Authentication operations.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    /** Current authenticated user or null */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Whether user is currently authenticated */
    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    /**
     * Observe authentication state changes as a Flow.
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Register a new user with email and password.
     */
    suspend fun register(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Registracija nije uspela")
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Prijava nije uspela")
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    /**
     * Sign out the current user.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    /**
     * Map Firebase exceptions to Serbian error messages.
     */
    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: return "Doslo je do greske"
        return when {
            message.contains("email address is badly formatted", ignoreCase = true) ->
                "Email adresa nije validna"
            message.contains("password is invalid", ignoreCase = true) ||
            message.contains("wrong password", ignoreCase = true) ->
                "Pogresna lozinka"
            message.contains("no user record", ignoreCase = true) ||
            message.contains("user may have been deleted", ignoreCase = true) ->
                "Korisnik sa ovom email adresom ne postoji"
            message.contains("email address is already in use", ignoreCase = true) ->
                "Email adresa je vec registrovana"
            message.contains("password should be at least 6 characters", ignoreCase = true) ->
                "Lozinka mora imati najmanje 6 karaktera"
            message.contains("network error", ignoreCase = true) ||
            message.contains("unable to resolve host", ignoreCase = true) ->
                "Nema internet konekcije"
            message.contains("too many requests", ignoreCase = true) ->
                "Previse pokusaja. Pokusajte ponovo kasnije."
            else -> "Doslo je do greske: $message"
        }
    }
}
