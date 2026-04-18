import { Sparkles } from "lucide-react"

import type { ReactNode } from "react"

interface AuthShellProps {
  title: string
  description: string
  illustration: "signin" | "signup"
  children: ReactNode
}

export function AuthShell({
  title,
  description,
  illustration,
  children,
}: AuthShellProps) {
  const isSignIn = illustration === "signin"

  return (
    <div className="relative min-h-svh overflow-hidden bg-[linear-gradient(180deg,#f7f3ff_0%,#f1ebff_55%,#eee5ff_100%)] px-4 py-6 sm:px-6 lg:px-8">
      <div className="pointer-events-none absolute -left-16 top-20 h-56 w-56 rounded-full bg-violet-300/35 blur-3xl" />
      <div className="pointer-events-none absolute -right-14 bottom-16 h-64 w-64 rounded-full bg-fuchsia-200/40 blur-3xl" />

      <div className="relative mx-auto flex min-h-[calc(100svh-3rem)] w-full max-w-6xl items-center justify-center">
        <div className="w-full max-w-5xl rounded-[1.15rem] border border-violet-200/70 bg-white/65 p-3 shadow-[0_24px_50px_-34px_rgba(76,29,149,0.45)] backdrop-blur-sm">
          <div className="grid overflow-hidden rounded-2xl border border-violet-200/80 bg-white lg:grid-cols-[1.03fr_0.97fr]">
            <section className="px-6 py-7 sm:px-8 sm:py-8">
              <div className="mx-auto w-full max-w-[420px]">
                <div className="mb-5 flex justify-center">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-violet-100 text-violet-700">
                    <Sparkles className="size-6" />
                  </div>
                </div>

                <h1 className="text-center text-3xl leading-tight font-semibold tracking-[-0.02em] text-slate-900 sm:text-[2rem]">
                  {title}
                </h1>
                <p className="mt-2 text-center text-sm text-slate-600">{description}</p>

                <div className="mt-6">{children}</div>
              </div>
            </section>

            <section className="relative hidden overflow-hidden bg-[#ece9f4] p-10 lg:flex lg:items-center lg:justify-center">
              <div className="pointer-events-none absolute left-8 top-8 h-20 w-20 rounded-full bg-violet-300/25" />
              <div className="pointer-events-none absolute right-12 top-14 h-14 w-14 rounded-full bg-orange-300/35" />
              <div className="pointer-events-none absolute bottom-10 right-12 h-24 w-24 rounded-full bg-fuchsia-300/25" />

              <div className="relative flex h-[310px] w-[290px] items-end justify-center">
                <div className="absolute top-3 left-1/2 h-[220px] w-[132px] -translate-x-1/2 rounded-[1.9rem] border-4 border-[#343d7a] bg-[#ff80a7] shadow-[0_18px_34px_-18px_rgba(31,41,55,0.55)]">
                  <div className="mx-auto mt-4 h-2 w-10 rounded-full bg-[#3f467f]" />
                  <div className="mx-auto mt-5 flex h-16 w-16 items-center justify-center rounded-full bg-white/90">
                    <div className="h-9 w-9 rounded-full bg-violet-300" />
                  </div>
                  <div className="mx-auto mt-5 h-2 w-20 rounded-full bg-white/85" />
                  <div className="mx-auto mt-2 h-2 w-16 rounded-full bg-white/70" />
                </div>

                <div className="absolute bottom-5 left-3 flex h-24 w-24 items-center justify-center rounded-full bg-violet-500/85 text-white shadow-[0_16px_30px_-18px_rgba(76,29,149,0.55)]">
                  <Sparkles className="size-8" />
                </div>

                <div className="absolute right-2 bottom-0 h-28 w-36 rounded-[1.4rem] border border-violet-300/40 bg-violet-500/90 p-4 shadow-[0_18px_30px_-22px_rgba(88,28,135,0.7)]">
                  <div className="h-2 w-14 rounded-full bg-violet-100/80" />
                  <div className="mt-4 h-2 w-full rounded-full bg-violet-200/70" />
                  <div className="mt-2 h-2 w-4/5 rounded-full bg-violet-200/70" />
                  <div className="mt-4 h-7 w-16 rounded-lg bg-white/90" />
                </div>

                <div className="absolute top-0 right-6 h-10 w-10 rounded-full border-4 border-violet-400/65" />
                <div className="absolute top-24 left-10 h-14 w-14 rounded-full border-4 border-amber-300/70" />
              </div>
            </section>
          </div>
        </div>
      </div>

      <p className="relative mx-auto mt-6 max-w-5xl text-center text-[11px] text-violet-700/70 sm:text-xs">
        Bang cach tiep tuc, ban dong y voi{" "}
        <span className="font-medium text-violet-700"> Dieu khoan dich vu </span>
        va{" "}
        <span className="font-medium text-violet-700"> Chinh sach bao mat</span>
        {isSignIn ? " cua chung toi." : " cua Moji."}
      </p>
    </div>
  )
}
