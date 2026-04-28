import type { PropsWithChildren, ReactNode } from "react";

interface AuthShellProps extends PropsWithChildren {
  mode: "signin" | "signup";
  title: string;
  description: string;
  footer: ReactNode;
}

const BrandMark = () => (
  <div className="mx-auto flex h-16 w-16 items-center justify-center">
    <svg
      viewBox="0 0 80 80"
      className="h-12 w-12 text-[#7b2ff7]"
      aria-hidden="true"
    >
      <g fill="currentColor">
        <rect x="36" y="4" width="8" height="20" rx="4" />
        <rect x="36" y="56" width="8" height="20" rx="4" />
        <rect
          x="36"
          y="4"
          width="8"
          height="20"
          rx="4"
          transform="rotate(45 40 40)"
        />
        <rect
          x="36"
          y="4"
          width="8"
          height="20"
          rx="4"
          transform="rotate(90 40 40)"
        />
        <rect
          x="36"
          y="4"
          width="8"
          height="20"
          rx="4"
          transform="rotate(135 40 40)"
        />
        <circle cx="40" cy="40" r="8" />
      </g>
    </svg>
  </div>
);

const SignupIllustration = () => (
  <div className="relative h-[320px] w-full max-w-[420px] animate-float-soft">
    <div className="absolute left-6 top-7 h-20 w-20 rounded-full bg-[#ff7ea6]/85 shadow-[0_16px_32px_rgba(255,126,166,0.3)]" />
    <div className="absolute right-10 top-10 h-28 w-24 rounded-[36px] bg-[#8d5cff]" />
    <div className="absolute right-2 top-24 h-40 w-28 rounded-[48px] bg-[#ff78b5]/78" />

    <div className="absolute left-10 top-20 flex h-16 w-16 items-center justify-center rounded-full bg-white shadow-[0_16px_35px_rgba(112,69,191,0.18)]">
      <div className="rounded-full bg-[#ff7ea6] p-4 text-white shadow-inner">
        <svg viewBox="0 0 24 24" className="h-6 w-6 fill-current">
          <path d="M17 10h-1V8a4 4 0 1 0-8 0v2H7a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2Zm-6 0V8a2 2 0 1 1 4 0v2h-4Z" />
        </svg>
      </div>
    </div>

    <div className="absolute left-[138px] top-6 h-[248px] w-[164px] rotate-[11deg] rounded-[34px] border-[10px] border-[#40336f] bg-[#f8f7ff] shadow-[0_28px_44px_rgba(67,50,115,0.2)]">
      <div className="mx-auto mt-4 h-2 w-12 rounded-full bg-[#40336f]/25" />
      <div className="mt-8 px-5">
        <div className="rounded-[22px] bg-[#ffb13f] px-4 py-6 shadow-[0_18px_28px_rgba(255,177,63,0.34)]">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-white/90 text-[#ff9d00]">
            <svg viewBox="0 0 24 24" className="h-6 w-6 fill-current">
              <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-3.314 0-6 1.79-6 4v1h12v-1c0-2.21-2.686-4-6-4Z" />
            </svg>
          </div>
          <div className="space-y-2">
            <div className="h-3 rounded-full bg-white/95" />
            <div className="h-3 w-4/5 rounded-full bg-white/85" />
            <div className="h-3 w-3/5 rounded-full bg-[#ffcf80]" />
          </div>
        </div>
      </div>
    </div>

    <div className="absolute bottom-2 left-8">
      <div className="relative h-32 w-24">
        <div className="absolute left-10 top-0 h-9 w-9 rounded-full bg-[#ffcfb8]" />
        <div className="absolute left-4 top-8 h-16 w-16 rounded-[26px_26px_22px_22px] bg-[#8356f2]" />
        <div className="absolute left-1 top-20 h-12 w-7 rounded-full bg-[#2d1b5b]" />
        <div className="absolute right-2 top-20 h-12 w-7 rounded-full bg-[#2d1b5b]" />
        <div className="absolute left-2 top-12 h-10 w-6 rotate-[35deg] rounded-full bg-[#8356f2]" />
        <div className="absolute right-0 top-12 h-10 w-6 -rotate-[35deg] rounded-full bg-[#8356f2]" />
      </div>
    </div>
  </div>
);

const SigninIllustration = () => (
  <div className="relative h-[320px] w-full max-w-[420px] animate-float-slower">
    <div className="absolute right-8 top-10 h-28 w-28 rounded-[40px] bg-[#8f5dff]" />
    <div className="absolute right-28 top-20 h-24 w-24 rounded-[34px] bg-[#ffb13f]" />
    <div className="absolute right-0 top-24 h-36 w-28 rounded-[48px] bg-[#ff7ea6]/85" />

    <div className="absolute left-[145px] top-8 h-[246px] w-[156px] rounded-[28px] border-[10px] border-[#372f67] bg-[#ff6b97] shadow-[0_28px_42px_rgba(67,50,115,0.22)]">
      <div className="mx-auto mt-4 h-2 w-12 rounded-full bg-white/40" />
      <div className="px-6 pt-8">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-white shadow-[inset_0_-6px_12px_rgba(123,47,247,0.12)]">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#8f6cff]/18 text-[#8f6cff]">
            <svg viewBox="0 0 24 24" className="h-8 w-8 fill-current">
              <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-3.314 0-6 1.79-6 4v1h12v-1c0-2.21-2.686-4-6-4Z" />
            </svg>
          </div>
        </div>
        <div className="mt-6 space-y-3">
          <div className="h-3 rounded-full bg-white/90" />
          <div className="h-3 w-4/5 rounded-full bg-white/82" />
          <div className="h-3 w-2/3 rounded-full bg-[#7b2ff7]" />
          <div className="h-3 w-3/5 rounded-full bg-[#ffcf80]" />
        </div>
      </div>
    </div>

    <div className="absolute left-14 top-[102px]">
      <div className="relative h-40 w-24">
        <div className="absolute left-6 top-0 h-10 w-10 rounded-full bg-[#ffcfb8]" />
        <div className="absolute left-0 top-9 h-24 w-24 rounded-[28px_28px_26px_26px] bg-[#7454d8]" />
        <div className="absolute left-1 top-[4.5rem] h-12 w-7 rotate-[22deg] rounded-full bg-[#7454d8]" />
        <div className="absolute right-0 top-16 h-12 w-7 -rotate-[30deg] rounded-full bg-[#7454d8]" />
        <div className="absolute left-4 bottom-0 h-16 w-7 rounded-full bg-[#2c2362]" />
        <div className="absolute right-4 bottom-0 h-16 w-7 rounded-full bg-[#2c2362]" />
      </div>
    </div>

    <div className="absolute bottom-5 right-10">
      <div className="relative h-28 w-32">
        <div className="absolute left-10 top-0 h-9 w-9 rounded-full bg-[#ffcfb8]" />
        <div className="absolute left-2 top-8 h-12 w-24 rounded-[28px] bg-[#7a58e2]" />
        <div className="absolute left-0 top-16 h-12 w-12 rounded-full bg-[#2c2362]" />
        <div className="absolute right-0 top-[4.5rem] h-10 w-14 rounded-full bg-[#2c2362]" />
        <div className="absolute left-12 top-10 h-12 w-16 rounded-[18px] bg-[#56309d] shadow-[0_14px_22px_rgba(44,35,98,0.2)]" />
      </div>
    </div>
  </div>
);

export const AuthShell = ({
  mode,
  title,
  description,
  footer,
  children,
}: AuthShellProps) => (
  <div className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,#f8f2ff_0%,#f5efff_45%,#efe7ff_100%)] text-slate-800">
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute left-[-8rem] top-[-7rem] h-72 w-72 rounded-full bg-white/55 blur-3xl" />
      <div className="absolute bottom-[-8rem] right-[-6rem] h-80 w-80 rounded-full bg-[#d8c6ff]/50 blur-3xl" />
    </div>

    <div className="relative mx-auto flex min-h-screen max-w-6xl flex-col justify-center px-4 py-8 sm:px-6 lg:px-8">
      <div className="animate-rise-in overflow-hidden rounded-[30px] border border-white/75 bg-white/92 shadow-[0_30px_80px_rgba(109,79,180,0.16)] backdrop-blur">
        <div className="grid lg:grid-cols-[minmax(0,430px)_1fr]">
          <section className="flex items-center justify-center p-6 sm:p-8 lg:p-10">
            <div className="w-full max-w-[320px]">
              <BrandMark />
              <div className="mt-3 space-y-2 text-center">
                <h1 className="text-[2rem] font-semibold leading-tight text-slate-800">
                  {title}
                </h1>
                <p className="text-sm leading-6 text-slate-500">{description}</p>
              </div>
              <div className="mt-8">{children}</div>
            </div>
          </section>

          <aside className="relative hidden items-center justify-center overflow-hidden bg-[#f4f0ff] px-10 py-12 lg:flex">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.72),transparent_38%),radial-gradient(circle_at_bottom_right,rgba(185,157,255,0.18),transparent_44%)]" />
            <div className="relative z-10 flex w-full items-center justify-center">
              {mode === "signup" ? <SignupIllustration /> : <SigninIllustration />}
            </div>
          </aside>
        </div>
      </div>

      <p className="mx-auto max-w-3xl pt-6 text-center text-xs leading-6 text-slate-500 sm:text-sm">
        {footer}
      </p>
    </div>
  </div>
);
