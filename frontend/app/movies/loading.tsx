export default function LoadingMovies() {
  const items = Array.from({ length: 15 });

  return (
    <section className="flex w-full flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Movies</h1>
        <p className="text-sm text-slate-300">
          Loading the MicroFlix catalog…
        </p>
      </div>

      {/* Simple skeleton grid while movies load */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
        {items.map((_, index) => (
          <div
            key={index}
            className="flex flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40"
          >
            <div className="aspect-[2/3] bg-slate-800 animate-pulse" />

            <div className="flex flex-1 flex-col gap-2 p-3">
              <div className="h-4 w-3/4 rounded bg-slate-800 animate-pulse" />
              <div className="h-3 w-1/4 rounded bg-slate-800 animate-pulse" />
              <div className="h-3 w-2/3 rounded bg-slate-800 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
