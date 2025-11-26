export default function MoviesPage() {
  return (
    <section className="flex w-full flex-col gap-4">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Movies</h1>
        <p className="text-sm text-slate-300">
          This page will show the movie catalog with search, filters, and
          pagination powered by the movie-service.
        </p>
      </header>

      {/* TODO: movie search filters + list from backend */}
      <div className="rounded-md border border-dashed border-slate-700 p-4 text-sm text-slate-400">
        Movie list goes here. Will fetch data from the backend in the next
        steps.
      </div>
    </section>
  );
}

// MOVIE LIST PAGE //
