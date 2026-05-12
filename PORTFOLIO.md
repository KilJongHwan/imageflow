# ImageFlow Portfolio

## 한 줄 소개

커머스 상품 이미지 업로드 과정에서 발생하는 용량, 작업 시간, 결과물 일관성 문제를 줄이기 위해 만든 이미지 최적화 워크스페이스입니다.

## 프로젝트 요약

이미지 최적화 기능 자체보다, 실제 운영에서 반복되는 흐름을 정리하는 데 더 집중했습니다.  
셀러나 운영 담당자가 여러 장의 이미지를 올리고, 필요한 규격으로 자르고, 최적화 결과를 확인한 뒤 한 번에 내려받을 수 있도록 만들었습니다.

핵심은 두 가지였습니다.

- 원본 이미지 용량을 줄여 전송량과 저장 비용을 낮추는 것
- 반복 작업을 줄이고 결과물 규격을 맞추는 것

## 해결하려던 문제

커머스 이미지 운영에서는 아래 문제가 자주 생깁니다.

- 원본 이미지 용량이 커서 업로드와 전송 비용이 커짐
- 작업자마다 크롭 기준이 달라 결과가 들쭉날쭉함
- 여러 장 이미지를 한 장씩 따로 편집해야 해서 시간이 오래 걸림
- 배치 처리와 다운로드 흐름이 약하면 운영 효율이 떨어짐

ImageFlow는 이 과정을 `업로드 -> 규격 선택 -> 처리 -> 결과 확인 -> ZIP 다운로드` 흐름으로 묶는 것을 목표로 했습니다.

## 구현 범위

- JWT 기반 로그인/회원가입
- 로그인 전 랜딩, Pricing, Get Started
- 로그인 후 대시보드형 워크스페이스
- 단일/다중 이미지 업로드
- ZIP 업로드
- 수동 크롭, center-crop, fit
- 리사이즈, 품질 조절
- 선택적 워터마크 적용
- 결과 미리보기와 최근 작업 이력
- 배치 ZIP 다운로드
- Redis + Python worker 기반 비동기 처리 구조

## 수치로 정리한 보호 장치

- 배치 업로드 최대 `10개`
- ZIP 최대 `32개 엔트리`
- ZIP 엔트리당 최대 `50MB`
- ZIP 전체 압축 해제 기준 최대 `200MB`
- 업로드 API 기본 rate limit `분당 30회`
- queue publish retry `3회`, `200ms`
- queue backlog 상한 `100`
- worker 기본 동시 처리 수 `3`

이 수치들은 단순 제한이 아니라, 많이 올렸을 때 어디서 서버가 무너질 수 있는지 보고 정한 값들입니다.

## 주요 구현과 해결 과정

### 1. 결과를 “압축됨”이 아니라 숫자로 보여주기

결과 화면에 `sourceFileSizeBytes`, `resultFileSizeBytes`, `savedBytes`, `reductionRate`를 포함해 실제로 얼마나 줄었는지 바로 보이게 했습니다.  
이 덕분에 이 프로젝트를 단순 편집 툴이 아니라 비용 절감 관점에서 설명할 수 있게 됐습니다.

코드:

- [ImageJobResponse.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/dto/ImageJobResponse.java:46)
- [ResultPanel.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/components/ResultPanel.jsx:177)

### 2. ZIP 업로드를 “되기만 하는 기능”이 아니라 통제된 입력으로 만들기

처음에는 ZIP 업로드만 지원하면 된다고 생각했지만, 실제로는 압축 해제 용량과 엔트리 수를 제한하지 않으면 서버에 부담이 컸습니다.  
그래서 엔트리 수, 엔트리당 용량, 전체 압축 해제 용량을 나눠 제한했고, 초과 시에는 사용자가 이해할 수 있는 문구로 안내하도록 정리했습니다.

코드:

- [application.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/resources/application.yaml:43)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:598)
- [App.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/App.jsx:35)

### 3. ZIP 다운로드 시 메모리 부담 줄이기

결과 파일을 한 번에 ZIP으로 묶을 때, 메모리에 전부 올리는 방식은 배치 크기가 커질수록 부담이 커집니다.  
그래서 `StreamingResponseBody + ZipOutputStream`으로 바꿔 스트리밍 방식으로 내려받도록 구성했습니다.

코드:

- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:389)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:418)

### 4. 배치 업로드가 체감상 멈춘 것처럼 보이던 문제

ZIP 안의 이미지가 여러 장이어도, worker가 하나씩만 처리하면 첫 번째 결과만 보이고 나머지는 오래 `QUEUED` 상태로 남았습니다.  
이 문제를 줄이기 위해 worker에 `WORKER_CONCURRENCY`를 넣어 기본 3개 작업까지 동시에 처리하도록 바꿨고, 프런트도 배치 진행 상태를 계속 따라가도록 수정했습니다.

코드:

- [worker.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/worker.py:1)
- [App.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/App.jsx:558)

### 5. 병렬 worker 도입 후 생긴 race condition 정리

worker를 빠르게 병렬 처리로 바꾸자, 일부 작업이 결과 콜백 단계에서 `404`로 실패했습니다.  
원인을 확인해 보니, job 저장 트랜잭션이 커밋되기 전에 worker가 결과를 먼저 보내는 경쟁 상태가 있었습니다.

그래서 queue publish를 즉시 실행하지 않고, Spring 트랜잭션의 `afterCommit` 이후에만 나가도록 바꿨습니다.  
이 수정으로 `저장 -> 커밋 -> queue publish -> worker 처리` 순서를 보장할 수 있었습니다.

코드:

- [ImageJobQueuePublisher.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/queue/ImageJobQueuePublisher.java:1)

### 6. 로컬 백엔드와 Docker worker 저장 경로가 어긋나던 문제

백엔드는 호스트에서 실행하고 worker는 Docker에서 돌릴 때, 처리 성공으로 보여도 결과 파일이 실제로는 안 보이는 문제가 있었습니다.  
worker가 `localhost` URL을 그대로 사용하거나 컨테이너 내부 경로에 저장하고 있었기 때문입니다.

이 문제를 해결하기 위해:

- worker에서 `localhost` URL을 `BACKEND_BASE_URL`로 재작성
- shared storage volume과 `STORAGE_ROOT` 추가

구조로 맞췄습니다.

코드:

- [worker.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/worker.py:380)
- [docker-compose.yml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docker-compose.yml:1)

## 운영 관점에서 보강한 부분

- upload rate limit 적용
- queue publish retry 적용
- queue backlog 상한 도입
- health endpoint에서 `processingMode`, `queueDepth`, `queueWritable`, `storageProvider` 노출
- 로그아웃 시 polling 정리

단순히 “처리된다”에서 멈추지 않고, 운영 중 어떤 상태를 봐야 하는지까지 같이 정리한 작업이었습니다.

## 주요 성과

- 단순 CRUD가 아니라 업로드, ZIP, 비동기 worker, 다운로드까지 이어지는 흐름을 설계했다는 점
- 화면도 도구 느낌이 아니라 SaaS형 제품 구조로 정리했다는 점
- 대용량 파일 처리에서 생길 수 있는 메모리, 압축, 큐 적체, race condition을 직접 디버깅하고 보완했다는 점
- 결과를 “최적화했다”가 아니라 “얼마나 줄였는지”로 설명할 수 있게 만들었다는 점

## 향후 보완 과제

- 객체 스토리지 전환은 아직 마무리 전입니다.
- Jobs 전용 화면과 필터링은 더 보강할 수 있습니다.
- billing과 observability는 기본 구조까지 정리했고, 실제 운영 수준까지는 더 확장 여지가 있습니다.
