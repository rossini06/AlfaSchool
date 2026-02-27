import defaultTheme from "tailwindcss/defaultTheme";
import forms from "@tailwindcss/forms";

/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./vendor/laravel/framework/src/Illuminate/Pagination/resources/views/*.blade.php",
        "./storage/framework/views/*.php",
        "./resources/views/**/*.blade.php",
    ],

    theme: {
        extend: {
            fontFamily: {
                sans: ["Figtree", ...defaultTheme.fontFamily.sans],
            },
            colors: {
                indigo: {
                    50: "#e7fbfd",
                    100: "#cff8fb",
                    200: "#b0f3f8",
                    300: "#7deffb",
                    400: "#44d9e8",
                    500: "#049cac",
                    600: "#038795",
                    700: "#046c84",
                    800: "#035b70",
                    900: "#024a5c",
                },
                blue: {
                    50: "#e7fbfd",
                    100: "#cff8fb",
                    200: "#b0f3f8",
                    300: "#7deffb",
                    400: "#44d9e8",
                    500: "#049cac",
                    600: "#038795",
                    700: "#046c84",
                    800: "#035b70",
                    900: "#024a5c",
                },
            },
        },
    },

    plugins: [forms],
};
