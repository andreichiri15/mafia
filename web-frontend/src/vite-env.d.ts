/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of the backend (no trailing slash). Empty string means use relative paths (Vite proxy in dev). */
  readonly VITE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
