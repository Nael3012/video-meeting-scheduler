'use client'

import { motion } from 'framer-motion'
import { Laptop, Monitor, Smartphone, Tablet } from 'lucide-react'
import { useState } from 'react'

type Platform = 'Android' | 'iOS' | 'Windows' | 'macOS'

const platforms: { name: Platform; label: string; icon: typeof Smartphone }[] = [
  { name: 'Android', label: 'Android phone', icon: Smartphone },
  { name: 'iOS', label: 'iPhone / iPad', icon: Tablet },
  { name: 'Windows', label: 'Windows PC', icon: Monitor },
  { name: 'macOS', label: 'Mac computer', icon: Laptop },
]

export default function Page() {
  const [platform, setPlatform] = useState<Platform>('Android')
  const selected = platforms.find((item) => item.name === platform)

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-8 text-foreground">
      <section className="w-full max-w-md">
        <div className="mb-8 flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
            <span className="text-sm font-semibold">K</span>
          </div>
          <div>
            <p className="font-semibold tracking-tight">Kite</p>
            <p className="text-xs text-muted-foreground">Join your Zoom meeting</p>
          </div>
        </div>

        <div className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-6">
          <div className="mb-6">
            <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">Device setup</p>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight text-balance">Which OS are you using?</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">Choose your device so we can open Zoom and your calendar the right way.</p>
          </div>

          <div className="grid grid-cols-2 gap-3" role="radiogroup" aria-label="Select operating system">
            {platforms.map((item) => {
              const Icon = item.icon
              const isSelected = platform === item.name

              return (
                <motion.button
                  key={item.name}
                  type="button"
                  whileTap={{ scale: 0.97 }}
                  onClick={() => setPlatform(item.name)}
                  role="radio"
                  aria-checked={isSelected}
                  className={`flex min-h-28 flex-col items-start justify-between rounded-2xl border p-4 text-left transition-colors ${isSelected ? 'border-foreground bg-foreground text-background' : 'border-border bg-background text-foreground hover:bg-muted'}`}
                >
                  <Icon className="size-5" aria-hidden="true" />
                  <span>
                    <span className="block text-sm font-medium">{item.name}</span>
                    <span className={`mt-1 block text-xs ${isSelected ? 'text-background/70' : 'text-muted-foreground'}`}>{item.label}</span>
                  </span>
                </motion.button>
              )
            })}
          </div>

          <motion.div
            key={platform}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-5 rounded-2xl bg-muted p-4"
          >
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">Selected device</p>
            <p className="mt-1 text-sm font-medium">{selected?.label}</p>
            <p className="mt-1 text-xs leading-5 text-muted-foreground">Calendar and Zoom actions will be optimized for {platform}.</p>
          </motion.div>
        </div>
      </section>
    </main>
  )
}
