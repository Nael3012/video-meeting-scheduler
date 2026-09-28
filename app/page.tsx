'use client'

import { motion } from 'framer-motion'
import { Bell, CalendarDays, Check, ChevronDown, Home, Laptop, Monitor, Settings, Smartphone, Tablet, Video } from 'lucide-react'
import { useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type Platform = 'Android' | 'iOS' | 'Windows' | 'macOS'
type CalendarSync = 'Google Calendar' | 'Local Calendar'

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

export default function Page() {
  const [platform, setPlatform] = useState<Platform>('Android')
  const [calendarSync, setCalendarSync] = useState<CalendarSync>('Google Calendar')
  const [joined, setJoined] = useState(false)
  const selected = platforms.find((item) => item.name === platform) ?? platforms[0]
  const PlatformIcon = selected.icon

  return (
    <main className="min-h-screen bg-background px-3 pb-24 pt-4 text-foreground sm:px-4 sm:py-6">
      <section className="mx-auto flex w-full max-w-md flex-col gap-5">
        <header className="flex items-center justify-between px-1">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
              <Video aria-hidden="true" />
            </div>
            <div>
              <p className="font-semibold tracking-tight">Kite</p>
              <p className="text-xs text-muted-foreground">Join your Zoom meeting</p>
            </div>
          </div>
          <Badge variant="secondary">Today</Badge>
        </header>

        <Card className="rounded-3xl shadow-sm">
          <CardHeader className="gap-2 p-5 sm:p-6">
            <Badge className="w-fit" variant="outline">Next meeting</Badge>
            <CardTitle className="text-2xl tracking-tight text-balance">Design sync with the product team</CardTitle>
            <CardDescription className="leading-6">Choose your OS to open Zoom and your calendar with the right native flow.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-5 p-5 pt-0 sm:p-6 sm:pt-0">
            <div className="flex items-center justify-between rounded-2xl bg-muted p-4">
              <div className="flex items-center gap-3">
                <CalendarDays aria-hidden="true" className="text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Today, 10:30 AM</p>
                  <p className="text-xs text-muted-foreground">45 minutes · Zoom</p>
                </div>
              </div>
              <Badge variant="secondary">In 24 min</Badge>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium" htmlFor="operating-system">Operating system</label>
              <Select value={platform} onValueChange={(value) => setPlatform(value as Platform)}>
                <SelectTrigger className="min-h-14 w-full rounded-2xl" id="operating-system">
                  <div className="flex items-center gap-3">
                    <PlatformIcon aria-hidden="true" />
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
              <label className="text-sm font-medium" htmlFor="calendar-sync">Calendar access</label>
              <Select value={calendarSync} onValueChange={(value) => setCalendarSync(value as CalendarSync)}>
                <SelectTrigger className="min-h-14 w-full rounded-2xl" id="calendar-sync">
                  <div className="flex items-center gap-3">
                    <CalendarDays aria-hidden="true" className="text-muted-foreground" />
                    <SelectValue placeholder="Choose calendar" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {calendarOptions.map((item) => (
                      <SelectItem key={item.name} value={item.name}>
                        <span className="flex flex-col items-start gap-0.5">
                          <span>{item.name}</span>
                          <span className="text-xs text-muted-foreground">{item.label}</span>
                        </span>
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            <motion.div key={`${platform}-${calendarSync}`} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} className="flex items-start gap-3 rounded-2xl border border-border bg-background p-4">
              <PlatformIcon aria-hidden="true" className="mt-0.5 text-muted-foreground" />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <p className="text-sm font-medium">{selected.label}</p>
                <p className="text-xs leading-5 text-muted-foreground">{calendarSync} will open with the {platform} Zoom flow.</p>
              </div>
              <Check aria-hidden="true" className="text-muted-foreground" />
            </motion.div>

            <Button className="min-h-12 w-full rounded-2xl" onClick={() => setJoined(true)}>
              {joined ? 'Zoom link ready' : 'Join Zoom meeting'}
              {!joined && <ChevronDown aria-hidden="true" data-icon="inline-end" />}
            </Button>
          </CardContent>
        </Card>

        <p className="px-2 text-center text-xs leading-5 text-muted-foreground">Your selection only controls how the meeting opens on this device.</p>
      </section>

      <nav aria-label="Primary navigation" className="fixed inset-x-2 bottom-2 z-10 mx-auto flex max-w-md items-center gap-0 rounded-3xl border border-border bg-card/95 p-1 shadow-lg backdrop-blur sm:inset-x-4 sm:p-2">
        <Button className="min-h-12 min-w-0 flex-1 flex-col gap-0 rounded-2xl px-0 text-[10px] tracking-tight sm:gap-1 sm:px-2 sm:text-[10px]" variant="ghost">
          <CalendarDays aria-hidden="true" data-icon="inline-start" />
          Plan
        </Button>
        <Button aria-current="page" className="min-h-12 min-w-0 flex-1 flex-col gap-0 rounded-none px-0 text-[10px] tracking-tight sm:gap-1 sm:px-2 sm:text-[10px]" variant="ghost">
          <Home aria-hidden="true" data-icon="inline-start" />
          Home
        </Button>
        <Button className="min-h-12 min-w-0 flex-1 flex-col gap-0 rounded-2xl px-0 text-[10px] tracking-tight sm:gap-1 sm:px-2 sm:text-[10px]" variant="ghost">
          <Bell aria-hidden="true" data-icon="inline-start" />
          Alerts
        </Button>
        <Button className="min-h-12 min-w-0 flex-1 flex-col gap-0 rounded-2xl px-0 text-[10px] tracking-tight sm:gap-1 sm:px-2 sm:text-[10px]" variant="ghost">
          <Settings aria-hidden="true" data-icon="inline-start" />
          More
        </Button>
      </nav>
    </main>
  )
}
