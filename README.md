# ImageFlow

ImageFlow is a SaaS-style workspace for commerce image optimization. It helps sellers and operators upload product images, apply repeatable output rules, review size savings, and download channel-ready assets in a single flow.

## What It Solves

Commerce teams repeatedly spend time and cost on:

- uploading oversized product images
- manually cropping and resizing the same assets
- applying inconsistent export rules
- preparing batches one file at a time

ImageFlow turns that into a structured workflow:

1. sign in
2. upload one or more product images
3. choose a marketplace-oriented preset
4. crop and tune output rules
5. review optimized results and savings
6. download a single file or ZIP package

## Product Overview

### User-facing experience

- SaaS-style landing page with `Get Started` and `Pricing`
- authenticated workspace dashboard
- single image, multi-image, and ZIP upload
- optional watermark application
- result preview, savings metrics, and ZIP download
- recent job history and workspace status

### System-facing capabilities

- Spring Boot API for auth, uploads, jobs, and downloads
- PostgreSQL for users, jobs, and usage data
- Redis queue and Python worker for async image processing
- local storage today, object storage migration path for R2/S3
- basic operational safeguards such as retry, rate limit, and health status

## Key Features

- JWT sign up and login
- landing page and pricing flow
- dashboard-style workspace after login
- marketplace preset strip for common commerce outputs
- manual crop, center crop, and fit modes
- quality and resize controls
- optional text or image watermark
- batch upload and ZIP upload
- batch ZIP download
- result metrics:
  - original size
  - result size
  - saved bytes
  - reduction rate
- health endpoint with queue and storage snapshot

## Architecture

```text
Frontend (React + Vite)
  -> Backend API (Spring Boot)
    -> PostgreSQL
    -> Local storage / object storage path
    -> Redis queue
      -> Python image worker
```

## Production-like Hardening Already Applied

- batch upload limit: `10 files`
- ZIP extraction safeguards:
  - `32 entries max`
  - `20 MB max per entry`
  - `50 MB max total extracted size`
- upload rate limit: `30 requests per minute`
- queue publish retry: `3 attempts`
- ZIP download streaming via `StreamingResponseBody + ZipOutputStream`

These are documented in code and surfaced in the portfolio docs as evidence of optimization and risk control.

## Repository Structure

```text
backend/        Spring Boot API
frontend/       React + Vite web app
image-agent/    Python worker for async image processing
docs/           product, deployment, and portfolio documentation
PORTFOLIO.md    recruiter-facing summary with code-backed impact
docker-compose.yml
```

## Local Run

### 1. Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Backend default:

```text
http://localhost:8080
```

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend default:

```text
http://localhost:5173
```

### 3. Async worker stack

```powershell
docker compose up -d redis image-agent
```

If you update the worker code, rebuild it:

```powershell
docker compose build image-agent
docker compose up -d image-agent
```

## Recommended Demo Modes

### Simple local demo

- backend on host
- frontend on host
- local storage enabled
- optional Docker worker for async mode

### Portfolio / free deploy demo

- frontend on Vercel
- backend on Render
- Postgres on Render
- worker disabled or simplified sync mode when cost matters

See [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1).

## Documentation Guide

- [docs/README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/README.md:1)
  Overall documentation map
- [docs/product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
  Product positioning, users, and current scope
- [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
  Local/demo deployment notes and checklist
- [docs/portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
  Recruiter-facing case study
- [PORTFOLIO.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/PORTFOLIO.md:1)
  Shorter screening-oriented portfolio summary

## Current Limits

- full R2/S3 migration is not complete yet
- billing is presented as product direction, not full payment integration
- monitoring is basic and not a full observability stack
- frontend bundle size still has room for code splitting

## Why This Project Reads Well In Screening

- it solves a concrete commerce operations problem
- it shows both product thinking and implementation depth
- it includes measurable optimization outputs, not only UI work
- it goes beyond CRUD by covering uploads, async processing, and operational safeguards
