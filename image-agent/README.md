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
- 분리 배포 환경에서는 `WORKER_RESULT_CALLBACK_ENABLED=true` 로 두고, 최적화 결과 파일을 backend callback endpoint로 다시 업로드할 수 있습니다.

## Main Environment Variables

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_URL`
- `IMAGE_JOB_QUEUE_KEY`
- `BACKEND_BASE_URL`
- `BACKEND_HOSTPORT`
- `WORKER_RESULT_CALLBACK_ENABLED`
- `STORAGE_ROOT`
- `R2_ENDPOINT`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`
- `R2_PUBLIC_BASE_URL`

## Run Directly

```bash
cd image-agent
pip install -r requirements.txt
python worker.py
```

### Local worker against deployed Render queue

1. [image-agent/.env.render-local-worker.example](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/.env.render-local-worker.example:1) 를 기준으로 값을 복사합니다.
2. `REDIS_URL` 을 Render Key Value connection string으로 교체합니다.
3. `BACKEND_BASE_URL` 을 현재 Render backend URL로 맞춥니다.
4. 아래처럼 실행합니다.

```powershell
Copy-Item .env.render-local-worker.example .env.render-local-worker
# 값 수정 후
.\run-render-local-worker.ps1
```

이 모드에서는 worker가 queue를 로컬에서 소비하고, 결과 파일은 backend callback endpoint로 다시 업로드합니다.

## Run With Docker Compose

```bash
docker compose up -d redis image-agent
```

If the worker code changed:

```bash
docker compose build image-agent
docker compose up -d image-agent
```

## Related Docs

- [README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/README.md:1)
- [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
