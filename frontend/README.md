# Frontend

This frontend now uses React with Vite.

## Files

- `index.html`: Vite entry html
- `src/main.jsx`: React bootstrap
- `src/App.jsx`: main page
- `src/styles.css`: app styles
- `vite.config.js`: Vite config

## What it does

- uploads a single image
- sends an optimization job to the backend
- polls job status
- shows the optimized result image when ready

## Install

```powershell
cd frontend
npm install
```

Recommended runtime:

```text
Node.js 16+
```

## Run

```powershell
npm run dev
```

Then open:

```text
http://localhost:5173
```

Default backend target:

```text
http://localhost:8080
```
