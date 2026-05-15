# Load Tests

## 목적

이 디렉터리는 ImageFlow의 업로드 동시성 테스트를 바로 실행할 수 있도록 만든 스크립트를 포함합니다.

## 실행 전 준비

- backend 실행
- redis / image-agent 실행
- master 계정 사용 가능

예시:

```powershell
docker compose up -d postgres redis image-agent
cd backend
.\gradlew.bat bootRun
```

## 동시 업로드 테스트

```powershell
python load-tests/concurrent_upload_test.py --base-url http://localhost:8080 --requests 12 --concurrency 3 --wait-completion
```

기본 계정은 현재 프로젝트의 master 계정을 사용합니다.

- email: `imageflowmaster@master`
- password: `imageflow123!`

## 출력 항목

- 총 실행 시간
- 업로드 성공/실패 수
- 업로드 평균 응답 시간
- 업로드 p95 응답 시간
- 최대 업로드 시간
- 완료 대기 시간 평균/p95
- 완료 상태 분포
- HTTP status code 분포
