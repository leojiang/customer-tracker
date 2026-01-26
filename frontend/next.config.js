/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  // Optional: Change the output directory `out` to something else
  // distDir: 'dist',
  // Disable server-based image optimization
  images: {
    unoptimized: true,
  },
}

module.exports = nextConfig
