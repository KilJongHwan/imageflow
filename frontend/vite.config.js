import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  },
  build: {
    target: "esnext",
    rollupOptions: {
      output: {
        manualChunks: {
          antd: ["antd"],
          icons: ["@ant-design/icons"],
          react: ["react", "react-dom"]
        }
      }
    }
  }
});
