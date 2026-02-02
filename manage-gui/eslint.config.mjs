import js from "@eslint/js";
import globals from "globals";
import pluginReact from "eslint-plugin-react";
import reactHooks from 'eslint-plugin-react-hooks';
import { defineConfig } from "eslint/config";

export default defineConfig([
    {
        ignores: ["dist/**"],
    },
    {
        files: ["**/*.{js,mjs,cjs,jsx}"],
        plugins: {
            js ,
            'react-hooks': reactHooks
        },
        extends: ["js/recommended"],
        languageOptions: { globals: globals.browser },
        settings: {
            react: {
                version: "detect",
            },
        },
    },
    pluginReact.configs.flat.recommended, // React config first
    reactHooks.configs.flat.recommended,
    {
        rules: {
            "react/prop-types": "off",
            "react/no-children-prop": "off",
            'react-hooks/exhaustive-deps': 'warn',
            "react/react-in-jsx-scope" : "off",
            "react-hooks/set-state-in-effect": "warn",
            "react-hooks/immutability":"off",
            "react/no-deprecated": "off",          // ReactDOM.unmountComponentAtNode / andere deprecated APIs
            "react/no-unsafe": "off",              // componentWillReceiveProps, componentWillMount, etc
        },
    },

]);
