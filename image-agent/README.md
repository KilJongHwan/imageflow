# Image Agent

This is the Python worker used for async image processing in ImageFlow.

## Responsibilities

1. read image jobs from Redis
2. mark a job as `PROCESSING`
3. load the source image
4. apply crop, resize, quality, and optional watermark rules
5. store the optimized result
6. report `SUCCEEDED` or `FAILED` back to the backend

## Important Runtime Behavior

- if a job contains a `localhost` file URL, the worker rewrites it using `BACKEND_BASE_URL`
- local output storage can be shared with the host through `STORAGE_ROOT`
- if object storage credentials are missing, the worker falls back to local output storage

## Main Environment Variables

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

## Run Directly

```powershell
cd image-agent
pip install -r requirements.txt
python worker.py
```

## Run With Docker Compose

```powershell
docker compose up -d redis image-agent
```

If the worker code changed:

```powershell
docker compose build image-agent
docker compose up -d image-agent
```

## Related Docs

- [README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/README.md:1)
- [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
