import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    include: ['JSCPP'],
  },
  server: {
    host: '127.0.0.1',
    port: 43124,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8090',
    },
  },
})
