package com.example

import com.example.model.CalendarSync
import com.example.model.MeetingLinkValidator
import com.example.model.MeetingPlatform
import com.example.model.OperatingSystem
import com.example.model.ReminderOption
import com.example.model.RepeatOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KiteMeetingTest {

    @Test
    fun `validates meeting links by platform host and subdomains`() {
        // Zoom formats
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://zoom.us/j/123456789", MeetingPlatform.ZOOM))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://us02web.zoom.us/j/123456789?pwd=test", MeetingPlatform.ZOOM))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("zoom.us/j/123456789", MeetingPlatform.ZOOM))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://company.zoom.com/j/987654321", MeetingPlatform.ZOOM))

        // Google Meet formats
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://meet.google.com/abc-defg-hij", MeetingPlatform.GOOGLE_MEET))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("meet.google.com/abc-defg-hij", MeetingPlatform.GOOGLE_MEET))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://g.co/meet/standup", MeetingPlatform.GOOGLE_MEET))

        // Teams formats
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://teams.microsoft.com/l/meetup-join/19", MeetingPlatform.MICROSOFT_TEAMS))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("https://teams.live.com/meet/987654321", MeetingPlatform.MICROSOFT_TEAMS))
        assertTrue(MeetingLinkValidator.isValidMeetingLink("teams.microsoft.com/l/meetup-join/19", MeetingPlatform.MICROSOFT_TEAMS))

        // Mismatched platform validation
        assertFalse(MeetingLinkValidator.isValidMeetingLink("https://meet.google.com/abc-defg-hij", MeetingPlatform.ZOOM))
        assertFalse(MeetingLinkValidator.isValidMeetingLink("https://zoom.us/j/123456789", MeetingPlatform.GOOGLE_MEET))
    }

    @Test
    fun `auto detects meeting platforms accurately`() {
        assertEquals(MeetingPlatform.ZOOM, MeetingLinkValidator.detectPlatform("https://us04web.zoom.us/j/123"))
        assertEquals(MeetingPlatform.GOOGLE_MEET, MeetingLinkValidator.detectPlatform("meet.google.com/xyz-uvw-rst"))
        assertEquals(MeetingPlatform.MICROSOFT_TEAMS, MeetingLinkValidator.detectPlatform("https://teams.microsoft.com/l/meetup-join/123"))
    }

    @Test
    fun `calculates duration and formats time ranges`() {
        val duration = MeetingLinkValidator.calculateDurationMinutes(9, 30, 10, 45)
        assertEquals(75, duration)
        assertEquals("1 jam 15 mnt", MeetingLinkValidator.formatDurationText(75))
        assertEquals("45 menit", MeetingLinkValidator.formatDurationText(45))
        assertEquals("09:30 AM", MeetingLinkValidator.formatTime12(9, 30))
        assertEquals("02:15 PM", MeetingLinkValidator.formatTime12(14, 15))
    }

    @Test
    fun `builds Google Calendar Outlook and ICS URLs with start and end times`() {
        val googleUrl = MeetingLinkValidator.buildCalendarUrl(
            os = OperatingSystem.ANDROID,
            calendarSync = CalendarSync.GOOGLE_CALENDAR,
            meetingPlatform = MeetingPlatform.ZOOM,
            meetingTitle = "Design sync",
            meetingLink = "us02web.zoom.us/j/123456789",
            passcode = "246810",
            meetingDateMillis = 1790591400000L,
            startHour = 10,
            startMinute = 30,
            endHour = 11,
            endMinute = 15,
            repeat = RepeatOption.EVERY_WEEK,
            reminder = ReminderOption.TEN_MINUTES
        )
        assertTrue(googleUrl.startsWith("https://calendar.google.com/calendar/render?"))
        assertTrue(googleUrl.contains("recur=RRULE%3AFREQ%3DWEEKLY"))

        val outlookUrl = MeetingLinkValidator.buildCalendarUrl(
            os = OperatingSystem.WINDOWS,
            calendarSync = CalendarSync.LOCAL_CALENDAR,
            meetingPlatform = MeetingPlatform.MICROSOFT_TEAMS,
            meetingTitle = "Design sync",
            meetingLink = "https://teams.microsoft.com/l/meetup-join/19",
            passcode = "999111",
            meetingDateMillis = 1790591400000L,
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 0,
            repeat = RepeatOption.DOES_NOT_REPEAT,
            reminder = ReminderOption.THIRTY_MINUTES
        )
        assertTrue(outlookUrl.startsWith("https://outlook.office.com/calendar/0/deeplink/compose?"))

        val icsUrl = MeetingLinkValidator.buildCalendarUrl(
            os = OperatingSystem.MACOS,
            calendarSync = CalendarSync.LOCAL_CALENDAR,
            meetingPlatform = MeetingPlatform.ZOOM,
            meetingTitle = "Design sync",
            meetingLink = "https://zoom.us/j/123456789",
            passcode = "246810",
            meetingDateMillis = 1790591400000L,
            startHour = 9,
            startMinute = 0,
            endHour = 10,
            endMinute = 0,
            repeat = RepeatOption.EVERY_DAY,
            reminder = ReminderOption.TEN_MINUTES
        )
        assertTrue(icsUrl.startsWith("data:text/calendar;charset=utf-8,"))
    }

    @Test
    fun `parses theme mode properly`() {
        assertEquals(
            com.example.model.ThemeMode.DARK,
            com.example.model.ThemeMode.fromName("DARK")
        )
        assertEquals(
            com.example.model.ThemeMode.LIGHT,
            com.example.model.ThemeMode.fromName("Mode Terang")
        )
        assertEquals(
            com.example.model.ThemeMode.SYSTEM,
            com.example.model.ThemeMode.fromName(null)
        )
    }
}
