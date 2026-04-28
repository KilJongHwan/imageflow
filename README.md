# ImageFlow

ImageFlow는 커머스 상품 이미지를 더 가볍고 일관되게 준비할 수 있도록 돕는 `SaaS형 이미지 최적화 워크스페이스`입니다.  
셀러와 운영 담당자가 여러 장의 상품 이미지를 업로드하고, 크롭·리사이즈·압축·워터마크 규칙을 적용한 뒤, 결과와 절감량을 한 번에 확인하고 다운로드할 수 있도록 설계했습니다.

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
- 로그인 후 대시보드형 워크스페이스
- 단일 이미지 업로드
- 다중 이미지 업로드
- ZIP 업로드
- 수동 크롭, center crop, fit 모드
- 리사이즈 및 품질 조정
- 선택적 워터마크 적용
- 최근 작업 이력 및 결과 검수
- 배치 ZIP 다운로드

### 시스템 기능

- JWT 기반 회원가입/로그인
- Spring Boot API
- PostgreSQL 기반 사용자/작업/사용량 저장
- Redis Queue + Python worker 기반 비동기 처리 구조
- 로컬 스토리지 기반 처리
- R2/S3 이전을 고려한 저장 구조
- health endpoint 기반 운영 상태 노출

## 이 프로젝트가 단순 데모를 넘는 이유

- 결과에 `savedBytes`, `reductionRate`를 포함해 최적화 효과를 숫자로 보여줍니다.
- 단일 업로드가 아니라 배치 업로드와 ZIP 흐름까지 포함합니다.
- API 서버와 이미지 처리 워커를 분리할 수 있는 비동기 구조를 갖고 있습니다.
- rate limit, retry, ZIP 안전장치 등 운영형 보호 로직이 포함돼 있습니다.
- 로그인 전 랜딩과 로그인 후 워크스페이스를 분리해 제품형 구조를 갖췄습니다.

## 하드닝 포인트

- 배치 업로드 제한: `최대 10개`
- ZIP 안전장치:
  - `최대 32개 엔트리`
  - `엔트리당 최대 20MB`
  - `전체 압축 해제 기준 최대 50MB`
- 업로드 rate limit: `분당 30회`
- 큐 publish retry: `3회`
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

### 1. 백엔드 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

기본 주소:

```text
http://localhost:8080
```

### 2. 프런트 실행

```powershell
cd frontend
npm install
npm run dev
```

기본 주소:

```text
http://localhost:5173
```

### 3. 비동기 워커 스택 실행

```powershell
docker compose up -d redis image-agent
```

워커 코드를 수정했다면 재빌드가 필요합니다.

```powershell
docker compose build image-agent
docker compose up -d image-agent
```

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

자세한 내용은 [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1) 에 정리되어 있습니다.

## 문서 가이드

- [docs/README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/README.md:1)
  전체 문서 안내
- [docs/product-overview.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/product-overview.md:1)
  제품 개요와 현재 구현 범위
- [docs/deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1)
  실행/배포 가이드
- [docs/portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1)
  포트폴리오용 케이스 스터디
- [PORTFOLIO.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/PORTFOLIO.md:1)
  서류용 요약 문서

## 현재 한계

- R2/S3 완전 이전은 아직 진행 중입니다.
- billing은 제품 방향 수준까지 정리되어 있고 실제 결제 연동은 미완성입니다.
- monitoring은 기본 health/rate limit/retry 수준이며 observability는 더 확장할 수 있습니다.
- 프런트 번들 크기는 추가 최적화 여지가 있습니다.

## 서류에서 강하게 보이는 포인트

- 커머스 운영 문제를 실제 사용자 흐름으로 풀었다는 점
- 단순 CRUD를 넘어서 업로드, 배치 처리, ZIP 다운로드, 비동기 워커까지 다뤘다는 점
- 결과를 `얼마나 줄였는가`로 설명할 수 있도록 수치화했다는 점
- 데모 툴이 아니라 운영형 SaaS 구조로 다듬으려는 제품 감각과 안정화 작업이 같이 보인다는 점
