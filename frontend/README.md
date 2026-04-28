# Vite Auth Client

This frontend includes:

- `Vite + React + TypeScript`
- `Tailwind CSS` with a minimal `shadcn/ui` setup
- `signup` and `signin` pages
- Form validation with `Zod + react-hook-form`
- Auth state with `Zustand`
- Client-side `Protected Route`
- `Axios interceptor` that auto-attaches the access token
- A complete `refresh access token` flow against the Spring Boot API

## Run locally

1. Copy `.env.example` to `.env`
2. Install dependencies:

```bash
npm install
```

3. Start the app:

```bash
npm run dev
```

The frontend runs on `http://localhost:5173` by default.
