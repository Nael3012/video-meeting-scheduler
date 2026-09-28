import Link from 'next/link'
import { AuthForm } from '@/components/auth-form'

export default function SignUpPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-8">
      <section className="flex w-full max-w-sm flex-col gap-6">
        <div className="flex flex-col gap-2 text-center">
          <Link className="text-sm font-semibold tracking-tight" href="/">Meetflow</Link>
          <h1 className="text-3xl font-semibold tracking-tight text-balance">Keep every join link in one place.</h1>
        </div>
        <AuthForm mode="sign-up" />
        <p className="text-center text-sm text-muted-foreground">Already have an account? <Link className="font-medium text-foreground underline underline-offset-4" href="/sign-in">Sign in</Link></p>
      </section>
    </main>
  )
}
