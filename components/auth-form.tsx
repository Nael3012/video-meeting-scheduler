'use client'

import Image from 'next/image'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { authClient } from '@/lib/auth-client'

type AuthFormProps = { mode: 'sign-in' | 'sign-up' }

export function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter()
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const isSignUp = mode === 'sign-up'

  async function handleGoogleAuth() {
    setPending(true)
    setError('')
    const result = await authClient.signIn.social({ provider: 'google', callbackURL: '/account' })
    if (result.error) {
      setPending(false)
      setError('Google sign-in is not available yet. Please try again later.')
      return
    }
    router.refresh()
  }

  function handleAccessRequest(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('Your request has been noted. An administrator must approve access before signup is available.')
  }

  return (
    <Card className="w-full rounded-2xl border-border/70 shadow-sm">
      <CardHeader className="gap-1.5 px-4 pb-3 pt-4 sm:px-6">
        <CardTitle className="text-lg">{isSignUp ? 'Request access' : 'Welcome back'}</CardTitle>
        <CardDescription className="text-xs leading-5">
          {isSignUp ? 'Signup requires approval from the Meetflow administrator.' : 'Continue with your Google account to manage meetings.'}
        </CardDescription>
      </CardHeader>
      <CardContent className="px-4 pb-4 sm:px-6">
        {isSignUp ? (
          <form className="flex flex-col gap-3" onSubmit={handleAccessRequest}>
            <Input aria-label="Name" autoComplete="name" className="min-h-10 text-sm" placeholder="Your name" required value={name} onChange={(event) => setName(event.target.value)} />
            <Button className="min-h-10 rounded-xl text-sm" type="submit">Request approval</Button>
          </form>
        ) : (
          <Button className="min-h-10 w-full rounded-xl text-sm" disabled={pending} onClick={handleGoogleAuth} type="button">
            <Image alt="" aria-hidden="true" className="size-5" height={20} src="/icons/google.png" width={20} />
            {pending ? 'Connecting…' : 'Continue with Google'}
          </Button>
        )}
        {error && <p aria-live="polite" className="mt-3 text-xs leading-5 text-muted-foreground">{error}</p>}
      </CardContent>
    </Card>
  )
}
