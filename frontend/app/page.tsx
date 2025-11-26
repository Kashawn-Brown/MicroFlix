import Link from "next/link";

export default function HomePage() {
  return (
    <section className="flex flex-col gap-6">
      <div className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">
          Welcome to MicroFlix
        </h1>
        <p className="text-sm text-slate-300">
          Sign in to rate movies and build your watchlist, or browse the catalog
          first.
        </p>
      </div>

      <div className="flex flex-wrap gap-4">
        <Link
          href="/login"
          className="rounded-md bg-sky-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400"
        >
          Sign in
        </Link>
        <Link
          href="/register"
          className="rounded-md border border-slate-600 px-4 py-2 text-sm font-medium hover:border-sky-400 hover:text-sky-300"
        >
          Create an account
        </Link>
        <Link
          href="/movies"
          className="text-sm text-sky-300 hover:text-sky-200"
        >
          Browse movies
        </Link>
      </div>
    </section>
  );
}

// HOME PAGE //

// RootLayout wraps the whole page with:
    // Header (MicroFlix + navigation),
    // Main area, etc.
// This HomePage component is what gets passed in as {children} when you go to the home route /.

// So the full page is:
    // Top: header from RootLayout
    // Middle: this HomePage content inside <main>
