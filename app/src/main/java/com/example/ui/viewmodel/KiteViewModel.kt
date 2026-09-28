package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.KiteRepository
import com.example.data.MeetingEntity
import com.example.data.ReminderEntity
import com.example.data.UserAccountEntity
import com.example.model.AccountSubScreen
import com.example.model.AppTab
import com.example.model.CalendarSync
import com.example.model.MeetingLinkValidator
import com.example.model.MeetingPlatform
import com.example.model.OperatingSystem
import com.example.model.ReminderOption
import com.example.model.RepeatOption
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ScheduleFormState(
    val operatingSystem: OperatingSystem = OperatingSystem.ANDROID,
    val calendarSync: CalendarSync = CalendarSync.GOOGLE_CALENDAR,
    val meetingPlatform: MeetingPlatform = MeetingPlatform.ZOOM,
    val isPlatformManuallySelected: Boolean = false,
    val meetingTitle: String = "",
    val meetingLink: String = "",
    val passcode: String = "",
    val meetingDateMillis: Long = defaultMeetingDateMillis(),
    val startHour: Int = 10,
    val startMinute: Int = 30,
    val endHour: Int = 11,
    val endMinute: Int = 15,
    val repeat: RepeatOption = RepeatOption.DOES_NOT_REPEAT,
    val reminder: ReminderOption = ReminderOption.TEN_MINUTES,
    val joined: Boolean = false,
    val statusBannerMessage: String? = null
) {
    val isValidMeetingLink: Boolean
        get() = MeetingLinkValidator.isValidMeetingLink(meetingLink, meetingPlatform)

    val canJoin: Boolean
        get() = meetingLink.trim().isNotEmpty()

    val formattedStartTime: String
        get() = MeetingLinkValidator.formatTime12(startHour, startMinute)

    val formattedEndTime: String
        get() = MeetingLinkValidator.formatTime12(endHour, endMinute)

    val formattedTimeRange: String
        get() = "$formattedStartTime - $formattedEndTime"

    val durationMinutes: Int
        get() = MeetingLinkValidator.calculateDurationMinutes(startHour, startMinute, endHour, endMinute)

    val formattedDuration: String
        get() = MeetingLinkValidator.formatDurationText(durationMinutes)

    val calendarTarget: String
        get() = if (calendarSync == CalendarSync.GOOGLE_CALENDAR) {
            "Google Calendar"
        } else {
            operatingSystem.nativeCalendar
        }

    val calendarActionLabel: String
        get() = if (joined) "Jadwal Siap di Kalender" else "Tambahkan ke $calendarTarget"

    fun buildCalendarUrl(): String = MeetingLinkValidator.buildCalendarUrl(
        os = operatingSystem,
        calendarSync = calendarSync,
        meetingPlatform = meetingPlatform,
        meetingTitle = meetingTitle,
        meetingLink = meetingLink,
        passcode = passcode,
        meetingDateMillis = meetingDateMillis,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        repeat = repeat,
        reminder = reminder
    )
}

private fun defaultMeetingDateMillis(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 0)
    cal.set(Calendar.HOUR_OF_DAY, 10)
    cal.set(Calendar.MINUTE, 30)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

class KiteViewModel(private val repository: KiteRepository) : ViewModel() {

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _accountSubScreen = MutableStateFlow(AccountSubScreen.OVERVIEW)
    val accountSubScreen: StateFlow<AccountSubScreen> = _accountSubScreen.asStateFlow()

    private val _scheduleForm = MutableStateFlow(ScheduleFormState())
    val scheduleForm: StateFlow<ScheduleFormState> = _scheduleForm.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _signUpName = MutableStateFlow("")
    val signUpName: StateFlow<String> = _signUpName.asStateFlow()

    private val _signUpEmail = MutableStateFlow("")
    val signUpEmail: StateFlow<String> = _signUpEmail.asStateFlow()

    private val _signUpPassword = MutableStateFlow("")
    val signUpPassword: StateFlow<String> = _signUpPassword.asStateFlow()

    val meetings: StateFlow<List<MeetingEntity>> = repository.allMeetings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAccount: StateFlow<UserAccountEntity?> = repository.userAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentThemeMode: StateFlow<ThemeMode> = repository.userAccount
        .map { ThemeMode.fromName(it?.themeMode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
        if (tab != AppTab.ACCOUNT) {
            _accountSubScreen.value = AccountSubScreen.OVERVIEW
        }
    }

    fun navigateAccountSubScreen(screen: AccountSubScreen) {
        _authMessage.value = null
        _accountSubScreen.value = screen
    }

    fun handleBackNavigation(): Boolean {
        if (_activeTab.value == AppTab.ACCOUNT && _accountSubScreen.value != AccountSubScreen.OVERVIEW) {
            _accountSubScreen.value = when (_accountSubScreen.value) {
                AccountSubScreen.SIGN_UP -> AccountSubScreen.SIGN_IN
                AccountSubScreen.SIGN_IN -> AccountSubScreen.OVERVIEW
                AccountSubScreen.ACCOUNT_DETAIL -> AccountSubScreen.OVERVIEW
                AccountSubScreen.OVERVIEW -> AccountSubScreen.OVERVIEW
            }
            _authMessage.value = null
            return true
        }
        if (_activeTab.value != AppTab.HOME) {
            _activeTab.value = AppTab.HOME
            return true
        }
        return false
    }

    fun updateCalendarSync(sync: CalendarSync) {
        _scheduleForm.update { it.copy(calendarSync = sync, statusBannerMessage = null) }
    }

    fun updateMeetingPlatform(platform: MeetingPlatform) {
        _scheduleForm.update {
            it.copy(
                meetingPlatform = platform,
                isPlatformManuallySelected = true,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun updateMeetingTitle(title: String) {
        _scheduleForm.update { it.copy(meetingTitle = title, joined = false, statusBannerMessage = null) }
    }

    fun updateMeetingLink(link: String) {
        val detected = MeetingLinkValidator.detectPlatform(link)
        _scheduleForm.update { current ->
            val newPlatform = if (!current.isPlatformManuallySelected && detected != null) {
                detected
            } else {
                current.meetingPlatform
            }
            current.copy(
                meetingLink = link,
                meetingPlatform = newPlatform,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun updatePasscode(passcode: String) {
        _scheduleForm.update { it.copy(passcode = passcode, joined = false, statusBannerMessage = null) }
    }

    fun updateMeetingDate(dateMillis: Long) {
        _scheduleForm.update { it.copy(meetingDateMillis = dateMillis, joined = false, statusBannerMessage = null) }
    }

    fun updateStartTime(hour: Int, minute: Int) {
        _scheduleForm.update { state ->
            val curDuration = state.durationMinutes
            val newStartTotal = hour * 60 + minute
            val newEndTotal = (newStartTotal + curDuration) % (24 * 60)
            val newEndHour = newEndTotal / 60
            val newEndMinute = newEndTotal % 60
            state.copy(
                startHour = hour,
                startMinute = minute,
                endHour = newEndHour,
                endMinute = newEndMinute,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun updateEndTime(hour: Int, minute: Int) {
        _scheduleForm.update { state ->
            state.copy(
                endHour = hour,
                endMinute = minute,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun setPresetDuration(durationMin: Int) {
        _scheduleForm.update { state ->
            val startTotal = state.startHour * 60 + state.startMinute
            val endTotal = (startTotal + durationMin) % (24 * 60)
            val newEndHour = endTotal / 60
            val newEndMinute = endTotal % 60
            state.copy(
                endHour = newEndHour,
                endMinute = newEndMinute,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun updateRepeat(repeat: RepeatOption) {
        _scheduleForm.update { it.copy(repeat = repeat, joined = false, statusBannerMessage = null) }
    }

    fun updateReminder(reminder: ReminderOption) {
        _scheduleForm.update { it.copy(reminder = reminder, joined = false, statusBannerMessage = null) }
    }

    fun fillSampleCredentials() {
        val currentPlatform = _scheduleForm.value.meetingPlatform
        _scheduleForm.update {
            it.copy(
                meetingTitle = if (it.meetingTitle.isBlank()) "Diskusi Tim ${currentPlatform.displayName}" else it.meetingTitle,
                meetingLink = currentPlatform.sampleLink,
                passcode = if (it.passcode.isBlank()) "246810" else it.passcode,
                joined = false,
                statusBannerMessage = null
            )
        }
    }

    fun resetScheduleForm() {
        _scheduleForm.value = ScheduleFormState()
    }

    fun markScheduledAndSave(onSaved: (String) -> Unit = {}) {
        val form = _scheduleForm.value
        if (!form.canJoin) return
        val url = form.buildCalendarUrl()
        _scheduleForm.update {
            it.copy(
                joined = true,
                statusBannerMessage = "Tersimpan ke ${form.calendarTarget} & Cloud Firestore (${form.formattedTimeRange})"
            )
        }
        viewModelScope.launch {
            repository.scheduleMeeting(
                title = form.meetingTitle,
                link = form.meetingLink,
                passcode = form.passcode,
                platform = form.meetingPlatform,
                os = form.operatingSystem,
                calendarSync = form.calendarSync,
                dateMillis = form.meetingDateMillis,
                startHour = form.startHour,
                startMinute = form.startMinute,
                endHour = form.endHour,
                endMinute = form.endMinute,
                repeat = form.repeat,
                reminder = form.reminder
            )
            onSaved(url)
        }
    }

    fun loadMeetingIntoSchedule(meeting: MeetingEntity) {
        _scheduleForm.update {
            ScheduleFormState(
                operatingSystem = OperatingSystem.fromName(meeting.operatingSystem),
                calendarSync = CalendarSync.fromName(meeting.calendarSync),
                meetingPlatform = MeetingPlatform.fromName(meeting.meetingPlatform),
                meetingTitle = meeting.title,
                meetingLink = meeting.link,
                passcode = meeting.passcode,
                meetingDateMillis = meeting.meetingDateMillis,
                startHour = meeting.startHour,
                startMinute = meeting.startMinute,
                endHour = meeting.endHour,
                endMinute = meeting.endMinute,
                repeat = RepeatOption.fromName(meeting.repeatOption),
                reminder = ReminderOption.fromName(meeting.reminderOption),
                joined = false
            )
        }
        _activeTab.value = AppTab.SCHEDULE
    }

    fun deleteMeeting(id: Long) {
        viewModelScope.launch {
            repository.deleteMeeting(id)
        }
    }

    fun dismissReminder(id: Long) {
        viewModelScope.launch {
            repository.dismissReminder(id)
        }
    }

    fun updateSignUpName(name: String) {
        _signUpName.value = name
    }

    fun updateSignUpEmail(email: String) {
        _signUpEmail.value = email
    }

    fun updateSignUpPassword(password: String) {
        _signUpPassword.value = password
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun signInWithGoogle(
        name: String = "Pengguna Google",
        email: String = "user@gmail.com",
        photoUrl: String? = null,
        idToken: String? = null
    ) {
        viewModelScope.launch {
            repository.signInWithGoogle(name, email, photoUrl, idToken)
            _authMessage.value = "Berhasil masuk & terverifikasi dengan Google OAuth!"
            _accountSubScreen.value = AccountSubScreen.ACCOUNT_DETAIL
        }
    }

    fun registerUser(
        name: String,
        email: String
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        if (cleanName.isEmpty() || cleanEmail.isEmpty()) {
            _authMessage.value = "Nama dan email wajib diisi."
            return
        }
        viewModelScope.launch {
            repository.registerUser(cleanName, cleanEmail)
            _authMessage.value = "Akun berhasil didaftarkan dan terhubung ke Firebase!"
            _accountSubScreen.value = AccountSubScreen.ACCOUNT_DETAIL
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _authMessage.value = null
            _accountSubScreen.value = AccountSubScreen.SIGN_IN
        }
    }

    fun submitAccessRequest() {
        val name = _signUpName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.submitAccessRequest(name)
            _authMessage.value =
                "Permintaan akses Anda telah dicatat. Administrator Meetflow akan memproses persetujuan sebelum akun diaktifkan."
        }
    }

    fun toggleGoogleCalendarSync() {
        viewModelScope.launch {
            repository.toggleGoogleCalendarSync()
        }
    }

    class Factory(private val repository: KiteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KiteViewModel(repository) as T
        }
    }
}
