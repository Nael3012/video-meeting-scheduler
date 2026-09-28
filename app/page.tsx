'use client'

import { AnimatePresence, motion } from 'framer-motion'
import {
  Bell,
  CalendarDays,
  Check,
  ChevronRight,
  Clock3,
  ExternalLink,
  Laptop,
  LayoutGrid,
  Monitor,
  MoreHorizontal,
  Smartphone,
  Plus,
  Settings2,
  Video,
  X,
} from 'lucide-react'
import { useState } from 'react'

type Platform = 'Android' | 'iOS' | 'Windows' | 'macOS'

const platformOptions: { name: Platform; icon: typeof Smartphone; note: string }[] = [
  { name: 'Android', icon: Smartphone, note: 'Google Calendar' },
  { name: 'iOS', icon: Smartphone, note: 'Apple Calendar' },
  { name: 'Windows', icon: Monitor, note: 'Outlook Calendar' },
  { name: 'macOS', icon: Laptop, note: 'Apple Calendar' },
]

const calendarAccess: Record<Platform, { title: string; description: string; action: string; icon: typeof CalendarDays }[]> = {
  Android: [
    { title: 'Google Calendar', description: 'Sinkronkan jadwal dan pengingat secara otomatis.', action: 'Connect Google Calendar', icon: CalendarDays },
    { title: 'Join dari notifikasi', description: 'Buka link Zoom langsung dari reminder meeting.', action: 'Use reminders', icon: Bell },
  ],
  iOS: [
    { title: 'Apple Calendar', description: 'Gunakan kalender bawaan iPhone untuk jadwal Zoom.', action: 'Connect Apple Calendar', icon: CalendarDays },
    { title: 'Add to Siri', description: 'Minta Siri mengingatkan waktu meeting berikutnya.', action: 'Set up Siri reminder', icon: Bell },
  ],
  Windows: [
    { title: 'Outlook Calendar', description: 'Tambahkan event ke kalender Outlook di desktop.', action: 'Connect Outlook Calendar', icon: CalendarDays },
    { title: 'Open Zoom desktop', description: 'Lanjutkan meeting melalui aplikasi Zoom untuk Windows.', action: 'Open Zoom app', icon: ExternalLink },
  ],
  macOS: [
    { title: 'Apple Calendar', description: 'Tambahkan meeting ke Calendar dan dapatkan alert native.', action: 'Connect Apple Calendar', icon: CalendarDays },
    { title: 'Open Zoom for Mac', description: 'Buka link meeting di aplikasi Zoom desktop.', action: 'Open Zoom app', icon: ExternalLink },
  ],
}

const meetings = [
  { time: '09:30', period: 'AM', title: 'Weekly product sync', people: '8 attendees', kind: 'Team' },
  { time: '11:00', period: 'AM', title: 'Design critique', people: '4 attendees', kind: 'Work' },
  { time: '02:30', period: 'PM', title: 'Client check-in', people: '3 attendees', kind: 'Client' },
]

export default function Page() {
  const [platform, setPlatform] = useState<Platform>('Android')
  const [activeTab, setActiveTab] = useState('Today')
  const [showComposer, setShowComposer] = useState(false)
  const [reminded, setReminded] = useState(false)
  const [connected, setConnected] = useState(false)
  const selectedPlatform = platformOptions.find((item) => item.name === platform)

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto flex min-h-screen max-w-6xl flex-col lg:flex-row">
        <aside className="hidden w-64 shrink-0 flex-col border-r border-border px-6 py-8 lg:flex">
          <div className="flex items-center gap-3"><div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground"><Video className="size-4" /></div><span className="font-semibold tracking-tight">Kite</span></div>
          <nav className="mt-12 flex flex-col gap-2" aria-label="Main navigation">
            {['Today', 'Calendar', 'Reminders'].map((item, index) => <button key={item} onClick={() => setActiveTab(item)} className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-colors ${activeTab === item ? 'bg-accent font-medium text-accent-foreground' : 'text-muted-foreground hover:bg-muted'}`}>{index === 0 ? <LayoutGrid className="size-4" /> : index === 1 ? <CalendarDays className="size-4" /> : <Bell className="size-4" />}{item}{item === 'Reminders' && <span className="ml-auto rounded-full bg-primary px-2 py-0.5 text-[10px] text-primary-foreground">3</span>}</button>)}
          </nav>
          <div className="mt-auto rounded-2xl bg-secondary p-4"><Settings2 className="size-4 text-primary" /><p className="mt-3 text-sm font-medium">Ready to join</p><p className="mt-1 text-xs leading-5 text-muted-foreground">Choose your device so every meeting opens the right way.</p></div>
        </aside>

        <section className="flex-1 px-4 pb-28 pt-5 sm:px-8 lg:px-14 lg:pb-10 lg:pt-10">
          <header className="flex items-center justify-between"><div className="flex items-center gap-3 lg:hidden"><div className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground"><Video className="size-4" /></div><span className="font-semibold tracking-tight">Kite</span></div><div className="hidden lg:block"><p className="text-sm text-muted-foreground">Tuesday, September 28, 2026</p><h1 className="mt-1 text-2xl font-semibold tracking-tight">Good morning, Alex</h1></div><div className="flex items-center gap-2"><button className="relative rounded-xl border border-border p-2.5 text-muted-foreground" aria-label="Notifications"><Bell className="size-4" /><span className="absolute right-2 top-2 size-1.5 rounded-full bg-foreground" /></button><div className="ml-1 flex size-9 items-center justify-center rounded-xl bg-secondary text-sm font-medium">AM</div></div></header>
          <div className="mt-7 lg:hidden"><p className="text-sm text-muted-foreground">Tuesday, September 28, 2026</p><h1 className="mt-1 text-2xl font-semibold tracking-tight">Good morning, Alex</h1></div>

          <div className="mt-7 rounded-2xl border border-border bg-card p-4 sm:p-5"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">Join experience</p><h2 className="mt-1 text-lg font-semibold tracking-tight">Which OS are you using?</h2></div><span className="rounded-full bg-muted px-2.5 py-1 text-[10px] font-medium text-muted-foreground">Step 1 of 2</span></div><div className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1 sm:grid-cols-4" role="tablist" aria-label="Select operating system">{platformOptions.map((item) => { const Icon = item.icon; return <button key={item.name} onClick={() => { setPlatform(item.name); setConnected(false) }} role="tab" aria-selected={platform === item.name} className={`flex min-w-0 flex-col items-center gap-1 rounded-lg px-1 py-2.5 text-[10px] transition-all sm:text-xs ${platform === item.name ? 'bg-background font-medium text-foreground shadow-sm' : 'text-muted-foreground'}`}><Icon className="size-4" />{item.name}</button> })}</div><AnimatePresence mode="wait"><motion.p key={platform} initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} className="mt-3 text-xs text-muted-foreground">Optimized for {selectedPlatform?.note} on {platform}.</motion.p></AnimatePresence></div>

          <motion.div layout className="mt-5 overflow-hidden rounded-3xl bg-primary p-5 text-primary-foreground sm:p-8"><div className="flex items-start justify-between gap-4"><div><p className="text-sm text-primary-foreground/65">Up next · in 18 min</p><h2 className="mt-2 max-w-md text-2xl font-semibold tracking-tight sm:text-3xl">Weekly product sync</h2></div><span className="rounded-full bg-primary-foreground/10 px-3 py-1.5 text-xs">Zoom</span></div><div className="mt-7 flex flex-wrap items-center gap-x-5 gap-y-3 text-sm text-primary-foreground/70"><span className="flex items-center gap-2"><Clock3 className="size-4" />09:30 – 10:15 AM</span><span className="flex items-center gap-2"><Globe2 className="size-4" />Your local time</span></div><div className="mt-7 flex items-center justify-between border-t border-primary-foreground/15 pt-5"><div className="flex -space-x-2"><div className="flex size-8 items-center justify-center rounded-full border-2 border-primary bg-secondary text-[10px] text-secondary-foreground">JD</div><div className="flex size-8 items-center justify-center rounded-full border-2 border-primary bg-muted text-[10px] text-muted-foreground">SK</div><div className="flex size-8 items-center justify-center rounded-full border-2 border-primary bg-secondary text-[10px] text-secondary-foreground">+6</div></div><button className="flex items-center gap-2 rounded-xl bg-primary-foreground px-4 py-2.5 text-sm font-medium text-primary transition-transform hover:scale-[1.02]"><Video className="size-4" />Join on {platform}</button></div></motion.div>

          <div className="mt-8 flex items-center justify-between"><div><h2 className="text-lg font-semibold tracking-tight">Calendar access</h2><p className="mt-1 text-sm text-muted-foreground">Use the calendar that fits your device.</p></div><button onClick={() => setShowComposer(true)} className="flex items-center gap-2 rounded-xl bg-secondary px-3.5 py-2.5 text-sm font-medium transition-colors hover:bg-accent"><Plus className="size-4" /><span className="hidden sm:inline">Schedule</span></button></div>
          <div className="mt-4 flex flex-col gap-3">{calendarAccess[platform].map((option, index) => { const Icon = option.icon; const isConnected = index === 0 && connected; return <motion.button key={option.title} layout initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.08 }} onClick={() => index === 0 && setConnected(true)} className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 text-left transition-colors hover:bg-muted"><div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-secondary"><Icon className="size-4" /></div><div className="min-w-0 flex-1"><p className="text-sm font-medium">{option.title}</p><p className="mt-1 text-xs leading-5 text-muted-foreground">{isConnected ? 'Connected and ready for your next meeting.' : option.description}</p></div>{isConnected ? <Check className="size-4 text-foreground" /> : <ChevronRight className="size-4 text-muted-foreground" />}</motion.button> })}</div>

          <div className="mt-8 flex items-center justify-between"><div><h2 className="text-lg font-semibold tracking-tight">Your schedule</h2><p className="mt-1 text-sm text-muted-foreground">3 meetings planned for today</p></div></div><div className="mt-4 flex flex-col gap-3">{meetings.map((meeting, index) => <motion.div key={meeting.title} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: index * 0.08 }} className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-4"><div className="w-14 shrink-0"><p className="text-sm font-medium">{meeting.time}</p><p className="text-xs text-muted-foreground">{meeting.period}</p></div><div className="h-10 w-1 rounded-full bg-foreground" /><div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{meeting.title}</p><p className="mt-1 text-xs text-muted-foreground">{meeting.people} · {meeting.kind}</p></div><button className="rounded-lg p-2 text-muted-foreground" aria-label={`More options for ${meeting.title}`}><MoreHorizontal className="size-4" /></button></motion.div>)}</div>
          <div className="mt-8 rounded-2xl border border-dashed border-border p-4"><div className="flex items-start gap-3"><div className="rounded-lg bg-secondary p-2"><Bell className="size-4" /></div><div className="flex-1"><p className="text-sm font-medium">Reminder after lunch</p><p className="mt-1 text-xs leading-5 text-muted-foreground">Keep a 45 minute window open for focused work.</p></div><button onClick={() => setReminded(!reminded)} className="rounded-lg p-2 text-muted-foreground hover:bg-muted" aria-label={reminded ? 'Reminder added' : 'Add reminder'}>{reminded ? <Check className="size-4" /> : <Plus className="size-4" />}</button></div></div>
        </section>
      </div>
      <nav className="fixed inset-x-0 bottom-0 z-10 flex border-t border-border bg-background/95 px-5 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 backdrop-blur lg:hidden" aria-label="Mobile navigation">{['Today', 'Calendar', 'Reminders'].map((item, index) => <button key={item} onClick={() => setActiveTab(item)} className={`flex flex-1 flex-col items-center gap-1 text-[10px] ${activeTab === item ? 'font-medium text-foreground' : 'text-muted-foreground'}`}>{index === 0 ? <LayoutGrid className="size-5" /> : index === 1 ? <CalendarDays className="size-5" /> : <Bell className="size-5" />}{item}</button>)}</nav>
      <AnimatePresence>{showComposer && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-20 flex items-end justify-center bg-foreground/20 p-0 backdrop-blur-sm sm:items-center sm:p-5"><motion.div initial={{ y: 40 }} animate={{ y: 0 }} exit={{ y: 40 }} className="w-full max-w-md rounded-t-3xl bg-background p-6 shadow-2xl sm:rounded-3xl"><div className="flex items-center justify-between"><div><p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">New event</p><h2 className="mt-1 text-xl font-semibold">Schedule a Zoom meeting</h2></div><button onClick={() => setShowComposer(false)} className="rounded-lg p-2 text-muted-foreground hover:bg-muted" aria-label="Close"><X className="size-4" /></button></div><div className="mt-6 flex flex-col gap-4"><input className="rounded-xl border border-input bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-ring" placeholder="Meeting title" autoFocus /><div className="flex gap-3"><input className="min-w-0 flex-1 rounded-xl border border-input bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-ring" placeholder="Date" /><input className="w-28 rounded-xl border border-input bg-background px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-ring" placeholder="Time" /></div><button onClick={() => { setShowComposer(false); setReminded(true) }} className="flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-medium text-primary-foreground"><Plus className="size-4" />Create meeting</button></div></motion.div></motion.div>}</AnimatePresence>
    </main>
  )
}
function Globe2({ className }: { className?: string }) { return <span className={className} aria-hidden="true">◎</span> }
