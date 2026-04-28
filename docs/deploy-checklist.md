# Demo Deploy Checklist

> This file is kept as a short legacy checklist.
> For the current deployment guide, use [deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1).

## 목적

이 문서는 ImageFlow를 `데모/포트폴리오용`으로 배포할 때 필요한 실제 체크리스트입니다.

권장 조합:

- frontend: Vercel
- backend: Render
- database: Render Postgres

## 배포 전 확인

- GitHub에 최신 코드 push
- frontend 로컬 빌드 확인
- backend 테스트 통과 확인
- 무료 배포 기준으로 `sync` 처리 모드 사용

## Backend: Render

`render.yaml`이 이미 포함되어 있어서 Blueprint 방식으로 올리면 됩니다.

확인할 값:

- `rootDir: backend`
- `buildCommand: ./gradlew build -x test`
- `startCommand: ./gradlew bootRun`

### Render 환경변수

반드시 확인할 값:

- `APP_PUBLIC_BASE_URL`
  - 예: `https://imageflow-backend.onrender.com`
- `APP_AUTH_JWT_SECRET`
  - 최소 32자 이상의 실제 비밀값으로 교체 권장

자동 연결되는 값:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

기본값으로 둬도 되는 값:

- `APP_STORAGE_ROOT=/tmp/imageflow-storage`
- `APP_PROCESSING_MODE=sync`
- `APP_QUEUE_ENABLED=false`

## Frontend: Vercel

Vercel에서 `frontend`를 Root Directory로 지정합니다.

### Vercel 환경변수

- `VITE_API_BASE_URL`
  - 예: `https://imageflow-backend.onrender.com`

## 배포 후 점검

1. 회원가입/로그인 되는지 확인
2. 작은 이미지 1장 업로드
3. 결과 이미지 생성 확인
4. 절감률/파일 크기 표시 확인
5. ZIP 다운로드 확인

## 데모 배포 한계

- Render free는 슬립될 수 있어서 첫 요청이 느릴 수 있음
- 업로드 파일과 결과 파일은 영구 저장이 아님
- Redis worker는 데모 배포 기준 비활성
- 대용량 파일은 플랫폼 제한에 영향을 받을 수 있음
