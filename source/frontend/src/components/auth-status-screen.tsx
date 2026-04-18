import { LoaderCircle, ShieldCheck } from "lucide-react"

interface AuthStatusScreenProps {
  title?: string
  description?: string
}

export function AuthStatusScreen({
  title = "Dang dong bo phien dang nhap",
  description = "Ung dung dang kiem tra access token va thong tin nguoi dung.",
}: AuthStatusScreenProps) {
  return (
    <div className="flex min-h-svh items-center justify-center px-6">
      <div className="glass-strong max-w-lg rounded-[2rem] p-8 text-center sm:p-10">
        <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-slate-950 text-white shadow-[0_24px_50px_-28px_rgba(15,23,42,0.5)]">
          <LoaderCircle className="size-7 animate-spin text-cyan-300" />
        </div>
        <div className="mt-6 inline-flex items-center gap-2 rounded-full border border-slate-900/8 bg-slate-900/5 px-4 py-2 text-[11px] font-semibold tracking-[0.24em] text-slate-700 uppercase">
          <ShieldCheck className="size-3.5" />
          Session bootstrap
        </div>
        <h1 className="mt-5 text-2xl font-semibold tracking-tight text-foreground">
          {title}
        </h1>
        <p className="mt-3 text-sm leading-7 text-muted-foreground">
          {description}
        </p>
      </div>
    </div>
  )
}
