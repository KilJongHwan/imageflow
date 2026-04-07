# Commerce Seller Roadmap

## Positioning

ImageFlow should be built as a seller image operations service, not just an image optimizer.

The best-fit users are:

- marketplace sellers
- commerce operators
- MD teams
- small brand owners
- non-designer staff who still need to prepare many product images

The product should help them prepare clean, lightweight, channel-ready product assets faster than doing the same work manually in a design tool.

## Target Problem

Commerce teams repeatedly do the same work:

- crop the important product area
- fit images to a required ratio
- resize for thumbnails or listings
- compress for upload speed
- add watermark or brand text
- repeat the same settings across many product images

That repetitive work is where ImageFlow should win.

## Core Promise

`Upload once, crop once, export marketplace-ready images in minutes.`

## Primary Users

### 1. Marketplace Seller

- runs a small store on Naver Smart Store, Coupang, Kurly, or similar channels
- needs fast product image cleanup
- cares about upload speed and basic visual consistency

### 2. Commerce Operator / MD

- handles many product pages
- updates thumbnails and listing assets often
- needs repeatable output rules more than advanced design freedom

### 3. Marketing or Content Assistant

- is not a designer
- still has to crop, resize, watermark, and export images correctly

## MVP Scope

The first real service MVP should focus on one sharp flow:

1. sign in
2. upload one or more product images
3. choose a marketplace preset
4. manually crop the visible product area in the UI
5. apply resize / quality / watermark rules
6. preview the result
7. download the final image or ZIP package
8. review recent jobs later

## Marketplace-Oriented Feature Set

### A. Upload And Crop

- authenticated upload
- drag-based crop selection
- center crop fallback
- original / square / portrait / wide ratios

### B. Export Rules

- width / height presets
- quality presets
- watermark toggle
- file naming rules

### C. Marketplace Presets

Examples to support later:

- Naver thumbnail preset
- Coupang product image preset
- Kurly catalog preset
- custom team preset

This does not require hardcoding official platform rules yet.
The product can start with user-friendly preset names and editable defaults.

### D. Result Delivery

- optimized image preview
- before / after comparison
- direct download
- ZIP download for multi-image jobs

### E. Work History

- recent jobs
- status tracking
- re-run with same settings
- failed job visibility

## What Makes It A Real Service

The service stops feeling like a prototype when these are true:

- the first screen clearly speaks to seller pain
- the crop UI is central, not hidden behind form fields
- presets reduce repeated setup work
- job history gives operational confidence
- batch processing saves real time
- export flow fits commerce tasks, not just demos

## Current Gap Analysis

### Already Present

- JWT login
- upload and optimization pipeline
- worker-ready architecture
- basic crop and watermark handling
- direct crop selection UI foundation

### Still Missing

- batch upload
- marketplace presets
- dashboard and recent jobs UX
- before / after compare
- ZIP export
- retry flow
- storage strategy for real operations
- rate limiting and usage controls
- service-quality error handling and monitoring

## Recommended Build Order

### Phase 1. Seller MVP

1. redesign landing and dashboard for seller workflow
2. add marketplace preset model
3. add batch upload
4. improve crop UX
5. add result download and recent job list

### Phase 2. Operational Value

1. add preset save / reuse
2. add batch ZIP export
3. add before / after compare
4. add failed job retry

### Phase 3. Service Hardening

1. move storage fully to R2 or S3-compatible storage
2. separate sync demo mode from real async mode
3. add worker retry and failure tracking
4. add usage limits and billing-ready structure

## Suggested Backend Additions

- `MarketplacePreset` entity or config-backed preset catalog
- batch job entity or grouped upload concept
- download package generation
- richer job status model
- user job history query optimization

## Suggested Frontend Additions

- seller-oriented landing copy
- dashboard with recent processed assets
- upload queue panel
- crop workspace with stronger controls
- preset selection drawer
- result compare and download panel

## Product North Star

If a seller with 30 product images can prepare channel-ready assets much faster than before, the service is working.

That is the standard the next implementation steps should optimize for.
