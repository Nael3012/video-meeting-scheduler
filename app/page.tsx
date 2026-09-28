'use client'

import { motion } from 'framer-motion'
import { CalendarDays, Check, ChevronDown, Laptop, Monitor, Smartphone, Tablet, Video } from 'lucide-react'
import { useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

type Platform = 'Android' | 'iOS' | 'Windows' | 'macOS'

const platforms: { name: Platform; label: string; icon: typeof Smartphone }[] = [
  { name: 'Android', label: 'Android phone', icon: Smartphone },
  { name: 'iOS', label: 'iPhone / iPad', icon: Tablet },
  { name: 'Windows', label: 'Windows PC', icon: Monitor },
  { name: 'macOS', label: 'Mac computer', icon: Laptop },
]

export default function Page() {
  const [platform, setPlatform] = useState<Platform>('Android')
  const [joined, setJoined] = useState(false)
  const selected = platforms.find((item) => item.name === platform) ?? platforms[0]
  const PlatformIcon = selected.icon

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-6 text-foreground">
      <section className="flex w-full max-w-md flex-col gap-5">
        <header className="flex items-center justify-between">
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
                <SelectTrigger className="min-h-14 rounded-2xl" id="operating-system">
                  <div className="flex items-center gap-3">
                    <PlatformIcon aria-hidden="true" />
                    <SelectValue placeholder="Choose your OS" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  {platforms.map((item) => {
                    const Icon = item.icon
                    return (
                      <SelectItem key={item.name} value={item.name}>
                        <span className="flex items-center gap-2"><Icon aria-hidden="true" />{item.name} <span className="text-muted-foreground">· {item.label}</span></span>
                      </SelectItem>
                    )
                  })}
                </SelectContent>
              </Select>
            </div>

            <motion.div key={platform} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} className="flex items-start gap-3 rounded-2xl border border-border bg-background p-4">
              <PlatformIcon aria-hidden="true" className="mt-0.5 text-muted-foreground" />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <p className="text-sm font-medium">{selected.label}</p>
                <p className="text-xs leading-5 text-muted-foreground">We&apos;ll use the {platform} calendar and Zoom app flow.</p>
              </div>
              <Check aria-hidden="true" className="text-muted-foreground" />
            </motion.div>

            <Button className="min-h-12 w-full rounded-2xl" onClick={() => setJoined(true)}>
              {joined ? 'Zoom link ready' : 'Join Zoom meeting'}
              {!joined && <ChevronDown aria-hidden="true" data-icon="inline-end" />}
            </Button>
          </CardContent>
        </Card>

        <p className="text-center text-xs leading-5 text-muted-foreground">Your selection only controls how the meeting opens on this device.</p>
      </section>
    </main>
  )
}
