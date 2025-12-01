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
        // Match: /api/movie-service/... OR /api/rating-service/... OR /api/user-service/...
        source:
          "/gateway/:service(movie-service|rating-service|user-service)/:path*",
        // Forward to the gateway, keeping the service + path
        destination: `${GATEWAY_BASE_URL}/:service/:path*`,
      },
    ];
  },

};

// could've done this, but above is safer and more restrictive:
// async rewrites() {
//   return [
//     {
//       source: "/gateway/:path*",
//       destination: `${GATEWAY_BASE_URL}/:path*`,
//     },
//   ];
// }


export default nextConfig;
