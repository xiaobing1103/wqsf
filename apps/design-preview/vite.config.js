import { resolve } from 'node:path'
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        preview: resolve(__dirname, 'index.html'),
        document: resolve(__dirname, 'document.html')
      }
    }
  }
})
