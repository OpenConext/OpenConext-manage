import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    resolve: {
        alias: [
            // JS: force browser-safe build
            {
                find: /^jsondiffpatch$/,
                replacement: 'jsondiffpatch/dist/jsondiffpatch.umd.js',
            },

            // CSS: keep original path working
            {
                find: /^jsondiffpatch\/dist\/formatters-styles\/(.*)$/,
                replacement: 'jsondiffpatch/dist/formatters-styles/$1',
            },
        ],
    },
    plugins: [react()],
    build: {
        chunkSizeWarningLimit: 1000
    },
    server: {
        port: 3006,
        open: true,
        strictPort: true,
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
                silenceDeprecations: ["global-builtin", "color-functions"],
            },
        },
    },
})
