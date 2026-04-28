# Frontend

This is the React + Vite web app for ImageFlow.

## What It Covers

- logged-out SaaS landing page
- pricing and get started flow
- authenticated workspace dashboard
- upload, crop, optimize, and review flow
- batch history and result review

## Main Files

- `src/App.jsx`
  App shell, auth state, upload flow, polling, landing/workspace split
- `src/components/LandingPage.jsx`
  Marketing-facing entry experience
- `src/components/WorkspaceDashboard.jsx`
  Logged-in SaaS-style workspace home
- `src/components/OptimizationForm.jsx`
  Upload and optimization controls
- `src/components/ResultPanel.jsx`
  Job review and download experience

## Run

```powershell
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

Default backend target:

```text
http://localhost:8080
```

You can override it with:

```text
VITE_API_BASE_URL
```

## Build

```powershell
npm run build
```

## Related Docs

- [README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/README.md:1)
- [docs/product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
