# 배포 가이드

## 목적

이 문서는 ImageFlow를 로컬에서 실행하는 방법과 데모 배포 및 비동기 워커 배포를 어떻게 생각해야 하는지를 설명합니다.

## 로컬 개발

현재 `docker-compose.yml`은 전체 애플리케이션 스택이 아니라 로컬 개발에 필요한 인프라와 워커만 지원합니다.

- `postgres`
- `redis`
- `image-agent`

Spring 백엔드와 React 프론트엔드는 로컬 개발 시 호스트 머신에서 직접 실행합니다.

### 도커 인프라

```bash
docker compose up -d postgres redis image-agent
```

종료:

```bash
docker compose down
```

### 백엔드 실행

```bash
cd backend
.\gradlew.bat bootRun
```

기본 백엔드 URL:

```text
http://localhost:8080
```

### 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

기본 프론트엔드 URL:

```text
http://localhost:5173
```

### 워커 코드 변경 시 재빌드

```bash
docker compose build image-agent
docker compose up -d image-agent
```

## 중요 런타임 안내

백엔드가 호스트에서 실행되고 워커가 도커에서 실행되는 경우:

- 워커는 `BACKEND_BASE_URL=http://host.docker.internal:8080`을 사용해야 합니다.
- 워커는 로컬 `storage` 디렉터리를 도커 볼륨으로 공유해야 합니다.

이 설정은 `docker-compose.yml`에 반영되어 있습니다.

## API 문서

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/api-docs`

배포 환경에서도 위와 동일한 경로를 백엔드 주소 뒤에 붙여서 접근할 수 있습니다.

## 부하 테스트

동시성 업로드 테스트는 [load-tests/README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/load-tests/README.md:1)와
[load-tests/concurrent_upload_test.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/load-tests/concurrent_upload_test.py:1)를 참고하여 실행할 수 있습니다.

## 권장 환경 변수

### 백엔드

- `APP_PUBLIC_BASE_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_PROCESSING_MODE`
- `APP_QUEUE_ENABLED`
- `APP_STORAGE_PROVIDER`
- `APP_STORAGE_ROOT`
- `APP_RATE_LIMIT_UPLOAD_REQUESTS_PER_MINUTE`
- `JWT_SECRET`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

배포 프론트가 Vercel인 경우 `APP_CORS_ALLOWED_ORIGINS`에 아래 값을 포함해야 합니다.

```text
https://imageflow-rose.vercel.app
```

여러 origin을 허용하려면 쉼표로 구분합니다.

```text
https://imageflow-rose.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

### 워커

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

## 데모 배포 추천

### 저비용 포트폴리오 데모

- 프론트엔드: Vercel
- 백엔드: Render
- 데이터베이스: Render Postgres
- 큐: Render Key Value (무료)
- 워커: 로컬 머신
- 처리 모드: worker
- 결과 파일은 워커 콜백 엔드포인트를 통해 백엔드로 다시 업로드합니다.

### 좀 더 현실적인 비동기 데모

- 프론트엔드를 별도 호스팅
- 백엔드 + Redis + 워커를 함께 배포
- Render 무료 플랜은 백그라운드 워커를 지원하지 않으므로 워커에는 유료 인스턴스가 필요합니다.
- 공유 스토리지가 없는 경우 워커는 최적화 결과를 백엔드 콜백 엔드포인트로 다시 업로드합니다.

## Render 블루프린트 노트

- `render.yaml`
  - 포트폴리오용 `backend + free key value` 구성
  - `APP_PROCESSING_MODE=worker`
  - `APP_QUEUE_ENABLED=true`
  - 워커는 로컬에서 Render Key Value를 소비합니다.
- `render.async.yaml`
  - `Render Key Value + background worker` 구성 예시
  - 워커는 `starter` 플랜으로 분리합니다.
  - 백엔드는 `REDIS_URL`로 Key Value에 연결합니다.
  - 워커는 `BACKEND_HOSTPORT`를 사용해 프라이빗 네트워크로 백엔드 콜백을 호출합니다.

## Render 데모용 로컬 워커

1. Render 백엔드와 Key Value를 배포합니다.
2. Render 백엔드 환경 변수에 아래 값이 포함되어 있는지 확인합니다.
   - `APP_QUEUE_ENABLED=true`
   - `APP_PROCESSING_MODE=worker`
   - `REDIS_URL=<Render Key Value connection string>`
   - `APP_CORS_ALLOWED_ORIGINS=https://imageflow-rose.vercel.app,http://localhost:5173,http://127.0.0.1:5173`
3. 로컬에서 [image-agent/.env.render-local-worker.example](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/.env.render-local-worker.example:1)를 [image-agent/.env.render-local-worker](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/.env.render-local-worker.example:1)로 복사하고 값을 채웁니다.
4. [image-agent/run-render-local-worker.ps1](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/run-render-local-worker.ps1:1)로 워커를 실행합니다.

이 구성은 포트폴리오 데모에는 적합하지만 상시 운영 구조로 보기는 어렵습니다.

## 배포 후 체크리스트

1. 회원가입 및 로그인
2. 이미지 한 장 업로드
3. 최적화 결과가 생성되는지 확인
4. `savedBytes`와 `reductionRate`가 표시되는지 확인
5. 작은 배치 업로드
6. ZIP 출력 다운로드
7. `/api/health`가 예상한 큐 및 스토리지 모드를 반영하는지 확인

## 현재 유의 사항

- 기본 설정에서는 여전히 로컬 스토리지가 사용됩니다.
- 객체 스토리지 마이그레이션이 아직 완전히 완료되지 않았습니다.
- Render 무료 플랜은 첫 요청에서 슬립 모드로 인해 느려질 수 있습니다.
- 프론트엔드 번들 크기를 더 분리할 여지가 있습니다.

## 관련 문서

- [product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
- [portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
