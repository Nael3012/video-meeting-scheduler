package com.example.model

import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class OperatingSystem(
    val displayName: String,
    val label: String,
    val calendarDesc: String,
    val nativeCalendar: String,
    val openLabel: String
) {
    ANDROID(
        displayName = "Android",
        label = "Android phone",
        calendarDesc = "Google Calendar atau Kalender Android",
        nativeCalendar = "Kalender Android",
        openLabel = "Buka di Android"
    ),
    IOS(
        displayName = "iOS",
        label = "iPhone / iPad",
        calendarDesc = "Google Calendar atau Apple Calendar",
        nativeCalendar = "Apple Calendar",
        openLabel = "Buka di iPhone / iPad"
    ),
    WINDOWS(
        displayName = "Windows",
        label = "Windows PC",
        calendarDesc = "Google Calendar atau Outlook",
        nativeCalendar = "Outlook Calendar",
        openLabel = "Buka di Windows"
    ),
    MACOS(
        displayName = "macOS",
        label = "Mac computer",
        calendarDesc = "Google Calendar atau Apple Calendar",
        nativeCalendar = "Apple Calendar",
        openLabel = "Buka di Mac"
    );

    companion object {
        fun fromName(name: String): OperatingSystem =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: ANDROID
    }
}

enum class CalendarSync(val displayName: String, val label: String) {
    GOOGLE_CALENDAR("Google Calendar", "Sinkronisasi cloud otomatis"),
    LOCAL_CALENDAR("Kalender Lokal", "Gunakan kalender bawaan perangkat");

    companion object {
        fun fromName(name: String): CalendarSync =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: GOOGLE_CALENDAR
    }
}

enum class MeetingPlatform(
    val displayName: String,
    val sampleLink: String
) {
    ZOOM(
        displayName = "Zoom",
        sampleLink = "https://zoom.us/j/123456789"
    ),
    GOOGLE_MEET(
        displayName = "Google Meet",
        sampleLink = "https://meet.google.com/abc-defg-hij"
    ),
    MICROSOFT_TEAMS(
        displayName = "Microsoft Teams",
        sampleLink = "https://teams.microsoft.com/l/meetup-join/19"
    );

    companion object {
        fun fromName(name: String): MeetingPlatform =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: ZOOM
    }
}

enum class RepeatOption(val displayName: String, val rruleFreq: String?) {
    DOES_NOT_REPEAT("Tidak berulang", null),
    EVERY_DAY("Setiap hari", "DAILY"),
    EVERY_WEEK("Setiap minggu", "WEEKLY"),
    EVERY_MONTH("Setiap bulan", "MONTHLY");

    companion object {
        fun fromName(name: String): RepeatOption =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: DOES_NOT_REPEAT
    }
}

enum class ReminderOption(val displayName: String, val minutesBefore: Int) {
    AT_TIME("Saat rapat dimulai", 0),
    TEN_MINUTES("10 menit sebelumnya", 10),
    THIRTY_MINUTES("30 menit sebelumnya", 30),
    ONE_HOUR("1 jam sebelumnya", 60);

    companion object {
        fun fromName(name: String): ReminderOption =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: TEN_MINUTES
    }
}

enum class AppTab(val id: String, val label: String) {
    HOME("home", "Home"),
    SCHEDULE("schedule", "Jadwal"),
    ALERTS("alerts", "Pemberitahuan"),
    ACCOUNT("account", "Akun")
}

enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("Ikuti Sistem", "Menyesuaikan dengan pengaturan tema perangkat"),
    LIGHT("Mode Terang", "Tampilan cerah dengan kontras bersih"),
    DARK("Mode Gelap", "Tampilan redup yang nyaman di mata");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AccountSubScreen {
    OVERVIEW,
    ACCOUNT_DETAIL,
    SIGN_IN,
    SIGN_UP
}

object MeetingLinkValidator {

    /**
     * Normalizes a raw URL input by prepending https:// if protocol is omitted
     */
    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    /**
     * Detects meeting platform from URL prefix or domain patterns
     * (e.g. us06web.zoom.us, meet.google.com, teams.microsoft.com)
     */
    fun detectPlatform(link: String): MeetingPlatform? {
        val normalized = normalizeUrl(link).lowercase(Locale.US)
        return when {
            normalized.contains("zoom.us") || normalized.contains("zoom.com") || normalized.contains("zoom.gov") || normalized.contains("zoom") ->
                MeetingPlatform.ZOOM
            normalized.contains("meet.google") || normalized.contains("g.co/meet") || normalized.contains("google.com/meet") ->
                MeetingPlatform.GOOGLE_MEET
            normalized.contains("teams.microsoft") || normalized.contains("teams.live") || normalized.contains("teams.office") || normalized.contains("teams.") ->
                MeetingPlatform.MICROSOFT_TEAMS
            else -> null
        }
    }

    /**
     * Accepts any meeting link input provided by the user without strict validation blocking.
     */
    fun isValidMeetingLink(link: String, platform: MeetingPlatform? = null): Boolean {
        return link.trim().isNotEmpty()
    }

    fun formatTime24(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun formatTime12(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%02d:%02d %s", h, minute, amPm)
    }

    fun calculateDurationMinutes(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Int {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        val diff = endTotal - startTotal
        return if (diff > 0) diff else 45
    }

    fun formatDurationText(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h jam $m mnt"
            h > 0 -> "$h jam"
            else -> "$m menit"
        }
    }

    fun buildCalendarUrl(
        os: OperatingSystem,
        calendarSync: CalendarSync,
        meetingPlatform: MeetingPlatform,
        meetingTitle: String,
        meetingLink: String,
        passcode: String,
        meetingDateMillis: Long,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        repeat: RepeatOption,
        reminder: ReminderOption
    ): String {
        val normalizedLink = normalizeUrl(meetingLink)
        val startCal = Calendar.getInstance().apply {
            timeInMillis = meetingDateMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = meetingDateMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If end time is before start time on the same date, roll to next day or minimum duration
        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.timeInMillis = startCal.timeInMillis + 45 * 60 * 1000
        }

        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val startDate = utcFormat.format(startCal.time)
        val endDate = utcFormat.format(endCal.time)
        val recurrence = repeat.rruleFreq?.let { "RRULE:FREQ=$it" } ?: ""
        val titleText = meetingTitle.ifBlank { "${meetingPlatform.displayName} meeting" }
        val timeRange = "${formatTime12(startHour, startMinute)} - ${formatTime12(endHour, endMinute)}"
        val details = "${meetingPlatform.displayName} meeting ($timeRange)\nJoin link: $normalizedLink\nPasscode: ${passcode.trim()}\nReminder: ${reminder.displayName}"

        if (calendarSync == CalendarSync.GOOGLE_CALENDAR) {
            val encodedText = encodeParam(titleText)
            val encodedDates = encodeParam("$startDate/$endDate")
            val encodedDetails = encodeParam(details)
            val encodedLocation = encodeParam(normalizedLink)
            val base = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$encodedText&dates=$encodedDates&details=$encodedDetails&location=$encodedLocation"
            return if (recurrence.isNotEmpty()) {
                "$base&recur=${encodeParam(recurrence)}"
            } else {
                base
            }
        }

        if (os == OperatingSystem.WINDOWS) {
            val subject = encodeParam(titleText)
            val startIso = encodeParam(isoFormat.format(startCal.time))
            val endIso = encodeParam(isoFormat.format(endCal.time))
            val body = encodeParam(details)
            val location = encodeParam(normalizedLink)
            return "https://outlook.office.com/calendar/0/deeplink/compose?subject=$subject&startdt=$startIso&enddt=$endIso&body=$body&location=$location"
        }

        val icsLines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "DTSTART:$startDate",
            "DTEND:$endDate",
            "SUMMARY:$titleText",
            "DESCRIPTION:${details.replace("\n", "\\n")}",
            "LOCATION:$normalizedLink"
        )
        if (recurrence.isNotEmpty()) {
            icsLines.add(recurrence)
        }
        icsLines.addAll(
            listOf(
                "BEGIN:VALARM",
                "TRIGGER:-PT${reminder.minutesBefore}M",
                "ACTION:DISPLAY",
                "DESCRIPTION:Meeting reminder",
                "END:VALARM",
                "END:VEVENT",
                "END:VCALENDAR"
            )
        )
        val ics = icsLines.joinToString("\r\n")
        return "data:text/calendar;charset=utf-8,${encodeParam(ics)}"
    }

    fun formatShortDate(millis: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        return formatter.format(Date(millis))
    }

    fun formatLongDate(millis: Long): String {
        val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US)
        return formatter.format(Date(millis))
    }

    fun formatMonthDay(millis: Long): String {
        val formatter = SimpleDateFormat("MMM d", Locale.US)
        return formatter.format(Date(millis))
    }

    private fun encodeParam(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
