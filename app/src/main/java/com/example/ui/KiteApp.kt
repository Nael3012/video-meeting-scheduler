package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AccountSubScreen
import com.example.model.AppTab
import com.example.ui.screens.AccountAndAuthScreens
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BadgePill
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.viewmodel.KiteViewModel

@Composable
fun KiteApp(
    viewModel: KiteViewModel
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val accountSubScreen by viewModel.accountSubScreen.collectAsStateWithLifecycle()
    val scheduleForm by viewModel.scheduleForm.collectAsStateWithLifecycle()
    val meetings by viewModel.meetings.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val signUpName by viewModel.signUpName.collectAsStateWithLifecycle()
    val signUpEmail by viewModel.signUpEmail.collectAsStateWithLifecycle()
    val signUpPassword by viewModel.signUpPassword.collectAsStateWithLifecycle()
    val authMessage by viewModel.authMessage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.currentThemeMode.collectAsStateWithLifecycle()

    BackHandler(enabled = activeTab != AppTab.HOME || accountSubScreen != AccountSubScreen.OVERVIEW) {
        viewModel.handleBackNavigation()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            KiteBottomBar(
                activeTab = activeTab,
                onSelectTab = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Material You Expressive Brand Header
                if (activeTab != AppTab.ACCOUNT || accountSubScreen == AccountSubScreen.OVERVIEW) {
                    KiteHeader(
                        platformName = scheduleForm.meetingPlatform.displayName,
                        timeRange = scheduleForm.formattedTimeRange
                    )
                }

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.HOME -> {
                            HomeScreen(
                                meetings = meetings,
                                userAccount = userAccount,
                                onCreateNewSchedule = { viewModel.selectTab(AppTab.SCHEDULE) },
                                onSelectMeeting = { viewModel.loadMeetingIntoSchedule(it) },
                                onDeleteMeeting = { viewModel.deleteMeeting(it) }
                            )
                        }

                        AppTab.SCHEDULE -> {
                            ScheduleScreen(
                                form = scheduleForm,
                                onCalendarSyncChange = { viewModel.updateCalendarSync(it) },
                                onPlatformChange = { viewModel.updateMeetingPlatform(it) },
                                onTitleChange = { viewModel.updateMeetingTitle(it) },
                                onLinkChange = { viewModel.updateMeetingLink(it) },
                                onPasscodeChange = { viewModel.updatePasscode(it) },
                                onDateChange = { viewModel.updateMeetingDate(it) },
                                onStartTimeChange = { h, m -> viewModel.updateStartTime(h, m) },
                                onEndTimeChange = { h, m -> viewModel.updateEndTime(h, m) },
                                onSetPresetDuration = { viewModel.setPresetDuration(it) },
                                onRepeatChange = { viewModel.updateRepeat(it) },
                                onReminderChange = { viewModel.updateReminder(it) },
                                onFillSample = { viewModel.fillSampleCredentials() },
                                onScheduleMeeting = { callback -> viewModel.markScheduledAndSave(callback) }
                            )
                        }

                        AppTab.ALERTS -> {
                            AlertsScreen(
                                reminders = reminders,
                                onDismissReminder = { viewModel.dismissReminder(it) }
                            )
                        }

                        AppTab.ACCOUNT -> {
                            AccountAndAuthScreens(
                                subScreen = accountSubScreen,
                                userAccount = userAccount,
                                currentThemeMode = currentThemeMode,
                                signUpName = signUpName,
                                signUpEmail = signUpEmail,
                                signUpPassword = signUpPassword,
                                authMessage = authMessage,
                                onNavigateSubScreen = { viewModel.navigateAccountSubScreen(it) },
                                onSignUpNameChange = { viewModel.updateSignUpName(it) },
                                onSignUpEmailChange = { viewModel.updateSignUpEmail(it) },
                                onSignUpPasswordChange = { viewModel.updateSignUpPassword(it) },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSignInWithGoogle = { name, email, photoUrl, idToken ->
                                    viewModel.signInWithGoogle(name, email, photoUrl, idToken)
                                },
                                onRegisterUser = { name, email ->
                                    viewModel.registerUser(name, email)
                                },
                                onSignOut = { viewModel.signOut() },
                                onSubmitAccessRequest = { viewModel.submitAccessRequest() },
                                onToggleCalendarSync = { viewModel.toggleGoogleCalendarSync() },
                                onGoHome = { viewModel.selectTab(AppTab.HOME) }
                            )
                        }
                    }
                }

                if (activeTab != AppTab.ACCOUNT || accountSubScreen == AccountSubScreen.OVERVIEW) {
                    Text(
                        text = stringResource(R.string.schedule_footer_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun KiteHeader(
    platformName: String,
    timeRange: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .testTag("kite_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Rapat $platformName · $timeRange",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BadgePill(
            text = stringResource(R.string.badge_today),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun KiteBottomBar(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .testTag("bottom_navigation_bar"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val interactionSource = remember { MutableInteractionSource() }

                    val animatedPillColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(durationMillis = 250),
                        label = "pill_color"
                    )
                    val animatedIconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 250),
                        label = "icon_color"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 250),
                        label = "text_color"
                    )
                    val animatedPillWidth by animateDpAsState(
                        targetValue = if (isSelected) 52.dp else 40.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_width"
                    )
                    val animatedIconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "icon_scale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelectTab(tab) }
                            .padding(vertical = 4.dp)
                            .testTag("nav_tab_${tab.id}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = animatedPillWidth, height = 30.dp)
                                .clip(CircleShape)
                                .background(animatedPillColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tabIcon(tab, isSelected),
                                contentDescription = tab.label,
                                tint = animatedIconColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer(
                                        scaleX = animatedIconScale,
                                        scaleY = animatedIconScale
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: AppTab, isSelected: Boolean): ImageVector = when (tab) {
    AppTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
    AppTab.SCHEDULE -> if (isSelected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
    AppTab.ALERTS -> if (isSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications
    AppTab.ACCOUNT -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
}
