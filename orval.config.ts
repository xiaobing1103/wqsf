import { defineConfig } from "orval";

const input = "./openapi/万企商服通_OpenAPI_V1.json";

const common = {
  input: { target: input },
  output: {
    mode: "split",
    client: "fetch",
    schemas: "./models",
    override: {
      fetch: { includeHttpResponseReturnType: false, forceSuccessResponse: true },
    },
  },
};

export default defineConfig({
  admin: {
    ...common,
    output: {
      ...common.output,
      target: "./apps/admin-web/src/api/generated/endpoints.ts",
      schemas: "./apps/admin-web/src/api/generated/models",
      override: {
        ...common.output.override,
        mutator: {
          path: "./apps/admin-web/src/api/generated-client.ts",
          name: "adminGeneratedRequest",
        },
      },
    },
  },
  client: {
    ...common,
    output: {
      ...common.output,
      target: "./apps/client-uni/src/api/generated/endpoints.ts",
      schemas: "./apps/client-uni/src/api/generated/models",
      override: {
        ...common.output.override,
        mutator: {
          path: "./apps/client-uni/src/api/generated-client.ts",
          name: "uniGeneratedRequest",
        },
      },
    },
  },
});
