# Image Agent

This worker consumes image jobs from Redis, optimizes the image, uploads it to Cloudflare R2, and reports the result back to the backend.

## Flow

1. read job payload from Redis list
2. mark backend job as `PROCESSING`
3. download source image
4. resize and optimize image
5. upload optimized file to R2
6. mark backend job as `SUCCEEDED` or `FAILED`

## Environment Variables

- `REDIS_HOST`
- `REDIS_PORT`
- `IMAGE_JOB_QUEUE_KEY`
- `BACKEND_BASE_URL`
- `R2_ENDPOINT`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`
- `R2_PUBLIC_BASE_URL`

## Run

```powershell
cd image-agent
pip install -r requirements.txt
python worker.py
```

If R2 credentials are missing, upload is simulated and the worker still returns a public-looking URL using `R2_PUBLIC_BASE_URL`.
