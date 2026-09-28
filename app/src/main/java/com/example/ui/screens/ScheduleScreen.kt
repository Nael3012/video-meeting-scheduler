package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import java.util.Calendar
import java.util.TimeZone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.CalendarSync
import com.example.model.MeetingLinkValidator
import com.example.model.MeetingPlatform
import com.example.model.OperatingSystem
import com.example.model.ReminderOption
import com.example.model.RepeatOption
import com.example.ui.hooks.rememberCalendarAccessHook
import com.example.ui.viewmodel.ScheduleFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    form: ScheduleFormState,
    onCalendarSyncChange: (CalendarSync) -> Unit,
    onPlatformChange: (MeetingPlatform) -> Unit,
    onTitleChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onPasscodeChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onStartTimeChange: (Int, Int) -> Unit,
    onEndTimeChange: (Int, Int) -> Unit,
    onSetPresetDuration: (Int) -> Unit,
    onRepeatChange: (RepeatOption) -> Unit,
    onReminderChange: (ReminderOption) -> Unit,
    onFillSample: () -> Unit,
    onScheduleMeeting: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendarHook = rememberCalendarAccessHook(autoRequestPermissions = true)
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlineBadgePill(text = stringResource(R.string.schedule_next_badge))
            Text(
                text = form.meetingTitle.ifBlank { "Jadwal Rapat Baru" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tentukan waktu mulai, selesai, dan tautan rapat video dengan standar Android Material You.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Time & Duration Summary Card (Material You)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            text = MeetingLinkValidator.formatMonthDay(form.meetingDateMillis) + ", " + form.formattedTimeRange,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${form.formattedDuration} · ${form.meetingPlatform.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BadgePill(
                    text = form.formattedDuration,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Meeting Platform Selector
        SelectorField<MeetingPlatform>(
            label = stringResource(R.string.schedule_platform_label),
            selectedText = form.meetingPlatform.displayName,
            leadingIcon = Icons.Default.Videocam,
            testTag = "meeting_platform_selector",
            options = MeetingPlatform.entries,
            optionLabel = { it.displayName },
            onSelect = { onPlatformChange(it) }
        )

        // Meeting Title Field
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.schedule_title_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = form.meetingTitle,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meeting_title_input"),
                placeholder = {
                    Text(
                        text = "Contoh: Diskusi Tim Proyek & Evaluasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Meeting Date Picker
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.schedule_date_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
                    .testTag("meeting_date_button"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = MeetingLinkValidator.formatLongDate(form.meetingDateMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Pilih tanggal rapat",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // TIME RANGE SECTION: Start Time & End Time ("Jam Berapa Sampai Jam Berapa")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waktu Rapat (Jam Mulai - Selesai)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Durasi: ${form.formattedDuration}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Start Time and End Time Dual Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Time Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showStartTimePicker = true }
                        .testTag("start_time_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mulai",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = form.formattedStartTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "sampai",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // End Time Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showEndTimePicker = true }
                        .testTag("end_time_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Selesai",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = form.formattedEndTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick Duration Preset Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(15 to "15 m", 30 to "30 m", 45 to "45 m", 60 to "1 jam", 90 to "1.5 jam").forEach { (durationMin, label) ->
                    val isSelected = form.durationMinutes == durationMin
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetPresetDuration(durationMin) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("preset_duration_${durationMin}")
                    )
                }
            }
        }

        // Meeting Link Input with Paste Button & Quick Sample Action
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedule_link_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                AssistChip(
                    onClick = onFillSample,
                    label = {
                        Text(
                            text = "Isi contoh tautan ${form.meetingPlatform.displayName}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("fill_sample_link_chip")
                )
            }
            OutlinedTextField(
                value = form.meetingLink,
                onValueChange = onLinkChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meeting_link_input"),
                placeholder = {
                    Text(
                        text = form.meetingPlatform.sampleLink,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            readClipboardText(context)?.let { onLinkChange(it) }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("paste_link_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = stringResource(R.string.schedule_paste_link),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Passcode Input with Paste Button
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.schedule_passcode_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = form.passcode,
                onValueChange = onPasscodeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meeting_passcode_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.schedule_passcode_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            readClipboardText(context)?.let { onPasscodeChange(it) }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("paste_passcode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = stringResource(R.string.schedule_paste_passcode),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Calendar Access Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.schedule_calendar_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            SelectorDropdownBox<CalendarSync>(
                selectedText = form.calendarSync.displayName,
                leadingIcon = Icons.Default.CalendarMonth,
                testTag = "calendar_sync_selector",
                options = CalendarSync.entries,
                optionLabel = { option ->
                    "${option.displayName} (${option.label})"
                },
                onSelect = { onCalendarSyncChange(it) }
            )
        }

        // Repeat & Reminder Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_repeat_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                SelectorDropdownBox<RepeatOption>(
                    selectedText = form.repeat.displayName,
                    leadingIcon = null,
                    testTag = "repeat_selector",
                    options = RepeatOption.entries,
                    optionLabel = { it.displayName },
                    onSelect = { onRepeatChange(it) }
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_reminder_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                SelectorDropdownBox<ReminderOption>(
                    selectedText = form.reminder.displayName,
                    leadingIcon = null,
                    testTag = "reminder_selector",
                    options = ReminderOption.entries,
                    optionLabel = { it.displayName },
                    onSelect = { onReminderChange(it) }
                )
            }
        }

        // Animated Flow Preview Summary Card (Material You)
        AnimatedContent(
            targetState = form,
            label = "flow_summary_card"
        ) { state ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flow_summary_card"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Alur Kalender Android Native",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.calendarSync.displayName} akan membuka rapat ${state.meetingPlatform.displayName} (${state.formattedTimeRange}) pada ${
                                MeetingLinkValidator.formatMonthDay(state.meetingDateMillis)
                            }.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.repeat.displayName} · Pengingat ${state.reminder.displayName.lowercase()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val hasCalendarPerm = calendarHook.hasCalendarPermission
        val hasNotifPerm = calendarHook.hasNotificationPermission

        // Comprehensive Device Permission Control Card (HyperOS / OEM Ready)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_permission_card"),
            shape = RoundedCornerShape(18.dp),
            color = if (hasCalendarPerm && hasNotifPerm) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, if (hasCalendarPerm && hasNotifPerm) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Status Izin Perangkat Android (${calendarHook.oemBrand})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    BadgePill(
                        text = if (hasCalendarPerm) "Izin Aktif" else "Izin Diperlukan",
                        containerColor = if (hasCalendarPerm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        contentColor = if (hasCalendarPerm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                    )
                }

                Text(
                    text = "Diperlukan izin Kalender (READ/WRITE) dan Notifikasi agar jadwal dapat langsung dimasukkan ke Kalender HP ${calendarHook.oemBrand}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasCalendarPerm || !hasNotifPerm) {
                    Button(
                        onClick = { calendarHook.requestPermissions() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Minta Izin Kalender & Notifikasi", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Primary Action Button (Material You Filled Button)
        Button(
            onClick = {
                onScheduleMeeting { calendarUrl ->
                    calendarHook.insertAndOpenCalendar(form, calendarUrl)
                }
            },
            enabled = form.canJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("add_to_calendar_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = form.calendarActionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!form.joined) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (!form.canJoin) {
            Text(
                text = "Masukkan tautan rapat untuk menyimpan ke kalender.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_validation_message"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        form.statusBannerMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_saved_banner"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Real-Time Calendar Verification & Diagnostic System Card
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diagnostik Kalender Native",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { calendarHook.verifyEventsInNativeCalendar(form) }
                ) {
                    Text("Cek Status Real-time", style = MaterialTheme.typography.labelMedium)
                }
            }

            val diag = calendarHook.diagnosticResult
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_diagnostic_card"),
                shape = RoundedCornerShape(18.dp),
                color = when {
                    diag == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    diag.isOk -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        diag == null -> MaterialTheme.colorScheme.outlineVariant
                        diag.isOk -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (diag?.isOk == true) "✓ Jadwal Terbaca di Kalender" else "⚠️ Diagnostik Status Kalender",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (diag?.isOk == true) MaterialTheme.colorScheme.onPrimaryContainer else if (diag != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                        diag?.errorCode?.let { errCode ->
                            BadgePill(
                                text = errCode.code,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                    }

                    Text(
                        text = diag?.message ?: "Tekan 'Cek Status Real-time' untuk memverifikasi apakah jadwal sudah terbaca di Kalender Native ${calendarHook.oemBrand}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diag?.isOk == false) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (diag != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Log Debug Diagnostik:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = diag.debugLog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { calendarHook.openNativeCalendarApp() }
                        ) {
                            Text("Buka App Kalender", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.meetingDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateChange(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih Tanggal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Start Time Picker Dialog
    if (showStartTimePicker) {
        val startTimePickerState = rememberTimePickerState(
            initialHour = form.startHour,
            initialMinute = form.startMinute,
            is24Hour = false
        )
        TimePickerDialogWrapper(
            title = "Pilih Jam Mulai",
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                onStartTimeChange(startTimePickerState.hour, startTimePickerState.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimePickerState)
        }
    }

    // Material 3 End Time Picker Dialog
    if (showEndTimePicker) {
        val endTimePickerState = rememberTimePickerState(
            initialHour = form.endHour,
            initialMinute = form.endMinute,
            is24Hour = false
        )
        TimePickerDialogWrapper(
            title = "Pilih Jam Selesai",
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                onEndTimeChange(endTimePickerState.hour, endTimePickerState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }
}

@Composable
fun TimePickerDialogWrapper(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Simpan Jam")
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorField(
    label: String,
    selectedText: String,
    leadingIcon: ImageVector?,
    testTag: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        SelectorDropdownBox(
            selectedText = selectedText,
            leadingIcon = leadingIcon,
            testTag = testTag,
            options = options,
            optionLabel = optionLabel,
            onSelect = onSelect
        )
    }
}

@Composable
private fun <T> SelectorDropdownBox(
    selectedText: String,
    leadingIcon: ImageVector?,
    testTag: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(item),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun readClipboardText(context: Context): String? {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        item?.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
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

private fun insertToOemLocalCalendar(
    context: Context,
    form: ScheduleFormState
): Boolean {
    return try {
        val hasWritePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasWritePerm) return false

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

        var targetCalId: Long? = null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryCol = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(idCol)
                    val isPrimary = if (primaryCol >= 0) cursor.getInt(primaryCol) == 1 else false
                    if (isPrimary) {
                        targetCalId = calId
                        break
                    } else if (targetCalId == null) {
                        targetCalId = calId
                    }
                }
            }
        } catch (_: Exception) {}

        val finalCalId = targetCalId ?: 1L

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
                try {
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                } catch (_: Exception) {}
            }
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

private fun launchCalendarOrUrl(
    context: Context,
    form: ScheduleFormState,
    calendarUrl: String
) {
    try {
        val oemBrand = getOemBrandName()
        if (form.calendarSync == CalendarSync.LOCAL_CALENDAR || form.operatingSystem == OperatingSystem.ANDROID) {
            val inserted = insertToOemLocalCalendar(context, form)

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

            // Open Xiaomi HyperOS / OEM Native Calendar Intent
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

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_EDIT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                    putExtra(CalendarContract.Events.EVENT_LOCATION, form.meetingLink.trim())
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(fallbackIntent) } catch (_: Exception) {}
            }

            Toast.makeText(
                context,
                if (inserted) "Jadwal tersimpan di DB & Kalender Native $oemBrand!" else "Membuka Kalender Native $oemBrand...",
                Toast.LENGTH_LONG
            ).show()
        } else if (calendarUrl.startsWith("http")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Membuka kalender: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
