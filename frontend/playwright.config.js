import { defineConfig, devices } from "@playwright/test";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const backendStoragePath = path.resolve(__dirname, "test-results", "ui-db.json");
const backendPort = 8081;
const frontendPort = 4173;

export default defineConfig({
  testDir: "./tests/ui",
  workers: 1,
  fullyParallel: false,
  timeout: 45_000,
  expect: {
    timeout: 8_000
  },
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: `http://127.0.0.1:${frontendPort}`,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "off",
    channel: "chrome"
  },
  projects: [
    {
      name: "chrome",
      use: {
        ...devices["Desktop Chrome"]
      }
    }
  ],
  webServer: [
    {
      command: `cmd /c mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=${backendPort}`,
      cwd: path.resolve(__dirname, "../backend-java"),
      url: `http://127.0.0.1:${backendPort}/api/health`,
      reuseExistingServer: false,
      timeout: 120_000,
      env: {
        ...process.env,
        RECIPE_BOOK_STORAGE_PATH: backendStoragePath
      }
    },
    {
      command: `cmd /c npm.cmd run dev -- --host 127.0.0.1 --port ${frontendPort}`,
      cwd: __dirname,
      url: `http://127.0.0.1:${frontendPort}`,
      reuseExistingServer: false,
      timeout: 120_000,
      env: {
        ...process.env,
        VITE_API_URL: "/api",
        VITE_PORT: String(frontendPort),
        VITE_PROXY_TARGET: `http://127.0.0.1:${backendPort}`
      }
    }
  ]
});
