import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        instaPurple: "#833AB4",
        instaRed: "#FD1D1D",
        instaOrange: "#F56040",
      },
      backgroundImage: {
        "insta-gradient": "linear-gradient(to right, #833AB4, #FD1D1D, #F56040)",
      },
    },
  },
  plugins: [],
};
export default config;
