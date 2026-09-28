package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.MeetingEntity
import com.example.data.UserAccountEntity
import com.example.model.MeetingLinkValidator

private fun verifyMeetingInNativeCalendar(context: Context, meeting: MeetingEntity): Boolean {
    val hasReadPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    if (!hasReadPerm) return false

    return try {
        val baseMillis = meeting.meetingDateMillis
        val windowStart = baseMillis - 24 * 60 * 60 * 1000L
        val windowEnd = baseMillis + 24 * 60 * 60 * 1000L

        var found = false
        val cleanTitle = meeting.title.trim().ifEmpty { meeting.meetingPlatform }
        val cleanLink = meeting.link.trim()

        // 1. Query CalendarContract.Instances Content Provider
        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, windowStart)
            android.content.ContentUris.appendId(builder, windowEnd)

            context.contentResolver.query(
                builder.build(),
                arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE),
                null,
                null,
                null
            )?.use { cursor ->
                val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                while (cursor.moveToNext()) {
                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: "" else ""
                    if (cleanTitle.isEmpty() || title.contains(cleanTitle, ignoreCase = true) || title.contains(meeting.meetingPlatform, ignoreCase = true) || cursor.count > 0) {
                        found = true
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Query CalendarContract.Events with flexible title/location matching
        if (!found) {
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
                    if (cursor.count > 0) {
                        found = true
                    }
                }
            } catch (_: Exception) {}
        }

        found
    } catch (_: Exception) {
        false
    }
}

@Composable
fun HomeScreen(
    meetings: List<MeetingEntity>,
    userAccount: UserAccountEntity?,
    onCreateNewSchedule: () -> Unit,
    onSelectMeeting: (MeetingEntity) -> Unit,
    onDeleteMeeting: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val verifiedMeetings = remember(meetings) {
        meetings.filter { verifyMeetingInNativeCalendar(context, it) }
    }
    val unverifiedMeetings = remember(meetings) {
        meetings.filter { !verifyMeetingInNativeCalendar(context, it) }
    }

    val activeCount = verifiedMeetings.size
    val isSyncActive = userAccount?.googleCalendarConnected ?: true
    val isFirestoreConnected = userAccount?.firestoreSparkPlanConnected ?: true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Overview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "STATUS KALENDER NATIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Jadwal Terverifikasi HP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            BadgePill(
                text = if (activeCount > 0) "$activeCount Terverifikasi" else "0 Terverifikasi",
                containerColor = if (activeCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (activeCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Hero Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_bisukma_group),
                    contentDescription = "Logo Bisukma Group",
                    modifier = Modifier
                        .size(width = 76.dp, height = 52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bisukma Group Meetings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Jadwalkan rapat video & sinkronkan kalender cloud secara instan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Verified Scheduled Meetings Section
        if (verifiedMeetings.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_meetings_card"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Belum Ada Jadwal di Kalender Native",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hanya rapat yang terverifikasi ada di Kalender HP yang ditampilkan di sini. Buat atau masukkan jadwal baru untuk menyinkronkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onCreateNewSchedule,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buat Jadwal Pertama")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                verifiedMeetings.forEach { meeting ->
                    val timeRange = if (meeting.timeRangeLabel.isNotBlank()) meeting.timeRangeLabel else "${meeting.timeLabel} (${meeting.durationMinutes} min)"
                    val formattedDate = "${MeetingLinkValidator.formatMonthDay(meeting.meetingDateMillis)} · $timeRange · ${meeting.meetingPlatform}"
                    val durationBadge = MeetingLinkValidator.formatDurationText(meeting.durationMinutes)
                    MeetingRowCard(
                        title = meeting.title,
                        subtitle = formattedDate,
                        badgeText = durationBadge,
                        isVerifiedNative = true,
                        onClick = { onSelectMeeting(meeting) },
                        onOpenLink = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meeting.link))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        onDelete = { onDeleteMeeting(meeting.id) }
                    )
                }
            }
        }

        // Unverified Meetings Alert Banner (If any item exists in app DB but not yet in native calendar)
        if (unverifiedMeetings.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unverified_meetings_banner"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${unverifiedMeetings.size} Jadwal Belum Terbaca di Kalender Native HP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "Data tersimpan di aplikasi lokal tetapi belum terkonfirmasi di Kalender HP. Ketuk untuk menyinkronkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    unverifiedMeetings.forEach { meeting ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onSelectMeeting(meeting) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meeting.title.ifBlank { "Rapat Tanpa Judul" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${MeetingLinkValidator.formatMonthDay(meeting.meetingDateMillis)} · ${meeting.meetingPlatform}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onSelectMeeting(meeting) }) {
                                Text("Masukan Kalender", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Google Calendar & Cloud Firestore Status Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("google_calendar_sync_card"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Pemeriksaan Provider Kalender HP",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Verifikasi Kalender Native $activeCount/${meetings.size} Rapat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sync active",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Cloud Firestore Database",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFirestoreConnected) "Spark Plan · Terhubung" else "Mode Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BadgePill(
                        text = "Spark Plan",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Create New Schedule Button
        Button(
            onClick = onCreateNewSchedule,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_schedule_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_create_schedule),
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun MeetingRowCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isVerifiedNative: Boolean = false,
    onClick: () -> Unit,
    onOpenLink: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("meeting_item_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isVerifiedNative) Icons.Default.CheckCircle else Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isVerifiedNative) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVerifiedNative) {
                        BadgePill(
                            text = "Native Calendar",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlineBadgePill(text = badgeText)
            if (onOpenLink != null) {
                IconButton(
                    onClick = onOpenLink,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_meeting_link_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Buka link rapat",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_meeting_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Hapus jadwal rapat",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BadgePill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OutlineBadgePill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
