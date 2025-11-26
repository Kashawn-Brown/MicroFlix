import Link from "next/link";

export default function RegisterPage() {
  return (
    <section className="mx-auto flex w-full max-w-md flex-col gap-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">
          Create your account
        </h1>
        <p className="text-sm text-slate-300">
          Sign up to start rating movies and building your watchlist.
        </p>
      </div>

      {/* Will hook this up to /auth/register */}
      <form className="space-y-4">
        <div className="space-y-1">
          <label
            htmlFor="displayName"
            className="block text-sm font-medium text-slate-200"
          >
            Display name
          </label>
          <input
            id="displayName"
            type="text"
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="space-y-1">
          <label
            htmlFor="email"
            className="block text-sm font-medium text-slate-200"
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="space-y-1">
          <label
            htmlFor="password"
            className="block text-sm font-medium text-slate-200"
          >
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            className="mt-1 w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <button
          type="submit"
          className="w-full rounded-md bg-sky-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400"
        >
          Create account
        </button>
      </form>

      <p className="text-xs text-slate-400">
        Already have an account?{" "}
        <Link href="/login" className="text-sky-300 hover:text-sky-200">
          Sign in
        </Link>
        .
      </p>
    </section>
  );
}

// REGISTER PAGE //