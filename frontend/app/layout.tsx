import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "MicroFlix",
  description: "MicroFlix movie platform frontend",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <div className="min-h-screen flex flex-col">
          <header className="border-b border-slate-800">
            <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
              <div className="text-lg font-semibold tracking-tight">
                MicroFlix
              </div>
              <div className="flex items-center gap-4 text-sm">
                {/* Public navigation for now – we'll gate some of this later with auth */}
                <Link href="/movies" className="hover:text-sky-400">
                  Movies
                </Link>
                <Link href="/watchlist" className="hover:text-sky-400">
                  Watchlist
                </Link>
                <Link href="/login" className="hover:text-sky-400">
                  Login
                </Link>
              </div>
            </nav>
          </header>

          <main className="mx-auto flex w-full max-w-5xl flex-1 px-4 py-6">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
