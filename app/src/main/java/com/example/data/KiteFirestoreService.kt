package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class KiteFirestoreService {
    private val tag = "KiteFirestore"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firebase Firestore not yet initialized: ${e.message}")
            null
        }
    }

    suspend fun saveMeeting(meeting: MeetingEntity): String? {
        val db = firestore ?: return null
        return try {
            val docRef = if (!meeting.firestoreId.isNullOrBlank()) {
                db.collection("meetings").document(meeting.firestoreId)
            } else {
                db.collection("meetings").document()
            }

            val data = hashMapOf(
                "id" to docRef.id,
                "userId" to meeting.userId,
                "title" to meeting.title,
                "link" to meeting.link,
                "passcode" to meeting.passcode,
                "meetingPlatform" to meeting.meetingPlatform,
                "operatingSystem" to meeting.operatingSystem,
                "calendarSync" to meeting.calendarSync,
                "meetingDateMillis" to meeting.meetingDateMillis,
                "startHour" to meeting.startHour,
                "startMinute" to meeting.startMinute,
                "endHour" to meeting.endHour,
                "endMinute" to meeting.endMinute,
                "timeLabel" to meeting.timeLabel,
                "timeRangeLabel" to meeting.timeRangeLabel,
                "durationMinutes" to meeting.durationMinutes,
                "repeatOption" to meeting.repeatOption,
                "reminderOption" to meeting.reminderOption,
                "createdAt" to meeting.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data, SetOptions.merge()).await()
            Log.d(tag, "Meeting saved to Firestore: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e(tag, "Failed to save meeting to Firestore: ${e.message}")
            null
        }
    }

    suspend fun deleteMeeting(firestoreId: String?) {
        if (firestoreId.isNullOrBlank()) return
        val db = firestore ?: return
        try {
            db.collection("meetings").document(firestoreId).delete().await()
            Log.d(tag, "Meeting deleted from Firestore: $firestoreId")
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete meeting from Firestore: ${e.message}")
        }
    }

    suspend fun saveReminder(reminder: ReminderEntity): String? {
        val db = firestore ?: return null
        return try {
            val docRef = if (!reminder.firestoreId.isNullOrBlank()) {
                db.collection("reminders").document(reminder.firestoreId)
            } else {
                db.collection("reminders").document()
            }

            val data = hashMapOf(
                "id" to docRef.id,
                "userId" to reminder.userId,
                "meetingId" to reminder.meetingId,
                "meetingTitle" to reminder.meetingTitle,
                "minutesBefore" to reminder.minutesBefore,
                "reminderLabel" to reminder.reminderLabel,
                "isDismissed" to reminder.isDismissed,
                "createdAt" to reminder.createdAt
            )

            docRef.set(data, SetOptions.merge()).await()
            docRef.id
        } catch (e: Exception) {
            Log.e(tag, "Failed to save reminder to Firestore: ${e.message}")
            null
        }
    }

    suspend fun syncUserAccount(account: UserAccountEntity) {
        val db = firestore ?: return
        try {
            val userDocumentId = if (account.email.isNotBlank() && account.email != "guest@meetflow.app") {
                account.email.replace(".", "_")
            } else {
                account.id
            }

            val data = hashMapOf(
                "uid" to account.id,
                "displayName" to account.name,
                "email" to account.email,
                "photoUrl" to account.photoUrl,
                "idToken" to account.idToken,
                "authProvider" to account.authProvider,
                "isSignedIn" to account.isSignedIn,
                "isVerified" to account.isVerified,
                "status" to if (account.isSignedIn) "active" else "signed_out",
                "googleCalendarConnected" to account.googleCalendarConnected,
                "themeMode" to account.themeMode,
                "pendingApprovalName" to account.pendingApprovalName,
                "plan" to "Spark (Free Tier)",
                "createdAt" to account.createdAt,
                "lastLoginAt" to System.currentTimeMillis()
            )

            // Sync to user_accounts collection
            db.collection("user_accounts").document(userDocumentId)
                .set(data, SetOptions.merge()).await()

            // Also sync to users collection for standard Firebase Dashboard view
            db.collection("users").document(userDocumentId)
                .set(data, SetOptions.merge()).await()

            Log.d(tag, "User account successfully synced to Firestore Spark plan: ${account.email}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync user account to Firestore: ${e.message}")
        }
    }
}
