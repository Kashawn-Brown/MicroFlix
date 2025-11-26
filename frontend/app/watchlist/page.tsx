export default function WatchlistPage() {
  return (
    <section className="flex w-full flex-col gap-4">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">My Watchlist</h1>
        <p className="text-sm text-slate-300">
          This page will show movies you&apos;ve added to your watchlist via the
          rating-service engagement API.
        </p>
      </header>

      {/* TODO: fetch watchlist items for the current user */}
      <div className="rounded-md border border-dashed border-slate-700 p-4 text-sm text-slate-400">
        Watchlist items will appear here once we connect to the backend.
      </div>
    </section>
  );
}

// USERS WATCHLIST PAGE //