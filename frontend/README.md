# CampusLink frontend

Existing Vite + vanilla JavaScript application with cream/deep-green styling. The career page now uploads a real PDF/DOCX to the authenticated Java backend, requests optional job matching and coaching, and renders validated evidence, recommendations and a 30/60/90-day roadmap.

```powershell
npm.cmd install
npm.cmd run dev
```

Open the URL printed by Vite, usually http://localhost:5173. Start the Spring Boot backend on port 8787 in a separate terminal. The Vite `/api` proxy remains unchanged. Accounts and chat messages use the backend database, not a simulated signup flow.

```powershell
npm.cmd run build
npx.cmd playwright install chromium
npm.cmd test
```

Build outputs static assets to `dist/`. A deployed frontend needs an authenticated backend at `/api`; static hosting alone cannot provide CV analysis. Playwright tests use clearly labelled synthetic/intercepted API fixtures. They verify upload/preview, required consent, result rendering, failures, mobile layout and existing navigation.

Never configure AI secrets in frontend files or `VITE_` variables. Set AI_PROVIDER, AI_MODEL, AI_API_KEY and AI_TIMEOUT only in the backend environment. Without provider configuration, the career screen explicitly reports unavailable and disables analysis. The CV input and output are not stored in localStorage; optional history belongs to the signed-in account on the backend.

The career UI and decorative banners do not display memes. Existing chat sticker functionality is preserved. Other pre-existing demo content/gallery preferences remain. See the [project README](../README.md), [API/privacy contract](../docs/CAREER_API.md) and [actual verification](../docs/VERIFICATION.md) for configuration, limits and test results.
