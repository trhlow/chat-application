export const FullScreenLoader = () => (
  <div className="flex min-h-screen items-center justify-center bg-background px-6">
    <div className="flex items-center gap-3 rounded-full border border-border bg-card px-5 py-3 text-sm text-muted-foreground shadow-soft">
      <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-primary" />
      Restoring your session...
    </div>
  </div>
);
