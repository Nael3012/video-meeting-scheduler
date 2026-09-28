package com.example.data

import com.example.model.CalendarSync
import com.example.model.MeetingLinkValidator
import com.example.model.MeetingPlatform
import com.example.model.OperatingSystem
import com.example.model.ReminderOption
import com.example.model.RepeatOption
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow

class KiteRepository(
    private val dao: KiteDao,
    private val firestoreService: KiteFirestoreService = KiteFirestoreService()
) {
    val allMeetings: Flow<List<MeetingEntity>> = dao.getAllMeetings()
    val activeReminders: Flow<List<ReminderEntity>> = dao.getActiveReminders()
    val userAccount: Flow<UserAccountEntity?> = dao.getUserAccount()

    suspend fun ensureInitialized() {
        val existingUser = dao.getUserAccountOnce()
        if (existingUser == null) {
            val initialUser = UserAccountEntity(
                id = "user_default",
                name = "Pengguna Kite",
                email = "guest@meetflow.app",
                isSignedIn = false,
                isVerified = false,
                googleCalendarConnected = true,
                firestoreSparkPlanConnected = true,
                themeMode = ThemeMode.SYSTEM.name
            )
            dao.upsertUserAccount(initialUser)
        }
    }

    suspend fun scheduleMeeting(
        title: String,
        link: String,
        passcode: String,
        platform: MeetingPlatform,
        os: OperatingSystem = OperatingSystem.ANDROID,
        calendarSync: CalendarSync,
        dateMillis: Long,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        repeat: RepeatOption,
        reminder: ReminderOption
    ): Long {
        val cleanTitle = title.trim().ifEmpty { "${platform.displayName} meeting" }
        val currentUser = dao.getUserAccountOnce()
        val userId = currentUser?.email?.takeIf { it.isNotBlank() } ?: "local_user"
        val duration = MeetingLinkValidator.calculateDurationMinutes(startHour, startMinute, endHour, endMinute)
        val startTimeFormatted = MeetingLinkValidator.formatTime12(startHour, startMinute)
        val endTimeFormatted = MeetingLinkValidator.formatTime12(endHour, endMinute)
        val rangeFormatted = "$startTimeFormatted - $endTimeFormatted"

        var meeting = MeetingEntity(
            userId = userId,
            title = cleanTitle,
            link = link.trim(),
            passcode = passcode.trim(),
            meetingPlatform = platform.displayName,
            operatingSystem = os.displayName,
            calendarSync = calendarSync.displayName,
            meetingDateMillis = dateMillis,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            timeLabel = startTimeFormatted,
            timeRangeLabel = rangeFormatted,
            durationMinutes = duration,
            repeatOption = repeat.displayName,
            reminderOption = reminder.displayName,
            isSyncedToCloud = false
        )

        val localId = dao.insertMeeting(meeting)

        val reminderTitle = when (reminder) {
            ReminderOption.AT_TIME -> "Rapat dimulai sekarang ($startTimeFormatted)"
            ReminderOption.TEN_MINUTES -> "Rapat dimulai dalam 10 menit ($startTimeFormatted)"
            ReminderOption.THIRTY_MINUTES -> "Rapat dimulai dalam 30 menit ($startTimeFormatted)"
            ReminderOption.ONE_HOUR -> "Rapat dimulai dalam 1 jam ($startTimeFormatted)"
        }

        val localReminder = ReminderEntity(
            userId = userId,
            meetingId = localId,
            meetingTitle = cleanTitle,
            minutesBefore = reminder.minutesBefore,
            reminderLabel = reminderTitle
        )
        val reminderId = dao.insertReminder(localReminder)

        // Sync to Cloud Firestore (Spark plan)
        val firestoreDocId = firestoreService.saveMeeting(meeting.copy(id = localId))
        if (firestoreDocId != null) {
            meeting = meeting.copy(id = localId, firestoreId = firestoreDocId, isSyncedToCloud = true)
            dao.updateMeeting(meeting)
            firestoreService.saveReminder(localReminder.copy(id = reminderId, firestoreId = firestoreDocId))
        }

        return localId
    }

    suspend fun deleteMeeting(id: Long) {
        val meeting = dao.getMeetingById(id)
        if (meeting != null) {
            firestoreService.deleteMeeting(meeting.firestoreId)
        }
        dao.deleteMeetingById(id)
        dao.deleteRemindersForMeeting(id)
    }

    suspend fun dismissReminder(id: Long) {
        dao.dismissReminder(id)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        val current = dao.getUserAccountOnce() ?: UserAccountEntity()
        val updated = current.copy(
            themeMode = mode.name,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }

    suspend fun signInWithGoogle(
        name: String,
        email: String,
        photoUrl: String? = null,
        idToken: String? = null
    ) {
        val current = dao.getUserAccountOnce()
        val cleanName = name.ifBlank { "Pengguna Google" }
        val cleanEmail = email.ifBlank { "user@gmail.com" }
        val userId = "google_" + cleanEmail.replace("@", "_").replace(".", "_")

        val updated = (current ?: UserAccountEntity()).copy(
            id = userId,
            name = cleanName,
            email = cleanEmail,
            photoUrl = photoUrl,
            idToken = idToken,
            isSignedIn = true,
            isVerified = true,
            authProvider = "google.com",
            googleCalendarConnected = true,
            firestoreSparkPlanConnected = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }

    suspend fun registerUser(
        name: String,
        email: String
    ) {
        val current = dao.getUserAccountOnce()
        val cleanName = name.trim().ifBlank { "Pengguna Baru" }
        val cleanEmail = email.trim().ifBlank { "user@meetflow.app" }
        val userId = "user_" + cleanEmail.replace("@", "_").replace(".", "_")

        val updated = (current ?: UserAccountEntity()).copy(
            id = userId,
            name = cleanName,
            email = cleanEmail,
            isSignedIn = true,
            isVerified = true,
            authProvider = "google.com",
            googleCalendarConnected = true,
            firestoreSparkPlanConnected = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }

    suspend fun signOut() {
        val current = dao.getUserAccountOnce() ?: return
        val updated = current.copy(
            isSignedIn = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }

    suspend fun toggleGoogleCalendarSync() {
        val current = dao.getUserAccountOnce() ?: return
        val updated = current.copy(
            googleCalendarConnected = !current.googleCalendarConnected,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }

    suspend fun submitAccessRequest(requesterName: String) {
        val current = dao.getUserAccountOnce()
        val updated = (current ?: UserAccountEntity(isSignedIn = false)).copy(
            pendingApprovalName = requesterName.trim(),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUserAccount(updated)
        firestoreService.syncUserAccount(updated)
    }
}
