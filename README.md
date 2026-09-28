# Kite — Video Meeting Scheduler (Android)

Kite ("Meetings, made intentional") is an Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room** for scheduling video meetings and managing calendar flows across platforms.

## Features
- **Home (`Home`)**: Daily meeting overview showing active meeting counts, upcoming meeting cards, Google Calendar sync status, and quick access to schedule creation.
- **Schedule (`Jadwal`)**: Configure meeting platform (`Zoom`, `Google Meet`, `Microsoft Teams`), operating system flow (`Android`, `iOS`, `Windows`, `macOS`), calendar sync target (`Google Calendar` or `Local Calendar`), meeting link with host validation and clipboard paste, passcode, date picker, recurrence (`Does not repeat`, `Every day`, `Every week`, `Every month`), and reminder timing. Generates Google Calendar template URLs, Outlook deeplinks, ICS definitions, or launches Android's native `CalendarContract.Events` intent while persisting scheduled meetings locally in Room.
- **Alerts (`Pemberitahuan`)**: Displays active meeting reminders and calendar connection notifications stored in Room.
- **Account & Meetflow Auth (`Akun`)**: Manage Google Calendar connection state, view the Meetflow account profile, sign in with Google, or submit an account access approval request.
