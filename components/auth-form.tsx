'use client'

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
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const isSignUp = mode === 'sign-up'

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPending(true)
    setError('')
    const result = isSignUp
      ? await authClient.signUp.email({ name, email, password })
      : await authClient.signIn.email({ email, password })
    setPending(false)
    if (result.error) {
      setError('We could not complete that request. Check your details and try again.')
      return
    }
    router.push('/account')
    router.refresh()
  }

  return (
    <Card className="w-full rounded-3xl">
      <CardHeader className="gap-2">
        <CardTitle>{isSignUp ? 'Create your account' : 'Welcome back'}</CardTitle>
        <CardDescription>{isSignUp ? 'Save meeting links and reminders across devices.' : 'Sign in to manage your meetings and calendar preferences.'}</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          {isSignUp && <Input aria-label="Name" autoComplete="name" placeholder="Your name" required value={name} onChange={(event) => setName(event.target.value)} />}
          <Input aria-label="Email" autoComplete="email" placeholder="Email address" required type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          <Input aria-label="Password" autoComplete={isSignUp ? 'new-password' : 'current-password'} placeholder="Password" required minLength={8} type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          {error && <p aria-live="polite" className="text-sm text-destructive">{error}</p>}
          <Button className="min-h-12 rounded-2xl" disabled={pending} type="submit">{pending ? 'Please wait…' : isSignUp ? 'Create account' : 'Sign in'}</Button>
        </form>
      </CardContent>
    </Card>
  )
}
