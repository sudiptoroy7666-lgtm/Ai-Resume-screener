package com.example.airesumescreener

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResumeAIApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this)

        // 2. ✅ ENABLE OFFLINE PERSISTENCE (must be done before any Firestore use)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d("ResumeAI", "✅ Firestore offline persistence enabled")
        } catch (e: Exception) {
            // Already set by another instance - safe to ignore
            Log.w("ResumeAI", "Firestore settings already configured")
        }

        // 3. ✅ ENABLE CRASHLYTICS (only in release builds)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // 4. Pre-authenticate with Firebase
        applicationScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                    Log.d("ResumeAI", "✅ Firebase anonymous auth successful")
                }
            } catch (e: Exception) {
                Log.e("ResumeAI", "❌ Firebase auth failed: ${e.message}")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}