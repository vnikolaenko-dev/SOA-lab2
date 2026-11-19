import { defineConfig } from "orval";

const api = (name: string, target: string) => ({
  input: { target: target },
  output: {
    target: `./src/api/generated/${name}/client.ts`,
    schemas: {
      path: `./src/api/generated/${name}/models`,
      type: "zod" as const,
    },
    client: "react-query" as const,
    httpClient: "fetch" as const,
    override: {
      fetch: { runtimeValidation: true },
    },
  },
});

export default defineConfig({
  spaceMarines: api("space-marines", "https://localhost:8080/collection-service/v3/api-docs"),
  starships: api("starships", "https://localhost:8080/starship-service/v3/api-docs"),
});
