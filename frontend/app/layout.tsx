import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import "./globals.css";
import AuthHeader from "../components/auth-header";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "MicroFlix",
  description: "MicroFlix movie platform frontend",
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
          <header className="border-b border-slate-800">
            <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
              <div className="text-lg font-semibold tracking-tight">
                <Link href="/">
                MicroFlix
                </Link>
              </div>
              <div className="flex items-center gap-4 text-sm">  {/* headerlinks */}
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
