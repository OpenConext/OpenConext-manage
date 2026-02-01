import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    define: {
        'process.env': {}
    },
    plugins: [react()],
    build: {
        chunkSizeWarningLimit: 1000
    },
    server: {
        port: 3006,
        open: true,
        proxy: {
            '/manage/api/client': {
                target: 'http://localhost:8081',
                changeOrigin: true,
                secure: false,
            }
        }

    },
    css: {
        preprocessorOptions: {
            scss: {
                // ToDo fix?
                silenceDeprecations: ["mixed-decls", "global-builtin", "color-functions"],
            },
        },
    },
})
