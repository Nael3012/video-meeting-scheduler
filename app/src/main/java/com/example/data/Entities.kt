package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String? = null,
    val userId: String = "local_user",
    val title: String,
    val link: String,
    val passcode: String,
    val meetingPlatform: String,
    val operatingSystem: String = "Android",
    val calendarSync: String,
    val meetingDateMillis: Long,
    val startHour: Int = 10,
    val startMinute: Int = 30,
    val endHour: Int = 11,
    val endMinute: Int = 15,
    val timeLabel: String = "10:30 AM",
    val timeRangeLabel: String = "10:30 AM - 11:15 AM",
    val durationMinutes: Int = 45,
    val repeatOption: String,
    val reminderOption: String,
    val isSyncedToCloud: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String? = null,
    val userId: String = "local_user",
    val meetingId: Long,
    val meetingTitle: String,
    val minutesBefore: Int,
    val reminderLabel: String,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String = "user_default",
    val name: String = "Pengguna Kite",
    val email: String = "guest@meetflow.app",
    val photoUrl: String? = null,
    val idToken: String? = null,
    val isSignedIn: Boolean = false,
    val isVerified: Boolean = false,
    val authProvider: String = "google.com",
    val googleCalendarConnected: Boolean = true,
    val firestoreSparkPlanConnected: Boolean = true,
    val themeMode: String = "SYSTEM",
    val pendingApprovalName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
