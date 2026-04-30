# Product Overview

## One-Line Summary

ImageFlow is a SaaS-style workspace for sellers and commerce operators who need to prepare lighter, consistent, marketplace-ready product images faster.

## Who It Is For

- marketplace sellers
- commerce operators and MD teams
- small brand owners
- non-designers who still need to prepare many product assets

## Core Problem

Commerce image preparation is repetitive and expensive.

Teams repeatedly:

- upload oversized files
- crop the same kind of product photos again and again
- resize inconsistently across channels
- manually export multiple images one by one

That increases:

- upload and delivery traffic
- storage usage
- repetitive manual work
- inconsistency across product listings

## Current Product Scope

### Marketing and entry flow

- SaaS landing page
- pricing section
- get started CTA
- separate logged-out and logged-in experiences

### Workspace flow

- email verification-based signup
- JWT login after verification
- Google / Naver social sign-in entry buttons
- dashboard-style workspace home
- single image upload
- multi-image upload
- ZIP upload
- manual crop
- center crop and fit modes
- quality and resize rules
- optional watermark application
- result preview and history
- ZIP download for batch output

### Operations and backend flow

- Spring Boot backend
- PostgreSQL persistence
- Redis-backed async queue
- Python image worker
- optional SMTP-backed verification email sending
- master account bootstrap through environment variables
- local storage with R2/S3 migration path
- health status with queue and storage snapshot

## What Makes It More Than A Demo Tool

- it reports `savedBytes` and `reductionRate`, not only success/failure
- it supports batch workflows instead of single-file happy paths only
- it separates API handling from image processing with a worker-ready structure
- it includes rate limit, retry, and ZIP safety constraints
- it presents a product-like landing and workspace split instead of a single utility screen

## Current Guardrails

- batch limit: `10 files`
- ZIP file safeguards:
  - `32 entries max`
  - `20 MB max per entry`
  - `50 MB max extracted total`
- upload rate limit: `30 requests per minute`
- queue publish retry: `3 attempts`

## Current Technical Shape

```text
Frontend
  React + Vite + Ant Design

Backend
  Spring Boot + JPA + PostgreSQL + JWT

Async processing
  Redis + Python worker

Storage
  Local storage today
  R2/S3-compatible path prepared
```

## Known Gaps

- full object storage migration is not finished
- billing is not fully integrated
- monitoring is still basic
- a dedicated jobs screen and deeper filters would improve operations UX
- frontend bundle size still has optimization room

## Recommended Reading

- [deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
- [portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
