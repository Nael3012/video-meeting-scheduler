package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import com.example.model.MeetingLinkValidator
import com.example.model.MeetingPlatform
import com.example.model.RepeatOption
import java.util.Calendar

/**
 * Centralized Handler for Android Native Apps Synchronization and System Intents:
 * - Native Android Calendar App Sync (CalendarContract.Events)
 * - Native Meeting Apps (Google Meet, Zoom, Microsoft Teams)
 * - Native Android System Alarm / Reminder Clock (AlarmClock)
 * - Native Android Share Sheet (Intent.ACTION_SEND)
 * - Native Email App (Intent.ACTION_SENDTO / mailto)
 * - Clipboard Sync with Toast feedback
 */
object NativeAppSyncHandler {

    /**
     * Synchronizes a meeting event directly to the Android Native Calendar app.
     */
    fun syncToNativeCalendar(
        context: Context,
        title: String,
        link: String,
        passcode: String,
        platform: MeetingPlatform,
        dateMillis: Long,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        repeat: RepeatOption,
        onSuccess: (String) -> Unit = {},
        onFallbackWeb: (String) -> Unit = {}
    ) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.timeInMillis = startCal.timeInMillis + 45 * 60 * 1000
        }

        val normalizedLink = MeetingLinkValidator.normalizeUrl(link)
        val description = buildString {
            append("Rapat ${platform.displayName}\n")
            append("Waktu: ${MeetingLinkValidator.formatTime12(startHour, startMinute)} - ${MeetingLinkValidator.formatTime12(endHour, endMinute)}\n")
            append("Link Rapat: $normalizedLink\n")
            if (passcode.isNotBlank()) {
                append("Passcode: ${passcode.trim()}\n")
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title.ifBlank { "Rapat ${platform.displayName}" })
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, normalizedLink)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
            repeat.rruleFreq?.let { freq ->
                putExtra(CalendarContract.Events.RRULE, "FREQ=$freq")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            onSuccess("Membuka Kalender Android Native...")
        } catch (_: Exception) {
            // If no native calendar app handles the intent, fallback to Google Calendar web URL
            val webUrl = MeetingLinkValidator.buildCalendarUrl(
                os = com.example.model.OperatingSystem.ANDROID,
                calendarSync = com.example.model.CalendarSync.GOOGLE_CALENDAR,
                meetingPlatform = platform,
                meetingTitle = title,
                meetingLink = link,
                passcode = passcode,
                meetingDateMillis = dateMillis,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                repeat = repeat,
                reminder = com.example.model.ReminderOption.TEN_MINUTES
            )
            launchBrowserUrl(context, webUrl)
            onFallbackWeb("Membuka Google Calendar Web...")
        }
    }

    /**
     * Launches the corresponding native app (Google Meet, Zoom, MS Teams) or falls back to Web.
     */
    fun launchMeetingNativeApp(
        context: Context,
        link: String,
        platform: MeetingPlatform? = null
    ) {
        val normalized = MeetingLinkValidator.normalizeUrl(link)
        val detected = platform ?: MeetingLinkValidator.detectPlatform(link)

        // Try direct URI intent first (Android handles app links if installed)
        try {
            val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(uriIntent)
            Toast.makeText(context, "Membuka ${detected?.displayName ?: "Rapat Video"}...", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            launchBrowserUrl(context, normalized)
        }
    }

    /**
     * Opens the native Android Share Sheet with pre-formatted meeting details.
     */
    fun shareMeetingDetails(
        context: Context,
        title: String,
        dateFormatted: String,
        timeRange: String,
        link: String,
        passcode: String,
        platform: String
    ) {
        val shareText = buildString {
            append("📅 Undangan Rapat: ${title.ifBlank { "Rapat $platform" }}\n")
            append("⏰ Waktu: $dateFormatted ($timeRange)\n")
            append("🌐 Link Rapat: ${MeetingLinkValidator.normalizeUrl(link)}\n")
            if (passcode.isNotBlank()) {
                append("🔑 Passcode: ${passcode.trim()}\n")
            }
            append("\nDijadwalkan via Meetflow App")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Undangan Rapat: $title")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Undangan Rapat via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membagikan.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets a native Android System Alarm / Reminder for the meeting time.
     */
    fun setNativeAlarmReminder(
        context: Context,
        hour: Int,
        minute: Int,
        title: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Pengingat Rapat: ${title.ifBlank { "Rapat Video" }}")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(alarmIntent)
            onResult(true, "Alarm pengingat disetel ke pukul ${String.format("%02d:%02d", hour, minute)}")
        } catch (_: Exception) {
            onResult(false, "Tidak dapat membuka aplikasi Alarm bawaan.")
        }
    }

    /**
     * Opens the native Android Email client with pre-filled invitation template.
     */
    fun sendEmailInvitation(
        context: Context,
        title: String,
        dateFormatted: String,
        timeRange: String,
        link: String,
        passcode: String
    ) {
        val body = buildString {
            append("Halo,\n\n")
            append("Anda diundang untuk menghadiri rapat berikut:\n\n")
            append("Judul: ${title.ifBlank { "Rapat Video" }}\n")
            append("Tanggal & Waktu: $dateFormatted ($timeRange)\n")
            append("Tautan: ${MeetingLinkValidator.normalizeUrl(link)}\n")
            if (passcode.isNotBlank()) {
                append("Passcode: ${passcode.trim()}\n")
            }
            append("\nTerima kasih,\nMeetflow Organizer")
        }

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Undangan Rapat: ${title.ifBlank { "Rapat Video" }}")
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Aplikasi Email tidak ditemukan.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies text to Android system clipboard with toast feedback.
     */
    fun copyToClipboard(context: Context, label: String, text: String, feedbackMessage: String? = null) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, feedbackMessage ?: "$label disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Helper to open URLs in system browser.
     */
    fun launchBrowserUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka tautan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
