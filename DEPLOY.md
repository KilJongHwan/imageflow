# Deploy

This project is prepared for demo-style free deployment with:

- frontend: Vercel
- backend: Render (Docker runtime)
- database: Render Postgres

## Important Note

This setup is designed for portfolio/demo usage, not always-on production.

- backend processing runs in `sync` mode
- Redis and the Python worker are not required for deployment
- uploaded/generated files are stored in temporary disk on the backend host
- files may disappear after redeploy or instance restart

## 1. Push The Latest Code

Push the repository to GitHub first.

## 2. Deploy Backend On Render

Render can read the included [render.yaml](/c:/Users/tsline/IdeaProjects/imageflow/render.yaml#L1).

### Option A: Blueprint

1. Log in to Render
2. Choose `New +`
3. Choose `Blueprint`
4. Select this GitHub repository
5. Render will detect `render.yaml`

This backend now deploys via `runtime: docker`, not `runtime: java`.

### Required environment variable

After service creation, set:

- `APP_PUBLIC_BASE_URL`
  - example: `https://imageflow-backend.onrender.com`
- `APP_AUTH_JWT_SECRET`
  - use a real secret value with at least 32 characters

## 3. Deploy Frontend On Vercel

1. Log in to Vercel
2. Import the same GitHub repository
3. Set `Root Directory` to `frontend`
4. Set the environment variable:

```text
VITE_API_BASE_URL=https://your-render-backend-url.onrender.com
```

5. Deploy

## 4. Verify

After both deploys:

1. Open the Vercel frontend URL
2. Upload a small image first
3. Confirm the optimized result appears

## 5. Known Demo Limits

- first backend request may be slow because Render free services sleep
- uploaded/output files are temporary
- no Redis worker is used in deploy mode
- very large files may fail depending on platform limits

## 6. Practical Checklist

For a short Korean checklist version, see:

- [docs/deploy-checklist.md](/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-checklist.md)
