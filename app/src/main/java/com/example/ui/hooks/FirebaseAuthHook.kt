package com.example.ui.hooks

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FirebaseAuthHookState(
    val currentUser: FirebaseUser?,
    val isAuthenticated: Boolean,
    val isLoading: Boolean,
    val errorMessage: String?,
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val providerId: String,
    val signInWithEmail: (String, String, (Boolean, String?) -> Unit) -> Unit,
    val signUpWithEmail: (String, String, String, (Boolean, String?) -> Unit) -> Unit,
    val signInWithGoogleToken: (String, (Boolean, String?) -> Unit) -> Unit,
    val signInAnonymously: ((Boolean, String?) -> Unit) -> Unit,
    val sendPasswordReset: (String, (Boolean, String?) -> Unit) -> Unit,
    val sendEmailVerification: ((Boolean, String?) -> Unit) -> Unit,
    val signOut: () -> Unit,
    val clearError: () -> Unit
)

/**
 * Jetpack Compose Hook for managing Firebase Authentication in real-time.
 * Supports Email/Password, Google OAuth credential sign-in, Anonymous Auth,
 * Email Verification, and Password Reset with automatic state synchronization.
 */
@Composable
fun rememberFirebaseAuthHook(): FirebaseAuthHookState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Real-time Firebase Auth state listener
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val signInWithEmail: (String, String, (Boolean, String?) -> Unit) -> Unit = { email, password, onResult ->
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            val msg = "Email dan kata sandi wajib diisi."
            errorMessage = msg
            onResult(false, msg)
        } else {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                    currentUser = result.user
                    isLoading = false
                    Toast.makeText(context, "Berhasil masuk sebagai ${result.user?.email}", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                } catch (e: Exception) {
                    isLoading = false
                    val msg = e.localizedMessage ?: "Gagal masuk. Periksa email dan kata sandi Anda."
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onResult(false, msg)
                }
            }
        }
    }

    val signUpWithEmail: (String, String, String, (Boolean, String?) -> Unit) -> Unit = { name, email, password, onResult ->
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        if (cleanName.isEmpty() || cleanEmail.isEmpty() || password.length < 6) {
            val msg = "Nama, email valid, dan kata sandi minimal 6 karakter diperlukan."
            errorMessage = msg
            onResult(false, msg)
        } else {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    val user = result.user
                    if (user != null) {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                        user.updateProfile(profileUpdate).await()
                    }
                    currentUser = auth.currentUser
                    isLoading = false
                    Toast.makeText(context, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                } catch (e: Exception) {
                    isLoading = false
                    val msg = e.localizedMessage ?: "Gagal mendaftar akun baru."
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onResult(false, msg)
                }
            }
        }
    }

    val signInWithGoogleToken: (String, (Boolean, String?) -> Unit) -> Unit = { idToken, onResult ->
        if (idToken.isBlank()) {
            val msg = "Google ID Token tidak valid."
            errorMessage = msg
            onResult(false, msg)
        } else {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val result = auth.signInWithCredential(credential).await()
                    currentUser = result.user
                    isLoading = false
                    Toast.makeText(context, "Berhasil masuk dengan Google!", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                } catch (e: Exception) {
                    isLoading = false
                    val msg = e.localizedMessage ?: "Gagal otentikasi Google OAuth."
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onResult(false, msg)
                }
            }
        }
    }

    val signInAnonymously: ((Boolean, String?) -> Unit) -> Unit = { onResult ->
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val result = auth.signInAnonymously().await()
                currentUser = result.user
                isLoading = false
                Toast.makeText(context, "Masuk sebagai Pengguna Tamu.", Toast.LENGTH_SHORT).show()
                onResult(true, null)
            } catch (e: Exception) {
                isLoading = false
                val msg = e.localizedMessage ?: "Gagal masuk mode tamu."
                errorMessage = msg
                onResult(false, msg)
            }
        }
    }

    val sendPasswordReset: (String, (Boolean, String?) -> Unit) -> Unit = { email, onResult ->
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            val msg = "Masukkan email Anda untuk reset kata sandi."
            errorMessage = msg
            onResult(false, msg)
        } else {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    auth.sendPasswordResetEmail(cleanEmail).await()
                    isLoading = false
                    Toast.makeText(context, "Email reset kata sandi telah dikirim ke $cleanEmail", Toast.LENGTH_LONG).show()
                    onResult(true, null)
                } catch (e: Exception) {
                    isLoading = false
                    val msg = e.localizedMessage ?: "Gagal mengirim email reset kata sandi."
                    errorMessage = msg
                    onResult(false, msg)
                }
            }
        }
    }

    val sendEmailVerification: ((Boolean, String?) -> Unit) -> Unit = { onResult ->
        val user = auth.currentUser
        if (user == null) {
            val msg = "Tidak ada pengguna aktif."
            errorMessage = msg
            onResult(false, msg)
        } else {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    user.sendEmailVerification().await()
                    isLoading = false
                    Toast.makeText(context, "Email verifikasi dikirim ke ${user.email}", Toast.LENGTH_LONG).show()
                    onResult(true, null)
                } catch (e: Exception) {
                    isLoading = false
                    val msg = e.localizedMessage ?: "Gagal mengirim verifikasi email."
                    errorMessage = msg
                    onResult(false, msg)
                }
            }
        }
    }

    val signOutAction: () -> Unit = {
        auth.signOut()
        currentUser = null
        errorMessage = null
        Toast.makeText(context, "Berhasil keluar.", Toast.LENGTH_SHORT).show()
    }

    val user = currentUser
    val provider = user?.providerData?.firstOrNull()?.providerId ?: if (user?.isAnonymous == true) "anonymous" else "firebase"

    return FirebaseAuthHookState(
        currentUser = user,
        isAuthenticated = user != null,
        isLoading = isLoading,
        errorMessage = errorMessage,
        userId = user?.uid ?: "local_guest",
        email = user?.email ?: "guest@meetflow.app",
        displayName = user?.displayName ?: if (user?.isAnonymous == true) "Pengguna Tamu" else "Pengguna Meetflow",
        photoUrl = user?.photoUrl?.toString(),
        isEmailVerified = user?.isEmailVerified ?: false,
        providerId = provider,
        signInWithEmail = signInWithEmail,
        signUpWithEmail = signUpWithEmail,
        signInWithGoogleToken = signInWithGoogleToken,
        signInAnonymously = signInAnonymously,
        sendPasswordReset = sendPasswordReset,
        sendEmailVerification = sendEmailVerification,
        signOut = signOutAction,
        clearError = { errorMessage = null }
    )
}
