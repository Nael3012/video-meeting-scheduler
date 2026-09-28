import Link from 'next/link'
import { headers } from 'next/headers'
import { redirect } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { auth } from '@/lib/auth'

export default async function AccountPage() {
  const session = await auth.api.getSession({ headers: await headers() })
  if (!session?.user) redirect('/sign-in')

  return (
    <main className="min-h-screen bg-background px-4 py-8 pb-10">
      <section className="mx-auto flex w-full max-w-md flex-col gap-6">
        <header className="flex items-center justify-between">
          <div className="flex flex-col gap-1">
            <Link className="text-sm font-semibold" href="/">Meetflow</Link>
            <h1 className="text-2xl font-semibold tracking-tight">Account</h1>
          </div>
          <form action="/api/auth/sign-out" method="post"><Button variant="outline" className="rounded-xl" type="submit">Sign out</Button></form>
        </header>
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle>{session.user.name}</CardTitle>
            <CardDescription>{session.user.email}</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center justify-between rounded-2xl bg-muted p-4 text-sm"><span>Account status</span><span className="font-medium">Active</span></div>
            <div className="flex items-center justify-between rounded-2xl bg-muted p-4 text-sm"><span>Calendar sync</span><span className="font-medium">Manage on Home</span></div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
