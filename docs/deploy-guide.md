# Deploy Guide

## Purpose

This guide explains how to run ImageFlow locally and how to think about demo deployment versus async worker deployment.

## Local Development

### Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Default backend URL:

```text
http://localhost:8080
```

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

### Async worker stack

```powershell
docker compose up -d redis image-agent
```

If worker code changed, rebuild it:

```powershell
docker compose build image-agent
docker compose up -d image-agent
```

## Important Runtime Note

When the backend runs on the host and the worker runs in Docker:

- the worker must use `BACKEND_BASE_URL=http://host.docker.internal:8080`
- the worker must share the local `storage` directory through Docker volume mapping

This is already reflected in `docker-compose.yml`.

## Recommended Environment Values

### Backend

- `APP_PUBLIC_BASE_URL`
- `APP_PROCESSING_MODE`
- `APP_QUEUE_ENABLED`
- `APP_STORAGE_PROVIDER`
- `APP_STORAGE_ROOT`
- `APP_RATE_LIMIT_UPLOAD_REQUESTS_PER_MINUTE`
- `JWT_SECRET`

### Worker

- `REDIS_HOST`
- `REDIS_PORT`
- `IMAGE_JOB_QUEUE_KEY`
- `BACKEND_BASE_URL`
- `STORAGE_ROOT`
- `R2_ENDPOINT`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`
- `R2_PUBLIC_BASE_URL`

## Demo Deployment Recommendation

### Low-cost portfolio demo

- frontend: Vercel
- backend: Render
- database: Render Postgres
- processing mode: sync or simplified mode when worker cost is not worth it

### More realistic async demo

- frontend hosted separately
- backend + Redis + worker deployed together
- object storage connected for result delivery

## Post-Deploy Checklist

1. sign up and login
2. upload one image
3. verify optimized result is generated
4. verify `savedBytes` and `reductionRate` are shown
5. upload a small batch
6. download ZIP output
7. verify `/api/health` reflects expected queue and storage mode

## Current Caveats

- local storage is still part of the default setup
- object storage migration is not fully complete
- Render free tier may sleep and slow the first request
- frontend bundle size still has room for splitting

## Related Docs

- [product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
- [portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
