// Tyoe of data to expect in URL
type MovieDetailsPageProps = {
  params: {
    id: string;
  };
};

// argument is MovieDetailsPageProps (defined above)
export default function MovieDetailsPage({ params }: MovieDetailsPageProps) {
  const { id } = params; // destructuring

  return (
    <section className="flex w-full flex-col gap-4">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">
          Movie details (#{id})
        </h1>
        <p className="text-sm text-slate-300">
          This page will show movie metadata, rating summary, your rating, and
          watchlist toggle.
        </p>
      </header>

      {/* TODO: fetch movie details + rating summary + user rating + watchlist state */}
      <div className="rounded-md border border-dashed border-slate-700 p-4 text-sm text-slate-400">
        Movie details content will be loaded here from the backend.
      </div>
    </section>
  );
}

// MOVIE INFO PAGE //
