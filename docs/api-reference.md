# API Reference

## 문서 접근 경로

- 로컬 Swagger UI: `http://localhost:8080/swagger-ui`
- 로컬 OpenAPI JSON: `http://localhost:8080/api-docs`

배포 환경에서는 같은 경로를 백엔드 주소 뒤에 붙여 접근할 수 있습니다.

## 주요 엔드포인트

### Auth

- `POST /api/auth/signup`
  - 로컬 계정을 생성하고 JWT를 반환합니다.
- `POST /api/auth/login`
  - 로그인 후 JWT를 반환합니다.
- `GET /api/auth/providers`
  - 설정된 소셜 로그인 진입 정보를 반환합니다.
- `GET /api/auth/me`
  - 현재 인증된 사용자를 반환합니다.

### Image Jobs

- `POST /api/image-jobs/upload`
  - 단일 이미지 업로드와 작업 생성을 처리합니다.
- `POST /api/image-jobs/uploads`
  - 다중 이미지 또는 ZIP 업로드와 배치 작업 생성을 처리합니다.
- `GET /api/image-jobs`
  - 최근 작업 목록을 반환합니다.
- `GET /api/image-jobs/{imageJobId}`
  - 개별 작업 상태와 결과 메타데이터를 반환합니다.
- `GET /api/image-jobs/download?jobIds=...`
  - 완료된 작업 결과를 ZIP으로 내려받습니다.
- `PATCH /api/image-jobs/{imageJobId}/result`
  - worker가 작업 상태를 업데이트하는 콜백 엔드포인트입니다.
- `PATCH /api/image-jobs/{imageJobId}/result-file`
  - worker가 최적화 결과 파일을 backend로 다시 업로드하는 콜백 엔드포인트입니다.

### Operations

- `GET /api/health`
  - processing mode, queue depth, outbox pending count, storage provider 등 운영 상태를 반환합니다.

## 인증 방식

- 대부분의 사용자 엔드포인트는 `Authorization: Bearer <token>` 헤더를 사용합니다.
- worker callback인 `PATCH /api/image-jobs/{imageJobId}/result`, `PATCH /api/image-jobs/{imageJobId}/result-file`는 현재 내부 처리 용도로 사용됩니다.
