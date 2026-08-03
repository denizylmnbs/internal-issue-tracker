export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded bg-signal font-heading text-sm font-bold text-signal-foreground">
            I
          </div>
          <span className="font-heading text-lg font-semibold tracking-tight">
            Internal Issue Tracker
          </span>
        </div>
        {children}
      </div>
    </div>
  );
}
