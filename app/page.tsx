'use client'

import { motion } from 'framer-motion'
import { format } from 'date-fns'
import { Bell, CalendarDays, Check, ChevronDown, ClipboardPaste, Home, Laptop, Link2, Monitor, Pencil, Settings, Smartphone, Tablet, UserRound, Video } from 'lucide-react'
import { useEffect, useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Calendar } from '@/components/ui/calendar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type Platform = 'Android' | 'iOS' | 'Windows' | 'macOS'
type CalendarSync = 'Google Calendar' | 'Local Calendar'
type MeetingPlatform = 'Zoom' | 'Google Meet' | 'Microsoft Teams'
type RepeatOption = 'Does not repeat' | 'Every day' | 'Every week' | 'Every month'
type ReminderOption = 'At time of meeting' | '10 minutes before' | '30 minutes before' | '1 hour before'
type AppTab = 'home' | 'schedule' | 'alerts' | 'account'

const platforms: { name: Platform; label: string; icon: typeof Smartphone }[] = [
  { name: 'Android', label: 'Android phone', icon: Smartphone },
  { name: 'iOS', label: 'iPhone / iPad', icon: Tablet },
  { name: 'Windows', label: 'Windows PC', icon: Monitor },
  { name: 'macOS', label: 'Mac computer', icon: Laptop },
]

const calendarOptions: { name: CalendarSync; label: string }[] = [
  { name: 'Google Calendar', label: 'Sync across devices' },
  { name: 'Local Calendar', label: 'Use the built-in calendar' },
]

const meetingPlatforms: MeetingPlatform[] = ['Zoom', 'Google Meet', 'Microsoft Teams']

const platformFlows: Record<Platform, { calendar: string; nativeCalendar: string; linkHint: string; openLabel: string }> = {
  Android: {
    calendar: 'Google Calendar or Android Calendar',
    nativeCalendar: 'Android Calendar',
    linkHint: 'https://zoom.us/j/123456789',
    openLabel: 'Open in Android',
  },
  iOS: {
    calendar: 'Google Calendar or Apple Calendar',
    nativeCalendar: 'Apple Calendar',
    linkHint: 'https://zoom.us/j/123456789',
    openLabel: 'Open on iPhone / iPad',
  },
  Windows: {
    calendar: 'Google Calendar or Outlook Calendar',
    nativeCalendar: 'Outlook Calendar',
    linkHint: 'https://zoom.us/j/123456789',
    openLabel: 'Open on Windows',
  },
  macOS: {
    calendar: 'Google Calendar or Apple Calendar',
    nativeCalendar: 'Apple Calendar',
    linkHint: 'https://zoom.us/j/123456789',
    openLabel: 'Open on Mac',
  },
}

export default function Page() {
  const [platform, setPlatform] = useState<Platform>('Android')
  const [calendarSync, setCalendarSync] = useState<CalendarSync>('Google Calendar')
  const [isHydrated, setIsHydrated] = useState(false)
  const [meetingPlatform, setMeetingPlatform] = useState<MeetingPlatform>('Zoom')
  const [meetingLink, setMeetingLink] = useState('')
  const [passcode, setPasscode] = useState('')
  const [meetingDate, setMeetingDate] = useState<Date>(new Date(2026, 8, 28))
  const [repeat, setRepeat] = useState<RepeatOption>('Does not repeat')
  const [reminder, setReminder] = useState<ReminderOption>('10 minutes before')
  const [joined, setJoined] = useState(false)
  const [activeTab, setActiveTab] = useState<AppTab>('home')

  useEffect(() => {
    setIsHydrated(true)
  }, [])

  const selected = platforms.find((item) => item.name === platform) ?? platforms[0]
  const selectedFlow = platformFlows[platform]
  const PlatformIcon = selected.icon
  const meetingHosts: Record<MeetingPlatform, string[]> = {
    Zoom: ['zoom.us', 'www.zoom.us'],
    'Google Meet': ['meet.google.com'],
    'Microsoft Teams': ['teams.microsoft.com', 'www.teams.microsoft.com'],
  }
  const isValidMeetingLink = (() => {
    try {
      const url = new URL(meetingLink.trim())
      return url.protocol === 'https:' && meetingHosts[meetingPlatform].includes(url.hostname)
    } catch {
      return false
    }
  })()
  const canJoin = isValidMeetingLink && passcode.trim().length > 0
  const calendarTarget = calendarSync === 'Google Calendar'
    ? 'Google Calendar'
    : selectedFlow.nativeCalendar
  const calendarActionLabel = !isHydrated ? 'Add to calendar' : joined ? 'Calendar event ready' : `Add to ${calendarTarget}`

  function buildCalendarUrl() {
    const start = new Date(meetingDate)
    start.setHours(10, 30, 0, 0)
    const end = new Date(start)
    end.setMinutes(end.getMinutes() + 45)
    const formatCalendarDate = (date: Date) => date.toISOString().replace(/[-:]/g, '').replace(/\\.\\d{3}Z$/, 'Z')
    const startDate = formatCalendarDate(start)
    const endDate = formatCalendarDate(end)
    const recurrence = repeat === 'Does not repeat' ? '' : `RRULE:FREQ=${repeat === 'Every day' ? 'DAILY' : repeat === 'Every week' ? 'WEEKLY' : 'MONTHLY'}`
    const details = `${meetingPlatform} meeting\\nJoin link: ${meetingLink.trim()}\\nPasscode: ${passcode.trim()}\\nReminder: ${reminder}`

    if (calendarSync === 'Google Calendar') {
      const params = new URLSearchParams({ action: 'TEMPLATE', text: `${meetingPlatform} meeting`, dates: `${startDate}/${endDate}`, details, location: meetingLink.trim() })
      if (recurrence) params.set('recur', recurrence)
      return `https://calendar.google.com/calendar/render?${params.toString()}`
    }

    if (platform === 'Windows') {
      const params = new URLSearchParams({ subject: `${meetingPlatform} meeting`, startdt: start.toISOString(), enddt: end.toISOString(), body: details, location: meetingLink.trim() })
      return `https://outlook.office.com/calendar/0/deeplink/compose?${params.toString()}`
    }

    const ics = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'BEGIN:VEVENT',
      `DTSTART:${startDate}`,
      `DTEND:${endDate}`,
      `SUMMARY:${meetingPlatform} meeting`,
      `DESCRIPTION:${details.replace(/\\n/g, '\\\\n')}`,
      `LOCATION:${meetingLink.trim()}`,
      recurrence ? recurrence : '',
      'BEGIN:VALARM',
      `TRIGGER:-PT${reminder === '10 minutes before' ? '10M' : reminder === '30 minutes before' ? '30M' : reminder === '1 hour before' ? '60M' : '0M'}`,
      'ACTION:DISPLAY',
      'DESCRIPTION:Meeting reminder',
      'END:VALARM',
      'END:VEVENT',
      'END:VCALENDAR',
    ].filter(Boolean).join('\\r\\n')
    return `data:text/calendar;charset=utf-8,${encodeURIComponent(ics)}`
  }

  async function pasteInto(setValue: (value: string) => void, resetJoined = true) {
    try {
      const value = await navigator.clipboard.readText()
      if (value) {
        setValue(value)
        if (resetJoined) setJoined(false)
      }
    } catch {
      // Clipboard access can be unavailable in insecure or restricted contexts.
    }
  }

  return (
    <main className="min-h-screen bg-background px-3 pb-[calc(5.5rem+env(safe-area-inset-bottom))] pt-4 text-foreground sm:px-6 sm:py-6">
      <section className="mx-auto flex w-full max-w-md min-w-0 flex-col gap-3">
        <header className="flex items-center justify-between px-1">
          <div className="flex items-center gap-2.5">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Video aria-hidden="true" className="size-4" />
            </div>
            <div>
              <p className="font-semibold tracking-tight">Kite</p>
              <p className="text-xs text-muted-foreground">Join your Zoom meeting</p>
            </div>
          </div>
          <Badge variant="secondary">Today</Badge>
        </header>

        {activeTab === 'home' && (
          <section aria-labelledby="overview-title" className="flex flex-col gap-3 border-y border-border py-4">
            <header className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[10px] font-medium uppercase tracking-[0.12em] text-muted-foreground">Overview</p>
                <h1 id="overview-title" className="text-base font-semibold tracking-tight">Your meeting day</h1>
              </div>
              <Badge variant="secondary">1 active</Badge>
            </header>
            <div className="flex items-center gap-3 rounded-xl bg-muted p-3">
              <CalendarDays aria-hidden="true" className="size-4 text-muted-foreground" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs font-medium">Design sync with the product team</p>
                <p className="text-[11px] text-muted-foreground">Today, 10:30 AM · Zoom</p>
              </div>
              <Badge variant="outline">24 min</Badge>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-3">
              <div className="flex items-center gap-3">
                <Link2 aria-hidden="true" className="size-4 text-muted-foreground" />
                <div>
                  <p className="text-xs font-medium">Google Calendar</p>
                  <p className="text-[11px] text-muted-foreground">Sync is active</p>
                </div>
              </div>
              <Check aria-hidden="true" className="size-4 text-muted-foreground" />
            </div>
            <Button className="min-h-10 w-full rounded-xl text-xs" onClick={() => setActiveTab('schedule')}>
              <CalendarDays aria-hidden="true" data-icon="inline-start" />
              Create new schedule
            </Button>
          </section>
        )}

        {activeTab === 'alerts' && (
          <section aria-labelledby="alerts-title" className="flex flex-col gap-3 border-y border-border py-4">
            <header>
              <p className="text-[10px] font-medium uppercase tracking-[0.12em] text-muted-foreground">Notifications</p>
              <h1 id="alerts-title" className="text-base font-semibold tracking-tight">Stay ahead of your meetings</h1>
            </header>
            <div className="flex items-start gap-3 rounded-xl bg-muted p-3">
              <Bell aria-hidden="true" className="mt-0.5 size-4 text-muted-foreground" />
              <div><p className="text-xs font-medium">Meeting starts in 10 minutes</p><p className="text-[11px] text-muted-foreground">Design sync with the product team</p></div>
            </div>
            <div className="flex items-start gap-3 rounded-xl border border-border p-3">
              <Link2 aria-hidden="true" className="mt-0.5 size-4 text-muted-foreground" />
              <div><p className="text-xs font-medium">Calendar account connected</p><p className="text-[11px] text-muted-foreground">Google Calendar is ready to sync.</p></div>
            </div>
          </section>
        )}

        {activeTab === 'account' && (
          <section aria-labelledby="account-title" className="flex flex-col gap-3 border-y border-border py-4">
            <header>
              <p className="text-[10px] font-medium uppercase tracking-[0.12em] text-muted-foreground">Account</p>
              <h1 id="account-title" className="text-base font-semibold tracking-tight">Calendar connections</h1>
            </header>
            <div className="flex items-center gap-3 rounded-xl border border-border p-3">
              <UserRound aria-hidden="true" className="size-4 text-muted-foreground" />
              <div className="min-w-0 flex-1"><p className="text-xs font-medium">Google Calendar</p><p className="text-[11px] text-muted-foreground">Connected and syncing</p></div>
              <Badge variant="secondary">Active</Badge>
            </div>
            <Button variant="outline" className="min-h-10 w-full rounded-xl text-xs"><Settings aria-hidden="true" data-icon="inline-start" />Manage account</Button>
          </section>
        )}

        {activeTab === 'schedule' && (
        <section aria-labelledby="meeting-title" className="flex min-w-0 flex-col gap-3 border-y border-border py-4">
          <header className="flex flex-col gap-2">
            <Badge className="w-fit" variant="outline">Next meeting</Badge>
            <h1 id="meeting-title" className="text-lg font-semibold tracking-tight text-balance">Design sync with the product team</h1>
            <p className="text-xs leading-4 text-muted-foreground">Choose your OS to open Zoom and your calendar with the right native flow.</p>
          </header>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between rounded-xl bg-muted p-3">
              <div className="flex items-center gap-3">
                <CalendarDays aria-hidden="true" className="size-4 text-muted-foreground" />
                <div>
                  <p className="text-xs font-medium">Today, 10:30 AM</p>
                  <p className="text-xs text-muted-foreground">45 minutes · {meetingPlatform}</p>
                </div>
              </div>
              <Badge variant="secondary">In 24 min</Badge>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="operating-system">Operating system</label>
              <Select value={platform} onValueChange={(value) => { setPlatform(value as Platform); setJoined(false) }}>
                <SelectTrigger className="min-h-11 w-full rounded-xl text-xs" id="operating-system">
                  <div className="flex items-center gap-2">
                    <PlatformIcon aria-hidden="true" className="size-4" />
                    <SelectValue placeholder="Choose your OS" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {platforms.map((item) => {
                      const Icon = item.icon
                      return (
                        <SelectItem key={item.name} value={item.name}>
                          <span className="flex items-center gap-2"><Icon aria-hidden="true" />{item.name} <span className="text-muted-foreground">· {item.label}</span></span>
                        </SelectItem>
                      )
                    })}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="calendar-sync">Calendar access</label>
              <p className="text-xs text-muted-foreground">{selectedFlow.calendar} · {selectedFlow.openLabel}</p>
              <Select value={calendarSync} onValueChange={(value) => setCalendarSync(value as CalendarSync)}>
                <SelectTrigger className="min-h-11 w-full rounded-xl text-xs" id="calendar-sync">
                  <div className="flex items-center gap-2">
                    <CalendarDays aria-hidden="true" className="size-4 text-muted-foreground" />
                    <SelectValue placeholder="Choose calendar" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {calendarOptions.map((item) => (
                      <SelectItem key={item.name} value={item.name}>
                        <span className="flex flex-col items-start gap-0.5">
                          <span>{item.name}</span>
                          <span className="text-xs text-muted-foreground">{item.name === 'Local Calendar' ? selectedFlow.nativeCalendar : item.label}</span>
                        </span>
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="meeting-platform">Meeting platform</label>
              <Select value={meetingPlatform} onValueChange={(value) => { setMeetingPlatform(value as MeetingPlatform); setJoined(false) }}>
                <SelectTrigger className="min-h-11 w-full rounded-xl text-xs" id="meeting-platform">
                  <div className="flex items-center gap-2">
                    <Video aria-hidden="true" className="size-4 text-muted-foreground" />
                    <SelectValue placeholder="Choose meeting platform" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {meetingPlatforms.map((item) => (
                      <SelectItem key={item} value={item}>{item}</SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="meeting-link">Meeting link</label>
              <div className="relative">
                <Input
                  id="meeting-link"
                  type="url"
                  placeholder={selectedFlow.linkHint}
                  value={meetingLink}
                  onChange={(event) => { setMeetingLink(event.target.value); setJoined(false) }}
                  className="min-h-11 w-full rounded-xl px-3 pr-12 text-xs"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label="Paste meeting link"
                  title="Paste meeting link"
                  className="absolute right-1 top-1/2 size-12 -translate-y-1/2 rounded-xl"
                  onClick={() => pasteInto(setMeetingLink)}
                >
                  <ClipboardPaste aria-hidden="true" className="size-4" />
                </Button>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="meeting-passcode">Passcode</label>
              <div className="relative">
                <Input
                  id="meeting-passcode"
                  type="text"
                  inputMode="text"
                  autoComplete="off"
                  placeholder="Enter meeting passcode"
                  value={passcode}
                  onChange={(event) => { setPasscode(event.target.value); setJoined(false) }}
                  className="min-h-11 w-full rounded-xl px-3 pr-12 text-xs"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label="Paste passcode"
                  title="Paste passcode"
                  className="absolute right-1 top-1/2 size-12 -translate-y-1/2 rounded-xl"
                  onClick={() => pasteInto(setPasscode)}
                >
                  <ClipboardPaste aria-hidden="true" className="size-4" />
                </Button>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium" htmlFor="meeting-date">Meeting date</label>
              <Popover>
                <PopoverTrigger
                  id="meeting-date"
                  className="flex min-h-11 w-full items-center justify-between rounded-xl border border-input bg-background px-3 text-left text-xs font-normal text-foreground shadow-xs transition-colors outline-none hover:bg-accent hover:text-accent-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                >
                  <span className="flex items-center gap-3">
                    <CalendarDays aria-hidden="true" className="size-4 text-muted-foreground" />
                    {format(meetingDate, 'EEE, MMM d, yyyy')}
                  </span>
                  <ChevronDown aria-hidden="true" />
                </PopoverTrigger>
                <PopoverContent align="start" className="w-auto p-0">
                  <Calendar
                    aria-label="Choose meeting date"
                    mode="single"
                    selected={meetingDate}
                    onSelect={(date) => {
                      if (date) {
                        setMeetingDate(date)
                        setJoined(false)
                      }
                    }}
                    disabled={{ before: new Date() }}
                  />
                </PopoverContent>
              </Popover>
              <p className="text-xs text-muted-foreground">Selected: {format(meetingDate, 'EEEE, MMM d, yyyy')}</p>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-2">
                <label className="text-xs font-medium" htmlFor="repeat-meeting">Repeat</label>
                <Select value={repeat} onValueChange={(value) => { setRepeat(value as RepeatOption); setJoined(false) }}>
                  <SelectTrigger className="min-h-12 w-full rounded-2xl" id="repeat-meeting">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      {(['Does not repeat', 'Every day', 'Every week', 'Every month'] as RepeatOption[]).map((option) => (
                        <SelectItem key={option} value={option}>{option}</SelectItem>
                      ))}
                    </SelectGroup>
                  </SelectContent>
                </Select>
              </div>

              <div className="flex flex-col gap-2">
                <label className="text-xs font-medium" htmlFor="meeting-reminder">Reminder</label>
                <Select value={reminder} onValueChange={(value) => { setReminder(value as ReminderOption); setJoined(false) }}>
                  <SelectTrigger className="min-h-12 w-full rounded-2xl" id="meeting-reminder">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      {(['At time of meeting', '10 minutes before', '30 minutes before', '1 hour before'] as ReminderOption[]).map((option) => (
                        <SelectItem key={option} value={option}>{option}</SelectItem>
                      ))}
                    </SelectGroup>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <motion.div key={`${platform}-${calendarSync}-${meetingDate.toISOString()}-${repeat}-${reminder}`} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} className="flex items-start gap-3 rounded-2xl border border-border bg-background p-4">
              <PlatformIcon aria-hidden="true" className="mt-0.5 text-muted-foreground" />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <p className="text-sm font-medium">{selected.label}</p>
                <p className="text-xs leading-5 text-muted-foreground">{calendarSync} will open {selectedFlow.nativeCalendar === 'Outlook Calendar' ? 'Outlook' : 'the native calendar'} with the {platform} {meetingPlatform} flow on {format(meetingDate, 'MMM d')}.</p>
                <p className="text-xs leading-5 text-muted-foreground">{repeat} · Reminder {reminder.toLowerCase()}.</p>
              </div>
              <Check aria-hidden="true" className="text-muted-foreground" />
            </motion.div>

            <Button
              className="min-h-12 w-full rounded-2xl"
              disabled={!canJoin}
              onClick={() => {
                window.open(buildCalendarUrl(), '_blank', 'noopener,noreferrer')
                setJoined(true)
              }}
            >
              {calendarActionLabel}
              {!joined && <CalendarDays aria-hidden="true" data-icon="inline-end" />}
            </Button>
            {!canJoin && (
              <p className="text-center text-xs text-muted-foreground">
                {meetingLink.trim().length > 0 && !isValidMeetingLink
                  ? `Use a valid ${meetingPlatform} link, then add the passcode.`
                  : 'Add the meeting link and passcode to continue.'}
              </p>
            )}
          </div>
        </section>
        )}

        <p className="px-2 text-center text-xs leading-5 text-muted-foreground">Your selection only controls how the meeting opens on this device.</p>
      </section>

      <nav aria-label="Primary navigation" className="fixed inset-x-3 bottom-3 z-10 mx-auto flex h-14 max-w-md items-stretch gap-1 rounded-2xl border border-border bg-card/95 p-1 shadow-lg backdrop-blur sm:inset-x-4 sm:h-16 sm:p-1.5">
        {([
          { id: 'home' as AppTab, label: 'Home', icon: Home },
          { id: 'schedule' as AppTab, label: 'Jadwal', icon: CalendarDays },
          { id: 'alerts' as AppTab, label: 'Pemberitahuan', icon: Bell },
          { id: 'account' as AppTab, label: 'Akun', icon: Settings },
        ]).map((item) => {
          const Icon = item.icon
          const isActive = activeTab === item.id
          return (
            <Button
              key={item.id}
              type="button"
              aria-current={isActive ? 'page' : undefined}
              variant="ghost"
              onClick={() => setActiveTab(item.id)}
              className={`min-h-0 min-w-0 flex-1 flex-col gap-1 rounded-xl px-1 py-1 text-[9px] leading-none tracking-tight sm:text-[10px] ${isActive ? 'bg-primary/10 font-semibold text-primary hover:bg-primary/10 hover:text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`}
            >
              <Icon aria-hidden="true" className="size-3.5" data-icon="inline-start" />
              <span className="max-w-full truncate">{item.label}</span>
            </Button>
          )
        })}
      </nav>
    </main>
  )
}
