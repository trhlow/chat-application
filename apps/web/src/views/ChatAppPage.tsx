const ChatAppPage = () => {
  return (
    <main className="min-h-svh bg-background p-6 text-foreground">
      <section className="mx-auto flex min-h-[calc(100svh-3rem)] max-w-6xl items-center justify-center">
        <div className="text-center">
          <p className="text-sm font-medium text-muted-foreground">InChat</p>
          <h1 className="mt-3 text-3xl font-bold">Realtime chat workspace</h1>
          <p className="mt-3 text-muted-foreground">
            Backend API and web app are now wired into the monorepo layout.
          </p>
        </div>
      </section>
    </main>
  );
};

export default ChatAppPage;
