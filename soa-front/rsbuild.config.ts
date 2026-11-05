import { defineConfig } from "@rsbuild/core";
import { pluginReact } from "@rsbuild/plugin-react";
import { tanstackStart } from "@tanstack/react-start/plugin/rsbuild";

// Docs: https://rsbuild.rs/config/
export default defineConfig({
  plugins: [
    pluginReact({
      reactCompiler: true,
    }),
    tanstackStart(),
  ],
  server: {
    port: 3000,
    host: "0.0.0.0",
  },
});
