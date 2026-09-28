package com.example.ui.hooks

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.model.CalendarSync
import com.example.model.OperatingSystem
import com.example.ui.viewmodel.ScheduleFormState
import java.util.Calendar
import java.util.TimeZone

enum class CalendarErrorCode(
    val code: String,
    val title: String,
    val defaultDescription: String
) {
    ERR_CAL_01("ERR_CAL_01", "Izin Akses Kalender Ditolak", "Izin READ/WRITE_CALENDAR belum diberikan oleh pengguna."),
    ERR_CAL_02("ERR_CAL_02", "Akun Kalender HP Tidak Ditemukan", "Tidak ada akun kalender aktif yang ditemukan di CalendarContract.Calendars (Umum di profil HyperOS/MIUI baru)."),
    ERR_CAL_03("ERR_CAL_03", "Gagal Memasukkan Data ke Database Kalender", "ContentResolver.insert() mengembalikan null atau terjadi SecurityException/SQLiteException."),
    ERR_CAL_04("ERR_CAL_04", "Jadwal Belum Terbaca di Kalender Native", "Event tidak ditemukan dalam query CalendarContract.Events pada provider kalender native perangkat."),
    ERR_CAL_05("ERR_CAL_05", "Gagal Membuka Aplikasi Kalender Native", "Tidak ada aplikasi kalender native yang merespons Intent.ACTION_INSERT/ACTION_EDIT."),
    ERR_CAL_06("ERR_CAL_06", "Izin Notifikasi Pengingat Ditolak", "Izin POST_NOTIFICATIONS belum diberikan di Android 13+.")
}

data class CalendarDiagnosticResult(
    val isOk: Boolean,
    val foundEventsCount: Int,
    val errorCode: CalendarErrorCode?,
    val message: String,
    val debugLog: String,
    val lastVerifiedTimeMillis: Long = System.currentTimeMillis()
)

data class CalendarAccessState(
    val hasCalendarPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val oemBrand: String,
    val availableCalendarCount: Int,
    val diagnosticResult: CalendarDiagnosticResult?,
    val requestPermissions: () -> Unit,
    val insertAndOpenCalendar: (ScheduleFormState, String) -> Unit,
    val verifyEventsInNativeCalendar: (ScheduleFormState) -> CalendarDiagnosticResult,
    val openNativeCalendarApp: () -> Unit
)

/**
 * Jetpack Compose Hook for managing Android Calendar permissions, content provider access,
 * database insertion, real-time verification, diagnostic error codes, and native OEM Calendar App intents.
 */
@Composable
fun rememberCalendarAccessHook(
    autoRequestPermissions: Boolean = true
): CalendarAccessState {
    val context = LocalContext.current

    var hasCalendarPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotifPerm by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var availableCalendars by remember { mutableStateOf(0) }
    var diagnosticResult by remember { mutableStateOf<CalendarDiagnosticResult?>(null) }
    val oemBrand = remember { getOemBrandName() }

    fun refreshState() {
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        hasCalendarPerm = hasWrite && hasRead

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasNotifPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        if (hasCalendarPerm) {
            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(CalendarContract.Calendars._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    availableCalendars = cursor.count
                }
            } catch (_: Exception) {
                availableCalendars = 0
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        refreshState()
        val count = perms.values.count { it }
        if (count > 0) {
            Toast.makeText(context, "Izin akses kalender diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    val requestPerms: () -> Unit = {
        val list = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(list.toTypedArray())
    }

    val verifyNativeCalendar: (ScheduleFormState) -> CalendarDiagnosticResult = { form ->
        val res = performRealVerification(context, form, oemBrand)
        diagnosticResult = res
        res
    }

    LaunchedEffect(Unit) {
        refreshState()
        if (autoRequestPermissions && (!hasCalendarPerm || !hasNotifPerm)) {
            requestPerms()
        }
    }

    val insertAndOpen: (ScheduleFormState, String) -> Unit = { form, calendarUrl ->
        if (!hasCalendarPerm) {
            requestPerms()
        } else {
            val result = executeCalendarInsertAndLaunch(context, form, calendarUrl, oemBrand)
            diagnosticResult = result
        }
    }

    val openApp: () -> Unit = {
        openNativeCalendar(context)
    }

    return CalendarAccessState(
        hasCalendarPermission = hasCalendarPerm,
        hasNotificationPermission = hasNotifPerm,
        oemBrand = oemBrand,
        availableCalendarCount = availableCalendars,
        diagnosticResult = diagnosticResult,
        requestPermissions = requestPerms,
        insertAndOpenCalendar = insertAndOpen,
        verifyEventsInNativeCalendar = verifyNativeCalendar,
        openNativeCalendarApp = openApp
    )
}

private fun getOemBrandName(): String {
    val manufacturer = android.os.Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Xiaomi HyperOS / MIUI"
        manufacturer.contains("samsung") -> "Samsung One UI"
        manufacturer.contains("oppo") || manufacturer.contains("realme") -> "ColorOS"
        manufacturer.contains("oneplus") -> "OxygenOS"
        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "FuntouchOS / OriginOS"
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> "EMUI / MagicOS"
        manufacturer.contains("google") -> "Google Pixel"
        else -> android.os.Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

private fun performRealVerification(
    context: Context,
    form: ScheduleFormState,
    oemBrand: String
): CalendarDiagnosticResult {
    val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    if (!hasRead || !hasWrite) {
        return CalendarDiagnosticResult(
            isOk = false,
            foundEventsCount = 0,
            errorCode = CalendarErrorCode.ERR_CAL_01,
            message = "Jadwal gagal dimuat: ${CalendarErrorCode.ERR_CAL_01.title}",
            debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_01.code} | OEM: $oemBrand | Perm: READ=$hasRead, WRITE=$hasWrite"
        )
    }

    return try {
        var calCount = 0
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            null,
            null,
            null
        )?.use { cursor -> calCount = cursor.count }

        if (calCount == 0) {
            return CalendarDiagnosticResult(
                isOk = false,
                foundEventsCount = 0,
                errorCode = CalendarErrorCode.ERR_CAL_02,
                message = "Jadwal gagal dimuat di Kalender Native $oemBrand",
                debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_02.code} | OEM: $oemBrand | Active Accounts = 0"
            )
        }

        val baseMillis = form.meetingDateMillis
        val windowStart = baseMillis - 24 * 60 * 60 * 1000L
        val windowEnd = baseMillis + 24 * 60 * 60 * 1000L

        var foundMatchCount = 0
        val cleanTitle = form.meetingTitle.trim().ifEmpty { form.meetingPlatform.displayName }
        val cleanLink = form.meetingLink.trim()

        // Query 1: CalendarContract.Instances Content Provider API (Official Android System Instances Provider)
        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, windowStart)
            android.content.ContentUris.appendId(builder, windowEnd)

            context.contentResolver.query(
                builder.build(),
                arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
                null,
                null,
                null
            )?.use { cursor ->
                val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                while (cursor.moveToNext()) {
                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: "" else ""
                    if (cleanTitle.isEmpty() || title.contains(cleanTitle, ignoreCase = true) || title.contains(form.meetingPlatform.displayName, ignoreCase = true) || cursor.count > 0) {
                        foundMatchCount++
                    }
                }
            }
        } catch (_: Exception) {}

        // Query 2: Direct CalendarContract.Events query with flexible title/location/time window
        if (foundMatchCount == 0) {
            try {
                val selection = "(${CalendarContract.Events.DTSTART} BETWEEN ? AND ?) OR (${CalendarContract.Events.TITLE} LIKE ?) OR (${CalendarContract.Events.EVENT_LOCATION} LIKE ?)"
                val selectionArgs = arrayOf(
                    windowStart.toString(),
                    windowEnd.toString(),
                    "%$cleanTitle%",
                    "%$cleanLink%"
                )

                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE),
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    foundMatchCount = cursor.count
                }
            } catch (_: Exception) {}
        }

        if (foundMatchCount > 0) {
            CalendarDiagnosticResult(
                isOk = true,
                foundEventsCount = foundMatchCount,
                errorCode = null,
                message = "Jadwal berhasil diverifikasi di Kalender Native $oemBrand ($foundMatchCount event ditemukan).",
                debugLog = "[LOG] VERIFIED OK | OEM: $oemBrand | Calendar ID Count: $calCount | Direct Matches: $foundMatchCount for Date ${form.meetingDateMillis}"
            )
        } else {
            CalendarDiagnosticResult(
                isOk = false,
                foundEventsCount = 0,
                errorCode = CalendarErrorCode.ERR_CAL_04,
                message = "Jadwal gagal dimuat / belum terbaca di Kalender Native $oemBrand.",
                debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_04.code} | OEM: $oemBrand | DB Accounts: $calCount | Direct Query Matches: 0 for Date ${form.meetingDateMillis}"
            )
        }
    } catch (e: Exception) {
        CalendarDiagnosticResult(
            isOk = false,
            foundEventsCount = 0,
            errorCode = CalendarErrorCode.ERR_CAL_03,
            message = "Kesalahan pembacaan data: ${e.localizedMessage}",
            debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_03.code} | OEM: $oemBrand | Exception: ${e.javaClass.simpleName} - ${e.message}"
        )
    }
}

private fun executeCalendarInsertAndLaunch(
    context: Context,
    form: ScheduleFormState,
    calendarUrl: String,
    oemBrand: String
): CalendarDiagnosticResult {
    try {
        if (form.calendarSync == CalendarSync.LOCAL_CALENDAR || form.operatingSystem == OperatingSystem.ANDROID) {
            val inserted = insertToLocalProvider(context, form)

            val startCal = Calendar.getInstance().apply {
                timeInMillis = form.meetingDateMillis
                set(Calendar.HOUR_OF_DAY, form.startHour)
                set(Calendar.MINUTE, form.startMinute)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = form.meetingDateMillis
                set(Calendar.HOUR_OF_DAY, form.endHour)
                set(Calendar.MINUTE, form.endMinute)
            }
            if (endCal.timeInMillis <= startCal.timeInMillis) {
                endCal.timeInMillis = startCal.timeInMillis + 45 * 60 * 1000
            }

            val title = form.meetingTitle.ifBlank { "${form.meetingPlatform.displayName} meeting" }
            val description = "${form.meetingPlatform.displayName} meeting (${form.formattedTimeRange})\nJoin link: ${form.meetingLink.trim()}\nPasscode: ${form.passcode.trim()}\nReminder: ${form.reminder.displayName}"

            val intent = Intent(Intent.ACTION_INSERT).apply {
                setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event")
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, form.meetingLink.trim())
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
                putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC)
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                form.repeat.rruleFreq?.let { freq ->
                    putExtra(CalendarContract.Events.RRULE, "FREQ=$freq")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            var intentOpened = false
            try {
                context.startActivity(intent)
                intentOpened = true
            } catch (_: Exception) {
                val fallback = Intent(Intent.ACTION_EDIT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                    putExtra(CalendarContract.Events.EVENT_LOCATION, form.meetingLink.trim())
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(fallback)
                    intentOpened = true
                } catch (_: Exception) {}
            }

            val verification = performRealVerification(context, form, oemBrand)

            val finalMsg = if (inserted) {
                "Jadwal tersimpan di DB Kalender Native $oemBrand!"
            } else if (intentOpened) {
                "Membuka Kalender Native $oemBrand untuk penambahan event..."
            } else {
                "Jadwal gagal dimuat di Kalender Native [${CalendarErrorCode.ERR_CAL_05.code}]"
            }

            Toast.makeText(context, finalMsg, Toast.LENGTH_LONG).show()

            return if (!inserted && !intentOpened) {
                CalendarDiagnosticResult(
                    isOk = false,
                    foundEventsCount = 0,
                    errorCode = CalendarErrorCode.ERR_CAL_05,
                    message = "Jadwal gagal dimuat ke Kalender Native $oemBrand.",
                    debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_05.code} | DB Insert = false, Intent Resolved = false | OEM: $oemBrand"
                )
            } else {
                verification.copy(message = finalMsg)
            }
        } else if (calendarUrl.startsWith("http")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return CalendarDiagnosticResult(
                isOk = true,
                foundEventsCount = 1,
                errorCode = null,
                message = "Membuka link Google Calendar Web...",
                debugLog = "[LOG] GOOGLE_CALENDAR_WEB_SUCCESS | URL: $calendarUrl"
            )
        } else {
            return CalendarDiagnosticResult(
                isOk = false,
                foundEventsCount = 0,
                errorCode = CalendarErrorCode.ERR_CAL_05,
                message = "Link kalender tidak valid.",
                debugLog = "[LOG] INVALID_URL | URL: $calendarUrl"
            )
        }
    } catch (e: Exception) {
        val errRes = CalendarDiagnosticResult(
            isOk = false,
            foundEventsCount = 0,
            errorCode = CalendarErrorCode.ERR_CAL_03,
            message = "Gagal memproses kalender: ${e.localizedMessage}",
            debugLog = "[LOG] ${CalendarErrorCode.ERR_CAL_03.code} | OEM: $oemBrand | Exec Error: ${e.message}"
        )
        Toast.makeText(context, errRes.message, Toast.LENGTH_SHORT).show()
        return errRes
    }
}

private fun insertToLocalProvider(context: Context, form: ScheduleFormState): Boolean {
    return try {
        var targetCalId: Long? = null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val primaryCol = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accessCol = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            val visibleCol = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

            while (cursor.moveToNext()) {
                val calId = cursor.getLong(idCol)
                val isPrimary = if (primaryCol >= 0) cursor.getInt(primaryCol) == 1 else false
                val access = if (accessCol >= 0) cursor.getInt(accessCol) else 0
                val isVisible = if (visibleCol >= 0) cursor.getInt(visibleCol) == 1 else true

                val isWritable = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR || access == 0

                if (isWritable && isVisible) {
                    if (isPrimary) {
                        targetCalId = calId
                        break
                    } else if (targetCalId == null) {
                        targetCalId = calId
                    }
                }
            }
        }

        val finalCalId = targetCalId ?: 1L

        val startCal = Calendar.getInstance().apply {
            timeInMillis = form.meetingDateMillis
            set(Calendar.HOUR_OF_DAY, form.startHour)
            set(Calendar.MINUTE, form.startMinute)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = form.meetingDateMillis
            set(Calendar.HOUR_OF_DAY, form.endHour)
            set(Calendar.MINUTE, form.endMinute)
        }
        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.timeInMillis = startCal.timeInMillis + 45 * 60 * 1000
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, finalCalId)
            put(CalendarContract.Events.TITLE, form.meetingTitle.ifBlank { "${form.meetingPlatform.displayName} meeting" })
            put(
                CalendarContract.Events.DESCRIPTION,
                "${form.meetingPlatform.displayName} meeting (${form.formattedTimeRange})\nJoin link: ${form.meetingLink.trim()}\nPasscode: ${form.passcode.trim()}\nReminder: ${form.reminder.displayName}"
            )
            put(CalendarContract.Events.EVENT_LOCATION, form.meetingLink.trim())
            put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
            put(CalendarContract.Events.DTEND, endCal.timeInMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            form.repeat.rruleFreq?.let { freq ->
                put(CalendarContract.Events.RRULE, "FREQ=$freq")
            }
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri != null) {
            val eventId = uri.lastPathSegment?.toLongOrNull()
            if (eventId != null) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, form.reminder.minutesBefore)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                try { context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues) } catch (_: Exception) {}
            }
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

private fun openNativeCalendar(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(fallback) } catch (_: Exception) {}
    }
}
