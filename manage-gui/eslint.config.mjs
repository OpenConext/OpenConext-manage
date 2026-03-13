import js from "@eslint/js";
import globals from "globals";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import { defineConfig } from "eslint/config";

export default defineConfig([
    {
        ignores: ["dist/**", "node_modules/**"],
    },

    js.configs.recommended,
    react.configs.flat.recommended,
    reactHooks.configs.flat.recommended,

    {
        files: ["**/*.{js,mjs,cjs,jsx}"],

        plugins: {
            react,
            "react-hooks": reactHooks,
        },

        languageOptions: {
            globals: globals.browser,
        },

        settings: {
            react: {
                version: "detect",
            },
        },

        rules: {
            "react/prop-types": "off",
            "react/no-children-prop": "off",
            "react/react-in-jsx-scope": "off",
            "react/no-deprecated": "off",
            "react/no-unsafe": "off",

            "react-hooks/exhaustive-deps": "warn",
            "react-hooks/set-state-in-effect": "warn",
            "react-hooks/immutability": "off",
        },
    },
]);
