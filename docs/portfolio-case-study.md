# Portfolio Case Study

## 프로젝트 개요

ImageFlow는 커머스 이미지 운영 과정에서 반복되는 업로드, 크롭, 리사이즈, 압축, 다운로드 작업을 한 흐름으로 정리한 프로젝트입니다.  
목표는 단순 편집 기능이 아니라, 대용량 이미지로 인해 생기는 비용과 운영 비효율을 줄이는 것이었습니다.

## 문제 정의

커머스 환경에서는 다음 문제가 반복됩니다.

- 원본 이미지 용량이 커서 전송량과 저장소 비용이 커진다
- 작업자마다 크롭 기준이 달라 결과물 규격이 흔들린다
- 여러 장 이미지를 개별 툴에서 반복 편집해야 한다
- 배치 처리와 다운로드 흐름이 약하면 운영 시간이 길어진다

이 프로젝트에서는 이 문제를 `서비스형 워크플로우`로 풀고 싶었습니다.

## 구현 목표

- 로그인한 사용자가 이미지 작업을 수행할 수 있는 구조 만들기
- 단일 이미지뿐 아니라 다중 이미지와 ZIP 업로드까지 지원하기
- 크롭, 리사이즈, 품질 조절, 워터마크를 한 번에 처리하기
- 결과를 단순 미리보기로 끝내지 않고 ZIP 다운로드까지 연결하기
- API 서버와 이미지 처리 worker를 분리해 확장 가능한 구조 만들기

## 구현한 기능

- JWT 기반 로그인/회원가입
- 랜딩 페이지, Pricing, Get Started 흐름
- 로그인 후 대시보드형 워크스페이스
- 단일/다중 이미지 업로드
- ZIP 업로드
- 수동 크롭, center-crop, fit
- 리사이즈 및 품질 조절
- 선택적 워터마크
- 최근 작업 이력 및 결과 검수
- 배치 ZIP 다운로드
- Redis + Python worker 기반 비동기 처리

## 운영 근거

- 서비스 URL: `https://imageflow-rose.vercel.app`
- Backend URL: `https://imageflow-backend-kdt1.onrender.com`
- API 문서: `https://imageflow-backend-kdt1.onrender.com/swagger-ui` 및 [api-reference.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/api-reference.md:1)
- Health: `https://imageflow-backend-kdt1.onrender.com/api/health`
- Docker 실행: `docker compose` 기반
- 부하테스트: [concurrent_upload_test.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/load-tests/concurrent_upload_test.py:1)

배포본 확인 흐름:

1. `https://imageflow-rose.vercel.app`
2. 로그인 후 워크스페이스 진입
3. 이미지 업로드 또는 ZIP 업로드
4. 결과 절감량과 다운로드 확인
5. `swagger-ui`, `api/health`로 백엔드 상태 확인

## 수치로 정리한 기준

- batch upload cap: `10`
- ZIP entry cap: `32`
- ZIP entry size cap: `50MB`
- total extracted ZIP size cap: `200MB`
- upload rate limit: `30 rpm`
- queue publish retry: `3 attempts`, `200ms`
- queue backlog threshold: `100`
- worker concurrency: `3`

이 수치들은 문서용 장식이 아니라, 실제로 어디까지 입력을 허용하고 어디서부터 보호 장치를 거는지 정리한 기준입니다.

## 설계에서 신경 쓴 부분

### 1. 결과를 숫자로 보여주는 구조

최적화 결과에 `savedBytes`, `reductionRate`를 포함해 원본 대비 얼마나 줄었는지 바로 확인할 수 있게 했습니다.  
이 덕분에 이 프로젝트를 “이미지 편집 기능”이 아니라 비용 절감과 연결해서 설명할 수 있었습니다.

코드:

- [ImageJobResponse.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/dto/ImageJobResponse.java:46)

### 2. ZIP 업로드를 통제된 입력으로 다룬 점

ZIP 업로드는 편하지만, 엔트리 수나 압축 해제 용량을 제한하지 않으면 서버에 부담이 큽니다.  
그래서 엔트리 수, 엔트리당 용량, 전체 압축 해제 용량을 나눠 제한했고, 사용자는 에러 원인을 바로 이해할 수 있도록 프런트에서 문구를 다시 변환했습니다.

코드:

- [application.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/resources/application.yaml:43)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:598)
- [App.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/App.jsx:35)

### 3. ZIP 다운로드를 스트리밍으로 전환한 점

배치 결과를 한 번에 내려받을 때, 파일 전체를 메모리에 올리는 방식은 위험하다고 봤습니다.  
그래서 `StreamingResponseBody + ZipOutputStream`으로 바꿔 스트리밍 방식으로 응답하도록 구성했습니다.

코드:

- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:389)

## 실제로 부딪힌 기술 문제

### 1. ZIP 배치 업로드가 체감상 멈춘 것처럼 보인 문제

처음에는 ZIP 안의 여러 이미지가 모두 비동기 처리되더라도, worker가 순차적으로 하나씩만 소비하고 있어서 첫 작업만 먼저 끝나고 나머지는 오래 `QUEUED` 상태로 남았습니다.

이 문제를 줄이기 위해:

- worker에 `WORKER_CONCURRENCY`를 넣어 기본 3개 작업까지 동시에 처리
- 프런트에서 배치 상태를 계속 따라가도록 polling 보강

구조로 수정했습니다.

코드:

- [worker.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/worker.py:1)
- [App.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/App.jsx:558)

### 2. worker 병렬 처리 이후 result callback 404가 발생한 문제

worker를 병렬 처리로 바꾼 뒤 일부 job이 `PATCH /api/image-jobs/{id}/result`에서 `404`로 실패했습니다.

원인은 저장 트랜잭션이 커밋되기 전에 queue publish가 먼저 일어나고 있었기 때문이었습니다. worker가 빨라지자 이 경쟁 상태가 실제 문제로 드러난 것입니다.

해결은 queue publish 시점을 `afterCommit` 이후로 미루는 것이었습니다.

코드:

- [ImageJobQueuePublisher.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/queue/ImageJobQueuePublisher.java:1)

이후에는 이 구조를 한 단계 더 보강해, 메시지를 바로 Redis로 발행하지 않고 DB Outbox 테이블에 먼저 적재한 뒤 relay가 전송하도록 변경했습니다.  
이렇게 하면 DB 커밋 이후 프로세스가 중단되더라도 메시지를 다시 확인해 재전송할 수 있습니다.

### 3. 호스트 백엔드와 Docker worker의 저장 경로가 달랐던 문제

백엔드는 호스트에서 돌고 worker는 Docker에서 돌 때, 성공으로 처리돼도 결과 이미지가 실제로는 백엔드에서 보이지 않는 문제가 있었습니다.

이 문제를 해결하기 위해:

- worker에서 `localhost` URL을 `BACKEND_BASE_URL` 기준으로 재작성
- shared storage volume과 `STORAGE_ROOT` 추가

를 적용했습니다.

코드:

- [worker.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/worker.py:380)
- [docker-compose.yml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docker-compose.yml:1)

### 4. 비동기 구조만으로는 과부하 문제가 해결되지 않는 점

큐와 worker를 분리해도, 입력이 처리 속도보다 빠르면 backlog가 쌓입니다.  
그래서 queue depth 기준으로 새 업로드를 막는 backpressure를 추가했고, health와 대시보드에서 현재 queue 상태를 볼 수 있게 했습니다.

코드:

- [QueueBackpressureService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/common/ops/QueueBackpressureService.java:1)
- [HealthController.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/api/HealthController.java:1)

### 5. 분리 배포 환경에서 worker 결과 파일이 백엔드에 남지 않는 문제

로컬에서는 shared volume으로 해결할 수 있었지만, Render처럼 backend와 worker를 다른 서비스로 분리하면 local output 파일을 직접 공유할 수 없습니다.

이 문제를 해결하기 위해:

- worker가 최적화 결과 파일을 `PATCH /api/image-jobs/{id}/result-file` endpoint로 다시 업로드
- backend가 이 파일을 자신의 output 경로에 저장
- async Render 예시에서는 `REDIS_URL`, `BACKEND_HOSTPORT`, `WORKER_RESULT_CALLBACK_ENABLED` 조합을 사용

구조로 정리했습니다.

이 변경으로 backend와 worker가 파일 시스템을 공유하지 않아도 비동기 결과 다운로드 흐름을 유지할 수 있게 됐습니다.

## 결과적으로 정리된 구조

- 프런트는 랜딩과 앱을 분리해 제품형 흐름을 갖추도록 정리
- 백엔드는 인증, 업로드, 작업 상태, ZIP 다운로드를 담당
- worker는 실제 이미지 처리 전용으로 분리
- 보호 장치는 업로드 제한, ZIP 제한, rate limit, queue retry, backlog control로 구성

## 운영 및 검증 항목

- 실제 배포 URL: `https://imageflow-rose.vercel.app`
- API 문서: Swagger UI(`/swagger-ui`), OpenAPI JSON(`/api-docs`)
- Docker 실행: 가능
- 부하테스트 스크립트: [concurrent_upload_test.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/load-tests/concurrent_upload_test.py:1)
- 장애 대응 구조: 반영
- Render async 예시: [render.async.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/render.async.yaml:1)

저장소 기준으로 확인 가능한 내용은 다음과 같습니다.

- Docker는 `docker-compose.yml`을 통해 `postgres`, `redis`, `image-agent`를 실행할 수 있습니다.
- 장애 대응 구조는 rate limit, queue retry, backlog control, health endpoint, worker concurrency, DB Outbox relay를 중심으로 구성되어 있습니다.
- 분리 배포 환경에서는 worker result-file callback으로 shared storage 없이 결과 파일을 백엔드에 반영할 수 있습니다.

## 주요 성과

- 업로드와 파일 처리 중심의 제품 흐름을 설계하고 구현함
- 대용량 입력에 대한 제한과 보호 장치를 정리함
- Redis 기반 비동기 worker 구조와 큐 운영 흐름을 직접 구성함
- worker 병렬 처리 도입 과정에서 생긴 race condition을 디버깅하고 수정함
