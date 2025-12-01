import type { NextConfig } from "next";

const GATEWAY_BASE_URL =
  process.env.GATEWAY_BASE_URL || "http://gateway:8081";

const nextConfig: NextConfig = {
  /* config options here */

  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "image.tmdb.org",
        pathname: "/t/p/**",
      },
    ],
  },

  // Rewrites: everything under /api/<service>/... goes to the gateway
  async rewrites() {
    return [
      {
        // Any request to /gateway/** on the frontend
        source: "/gateway/:path*",
        // is proxied to the real Spring Cloud Gateway
        destination: `${GATEWAY_BASE_URL}/:path*`,
      },
    ];
  },

};


export default nextConfig;
