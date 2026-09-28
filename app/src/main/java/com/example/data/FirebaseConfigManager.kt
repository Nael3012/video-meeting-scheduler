package com.example.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FirebaseAppletConfig(
    val projectId: String = "",
    val appId: String = "",
    val apiKey: String = "",
    val authDomain: String = "",
    val storageBucket: String = "",
    val messagingSenderId: String = "",
    val measurementId: String = "",
    val oAuthClientId: String = "",
    val recaptchaSiteKey: String = ""
)

object FirebaseConfigManager {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedConfig: FirebaseAppletConfig? = null

    fun getConfig(context: Context): FirebaseAppletConfig {
        cachedConfig?.let { return it }
        val rawJson = context.assets.open("firebase-applet-config.json")
            .bufferedReader()
            .use { it.readText() }
        val parsed = json.decodeFromString<FirebaseAppletConfig>(rawJson)
        cachedConfig = parsed
        return parsed
    }

    fun ensureInitialized(context: Context): FirebaseAppletConfig {
        val appContext = context.applicationContext
        val config = getConfig(appContext)
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(config.projectId)
                .setApplicationId(config.appId)
                .setApiKey(config.apiKey)
                .setStorageBucket(config.storageBucket)
                .setGcmSenderId(config.messagingSenderId)
                .build()
            FirebaseApp.initializeApp(appContext, options)
        }
        return config
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val config = ensureInitialized(context)
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))

        if (config.oAuthClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(config.oAuthClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }
}
