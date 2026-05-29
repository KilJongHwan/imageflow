# ImageFlow

ImageFlow는 커머스 상품 이미지를 정해진 규격으로 빠르게 가공하기 위한 이미지 최적화 워크스페이스입니다.  
여러 장의 이미지를 업로드하고, 크롭·리사이즈·압축·워터마크를 적용한 뒤 결과를 확인하고 내려받는 흐름을 한 곳에 모았습니다.

## 서비스 URL

- Frontend: `https://imageflow-rose.vercel.app`
- Backend: `https://imageflow-backend-yol7.onrender.com`
- API Docs: `https://imageflow-backend-yol7.onrender.com/swagger-ui`
- Health: `https://imageflow-backend-yol7.onrender.com/api/health`

## 배포본에서 먼저 볼 부분

평가자가 실제 배포본을 확인할 때는 아래 흐름으로 보면 됩니다.

1. `https://imageflow-rose.vercel.app`
   - 랜딩, Pricing, Get Started 흐름 확인
2. 회원가입 또는 로그인
   - 별도 인증 화면과 워크스페이스 진입 확인
3. 이미지 업로드 또는 ZIP 업로드
   - 단일/배치 최적화 흐름 확인
4. 결과 화면
   - `savedBytes`, `reductionRate`, 결과 다운로드 확인
5. `https://imageflow-backend-yol7.onrender.com/swagger-ui`
   - API 문서와 엔드포인트 구성 확인
6. `https://imageflow-backend-yol7.onrender.com/api/health`
   - processing mode, queue 상태, outbox 상태 확인

## 대상 사용자

- 마켓플레이스 셀러
- 커머스 운영 담당자와 MD 팀
- 반복적으로 상품 이미지를 다루는 소규모 브랜드 운영자
- 디자인 툴보다 작업 속도와 일관성이 더 중요한 실무 사용자

## 어떤 문제를 해결하나

커머스 환경에서는 다음 작업이 반복됩니다.

- 고용량 상품 이미지 업로드
- 비슷한 이미지를 계속 수동 크롭/리사이즈
- 채널마다 다른 출력 규격 적용
- 여러 장 이미지를 한 장씩 따로 가공

이 과정은 다음 비용으로 이어집니다.

- 업로드 및 전송 트래픽 증가
- 스토리지 사용량 누적
- 반복 수작업 증가
- 결과물 품질과 규격 불일치

ImageFlow는 이 흐름을 아래처럼 하나의 워크플로우로 정리합니다.

1. 로그인
2. 이미지 한 장 또는 여러 장 업로드
3. 마켓플레이스 프리셋 또는 출력 규칙 선택
4. 크롭, 품질, 워터마크 옵션 조정
5. 최적화 결과와 절감량 확인
6. 단일 파일 또는 ZIP으로 다운로드

## 현재 구현된 핵심 기능

### 사용자 경험

- SaaS형 랜딩 페이지
- `Get Started`, `Pricing` 흐름
- 별도 로그인 / 회원가입 화면
- 로그인 후 대시보드형 워크스페이스
- Google / Naver 소셜 로그인 진입 버튼
- 단일 이미지 업로드
- 다중 이미지 업로드
- ZIP 업로드
- 수동 크롭, center crop, fit 모드
- 리사이즈 및 품질 조정
- 선택적 워터마크 적용
- 최근 작업 이력 및 결과 검수
- 배치 ZIP 다운로드

### 시스템 기능

- JWT 기반 로그인
- 환경변수 기반 API 주소 관리
- 환경변수 기반 master 계정 부트스트랩
- Spring Boot API
- PostgreSQL 기반 사용자/작업/사용량 저장
- Redis Queue + Python worker 기반 비동기 처리 구조
- 로컬 스토리지 기반 처리
- R2/S3 이전을 고려한 저장 구조
- health endpoint 기반 운영 상태 노출

## 프로젝트 특징

- 결과에 `savedBytes`, `reductionRate`를 포함해 최적화 효과를 숫자로 확인할 수 있습니다.
- 단일 업로드뿐 아니라 다중 업로드와 ZIP 흐름까지 지원합니다.
- API 서버와 이미지 처리 worker를 분리한 비동기 구조를 갖고 있습니다.
- rate limit, retry, ZIP 제한, backlog control 같은 보호 장치를 넣었습니다.
- 로그인 전 랜딩과 로그인 후 워크스페이스를 분리해 서비스 형태로 정리했습니다.

## 운영 근거

- 실제 배포 URL 보유
- Swagger/OpenAPI 문서 제공
- Docker 기반 실행 가능
- 동시성 업로드 부하테스트 스크립트 제공
- rate limit, backlog control, outbox relay 기반 장애 대응 구조 반영

## 현재 배포 구성

- frontend: Vercel
- backend: Render Web Service
- database: Render Postgres
- queue: Render Key Value
- worker: local machine

포트폴리오 데모에서는 Render 무료 범위 안에서 backend와 queue를 배포하고, Python worker는 로컬에서 queue를 소비하도록 구성했습니다.  
worker는 최적화 결과 파일을 backend callback endpoint로 다시 업로드하므로, 분리된 파일 시스템을 직접 공유하지 않아도 됩니다.

> 주의: Render 무료 서버는 일정 시간 사용하지 않으면 슬립 상태에 들어갑니다. 첫 요청은 느리게 응답할 수 있습니다.
> 또한 현재 데모 구성에서는 worker를 로컬에서 실행해야 합니다.

## 하드닝 포인트

- 배치 업로드 제한: `최대 10개`
- ZIP 안전장치:
  - `최대 32개 엔트리`
  - `엔트리당 최대 50MB`
  - `전체 압축 해제 기준 최대 200MB`
- 업로드 rate limit: `분당 30회`
- 큐 publish retry: `3회`
- queue backlog 상한: `100`
- worker concurrency: `3`
- 결과 ZIP 다운로드: `StreamingResponseBody + ZipOutputStream` 기반 스트리밍

## 기술 스택

### Frontend

- React
- Vite
- Ant Design

### Backend

- Spring Boot
- Spring Data JPA
- PostgreSQL
- JWT Authentication

### Async Worker

- Redis
- Python
- Pillow

## 아키텍처

```text
Frontend (React + Vite)
  -> Backend API (Spring Boot)
    -> PostgreSQL
    -> Local storage / object storage migration path
    -> Redis queue
      -> Python image worker
```

## 저장소 구조

```text
backend/        Spring Boot API
frontend/       React + Vite 웹 앱
image-agent/    Python 비동기 이미지 워커
docs/           제품/배포/포트폴리오 문서
PORTFOLIO.md    서류용 포트폴리오 요약
docker-compose.yml
```

## 로컬 실행 방법

현재 `docker-compose.yml`은 전체 애플리케이션을 모두 올리는 구성이 아니라, 로컬 개발에 필요한 인프라와 worker만 올리는 구성을 사용합니다.

- Docker로 실행: `postgres`, `redis`, `image-agent`
- 호스트에서 실행: `backend`, `frontend`

### 1. Docker 인프라 실행

```bash
docker compose up -d postgres redis image-agent
```

워커 코드를 수정했다면 재빌드가 필요합니다.

```bash
docker compose build image-agent
docker compose up -d image-agent
```
중지 및 삭제입니다.

``` bash
docker-compose down -v --rmi all
```

### 2. 백엔드 실행

```bash
cd backend
.\gradlew.bat bootRun
```

기본 주소:

```text
http://localhost:8080
```

### 3. 프런트 실행

```bash
cd frontend
npm install
npm run dev
```

기본 주소:

```text
http://localhost:5173
```

프런트는 화면에서 백엔드 URL을 직접 입력하지 않습니다.  
개발 환경에서는 기본적으로 `http://localhost:8080` 을 사용하고, 배포 환경에서는 `VITE_API_BASE_URL` 로 API 주소를 관리합니다.

### Local worker against deployed Render queue

1. [image-agent/.env.render-local-worker.example](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/.env.render-local-worker.example:1) 를 기준으로 값을 복사합니다.
2. `REDIS_URL` 을 Render Key Value connection string으로 교체합니다.
3. `BACKEND_BASE_URL` 을 현재 Render backend URL로 맞춥니다.
4. 아래처럼 실행합니다.

```bash
cd imageagent
python -m pip install -r requirements.txt
Copy-Item .env.render-local-worker.example .env.render-local-worker
# 값 수정 후
.\run-render-local-worker.ps1
```

> 주의: 이 모드는 worker를 실제 로컬 머신에서 실행해야 합니다. Render 무료 환경에서는 background worker를 사용하지 않으므로, worker는 로컬로 띄워야 합니다.

이 모드에서는 worker가 queue를 로컬에서 소비하고, 결과 파일은 backend callback endpoint로 다시 업로드합니다.

### 4. 선택 설정

소셜 로그인 버튼을 실제 인증 페이지와 연결하려면 아래 값을 설정합니다.

- `APP_AUTH_SOCIAL_GOOGLE_AUTH_URL`
- `APP_AUTH_SOCIAL_NAVER_AUTH_URL`

마스터 계정을 자동 생성하려면 아래 값을 설정합니다.

- `APP_MASTER_EMAIL`
- `APP_MASTER_PASSWORD`

기본 master 계정:

- `imageflowmaster@master`
- `imageflow123!`

## 데모 실행 권장 방식

### 로컬 데모

- backend: 호스트 실행
- frontend: 호스트 실행
- worker: Docker 실행 가능
- storage: 로컬 사용

### 포트폴리오/무료 배포 데모

- frontend: Vercel
- backend: Render
- database: Render Postgres
- 비용 제약이 있으면 sync 또는 단순 모드 사용

### Render 비동기 배포 메모

- 기본 `render.yaml`은 무료 데모 기준의 `backend + postgres` 구성입니다.
- Render 무료 플랜은 background worker를 지원하지 않아서, full async 배포는 별도 worker 인스턴스가 필요합니다.
- async 배포 예시는 [render.async.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/render.async.yaml:1) 에 정리했습니다.
- 이 예시는 `Render Key Value + starter worker` 기준입니다.
- 분리 배포 환경에서는 worker가 결과 파일을 백엔드로 다시 업로드하는 callback 경로를 사용하므로, 백엔드와 worker가 로컬 파일 시스템을 직접 공유하지 않아도 됩니다.

자세한 내용은 [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1) 에 정리되어 있습니다.

## 참고 문서

- [docs/product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
  README 기준 제품 요약본
- [docs/api-reference.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/api-reference.md:1)
  API 문서 요약
- [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
  실행/배포 가이드
- [docs/portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
  포트폴리오 케이스 스터디
- [PORTFOLIO.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/PORTFOLIO.md:1)
  포트폴리오 요약 문서
- [load-tests/README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/load-tests/README.md:1)
  동시성 업로드 테스트 실행 가이드

## 현재 한계

- R2/S3 완전 이전은 아직 진행 중입니다.
- billing은 제품 방향 수준까지 정리되어 있고 실제 결제 연동은 미완성입니다.
- monitoring은 기본 health/rate limit/retry 수준이며 observability는 더 확장할 수 있습니다.
- 프런트 번들 크기는 추가 최적화 여지가 있습니다.
