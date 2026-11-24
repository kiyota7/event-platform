import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// ポートは固定(5173)。競合しても別ポートに逃がさず失敗させる
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
