import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Add an empty turbopack config to avoid Turbopack/webpack conflict error
  turbopack: {},
  webpack: (config, { dev }) => {
    if (dev) {
      config.cache = false;
    }
    return config;
  },
};

export default nextConfig;
