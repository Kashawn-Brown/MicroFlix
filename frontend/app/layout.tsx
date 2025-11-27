import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import Image from "next/image";
import "./globals.css";
import AuthHeader from "../components/auth-header";

const inter = Inter({ subsets: ["latin"] });

// Metadata for Next.js to use -> automatically uses it to create <head> tags for every page
// Tells Next.js: “Here is all the important info about my site (name, description, how to show it on social media)
export const metadata: Metadata = {
  title: "MicroFlix",
  description:
    "Discover, track, and rate movies with MicroFlix — a simple movie catalog and watchlist app.",
  metadataBase: new URL("http://localhost:3000"), // swap to real URL in prod
  
  // the rules many sites use to show nice previews when you paste a link.
  openGraph: {
    title: "MicroFlix",
    description:
      "Discover, track, and rate movies with MicroFlix — a simple movie catalog and watchlist app.",
    siteName: "MicroFlix",
    type: "website",
  },

  // similar to openGraph, but specifically for Twitter/X
  twitter: {
    card: "summary_large_image",
    title: "MicroFlix",
    description:
      "Discover, track, and rate movies with MicroFlix — a simple movie catalog and watchlist app.",
  },
};


export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-slate-950 text-slate-100 antialiased`} >  {/* Main container for everything on the page */}
        <div className="min-h-screen flex flex-col">
          
          {/* Header and Nav bar */}
          {/* <header className="border-b border-slate-800"> */}
          <header className="border-b border-slate-800 bg-slate-950/90 backdrop-blur">
            <nav className="mx-auto flex max-w-8xl items-center justify-between px-6 py-5">
              <Link href="/" className="flex items-center gap-3">
                <div className="relative h-9 w-9 md:h-10 md:w-10">
                  <Image
                    src="icon.svg"   // <-- update to your actual logo path
                    alt="MicroFlix logo"
                    fill
                    className="rounded-2xl object-cover"
                  />
                </div>
                {/* slate-300  .  bg-slate-900/60  .  bg-slate-800  .  slate-400  .   */}
                <span className="text-2xl md:text-3xl font-extrabold tracking-tight">
                  Micro<span className="text-sky-500">Flix</span>
                </span>
              </Link>

              {/* Nav links */}
              <div className="flex items-center gap-8 text-xl">  {/* headerlinks */}
                {/* Public navigation for now – we'll gate some of this later with auth */}
                <Link href="/movies" className="hover:text-sky-400">
                  Movies
                </Link>
                <Link href="/watchlist" className="hover:text-sky-400">
                  Watchlist
                </Link>
              </div>
              <div>
                <AuthHeader />
              </div>
              
            </nav>
          </header>

          <main className="mx-auto flex w-full max-w-7xl flex-1 px-4 py-6">
            {children}  {/* where the page content goes */}
          </main>
        </div>
      </body>
    </html>
  );
}

// main layout of next.js app

// RootLayout wraps the whole page with:
    // Header (MicroFlix + navigation),
    // Main area, etc.
// This HomePage component is what gets passed in as {children} when you go to the home route /.

// So the full page is:
    // Top: header from RootLayout
    // Middle: this HomePage content inside <main>
