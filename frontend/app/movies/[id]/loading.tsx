export default function LoadingMovieDetails() {
  return (
    <section className="flex w-full flex-col gap-4 md:flex-row">
      {/* Poster skeleton */}
      <div className="w-full max-w-xs">
        <div className="aspect-[2/3] rounded-lg bg-slate-800 animate-pulse" />
      </div>

      {/* Text + panels skeleton */}
      <div className="mt-4 flex flex-1 flex-col gap-4 md:mt-0 md:ml-6">
        <div className="space-y-2">
          <div className="h-7 w-2/3 rounded bg-slate-800 animate-pulse" />
          <div className="h-4 w-1/4 rounded bg-slate-800 animate-pulse" />
          <div className="h-3 w-1/2 rounded bg-slate-800 animate-pulse" />
        </div>

        <div className="space-y-2">
          <div className="h-4 w-20 rounded bg-slate-800 animate-pulse" />
          <div className="h-3 w-full rounded bg-slate-800 animate-pulse" />
          <div className="h-3 w-5/6 rounded bg-slate-800 animate-pulse" />
          <div className="h-3 w-4/6 rounded bg-slate-800 animate-pulse" />
        </div>

        <div className="mt-2 space-y-2">
          <div className="h-4 w-24 rounded bg-slate-800 animate-pulse" />
          <div className="h-20 rounded bg-slate-900/60 border border-slate-800 animate-pulse" />
        </div>
      </div>
    </section>
  );
}
