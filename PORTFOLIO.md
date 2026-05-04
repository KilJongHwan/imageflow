# ImageFlow Portfolio

## 프로젝트 한 줄 소개

`대용량 상품 이미지 업로드로 인해 발생하는 트래픽 및 스토리지 비용 증가 문제를 완화하기 위한 커머스 이미지 최적화 SaaS 워크스페이스 개발`

## 프로젝트 개요

ImageFlow는 커머스 환경에서 반복적으로 발생하는 상품 이미지 업로드, 크롭, 리사이즈, 압축, 워터마크, 다운로드 작업을 하나의 흐름으로 정리한 프로젝트입니다.  
단순 이미지 편집기가 아니라, `운영 비용 절감`과 `실무 작업 효율화`를 함께 다루는 `SaaS형 워크스페이스`를 목표로 설계했습니다.

타깃 사용자는 다음과 같습니다.

- 네이버 스마트스토어, 쿠팡, 컬리 등 상품 이미지를 자주 다루는 셀러
- 상품 대표 이미지와 상세용 이미지를 반복 가공하는 MD/운영 담당자
- 대량 이미지 업로드로 트래픽과 스토리지 비용이 커지는 서비스를 운영하는 팀

## 문제 정의

커머스 서비스에서는 고해상도 상품 이미지가 반복적으로 업로드되면서 다음 문제가 발생합니다.

- 원본 이미지 용량이 커서 업로드/전송 트래픽 비용이 증가함
- 불필요하게 큰 파일이 누적되어 스토리지 사용량이 빠르게 늘어남
- 작업자마다 크롭과 리사이즈 기준이 달라 결과물 일관성이 떨어짐
- 배치 처리와 다운로드 흐름이 부족하면 운영 시간이 길어짐

이 프로젝트는 이 문제를 다음 두 축에서 해결하려고 했습니다.

- 시스템 관점: 이미지 용량을 줄여 트래픽 및 저장 비용을 낮춤
- 사용자 관점: 대량 업로드와 결과 검수 흐름을 단순화해 반복 작업 시간을 줄임

## 문제 해결을 수치로 요약

서류에서 바로 읽히는 핵심 수치는 아래와 같습니다.

- 최적화 결과를 `savedBytes`, `reductionRate`로 계산해 원본 대비 절감량과 절감률을 수치로 확인 가능
- 마케팅/데모 화면에서는 `Up To 72%` 용량 절감 예시를 제시해 제품 가치가 직관적으로 보이도록 구성
- 배치 업로드 최대 `10개` 제한으로 동시 처리 범위를 제어
- ZIP 업로드 최대 `32개 엔트리` 제한
- ZIP 엔트리당 최대 `20MB` 제한
- ZIP 전체 압축 해제 기준 최대 `50MB` 제한
- 업로드 API 기본 rate limit `분당 30회`
- Redis queue publish retry `3회`, retry 간격 `200ms`
- 무료 플랜 기본 크레딧 `20`

이 수치들은 단순 기능 나열이 아니라, `대용량 업로드`, `메모리 리스크`, `무제한 요청`, `비용 가시성 부족` 같은 문제를 어디까지 통제했는지 설명하는 근거로 사용했습니다.

## 핵심 기능

- JWT 기반 회원가입/로그인
- 인증된 사용자만 이미지 업로드 가능
- SaaS형 랜딩 페이지, Pricing 3단 구조, Get Started 흐름
- 로그인 전 랜딩 / 로그인 후 워크스페이스 앱 분리
- 단일 이미지 업로드 및 다중 이미지 업로드 지원
- ZIP 업로드 지원
- 업로드 파일 형식 검증
- 수동 크롭 UI 지원
- 리사이즈, 품질 조정, 선택적 워터마크 적용
- 텍스트/이미지 업로드 기반 워터마크 지원
- 마켓플레이스별 프리셋 확장
- 배치 결과 ZIP 다운로드
- 최근 작업 목록 및 결과 검수 화면 제공
- 로그인 후 대시보드형 홈, 플랜/사용량/운영 상태 요약
- 원본 대비 최적화 결과 용량/절감률 표시
- 큐 기반 비동기 처리 구조
- 업로드 rate limit, 큐 publish retry, health 기반 ops snapshot

## 기술 선택 이유

### Frontend

- React + Vite
- Ant Design

선택 이유:

- 빠르게 상태 기반 UI를 구성할 수 있고, 업로드/폼/리스트/카드 중심의 운영 화면을 만들기 적합함
- Ant Design을 사용해 기본적인 제품형 UI 톤을 빠르게 맞출 수 있음

### Backend

- Spring Boot
- Spring Data JPA
- PostgreSQL
- JWT 인증

선택 이유:

- 업로드, 인증, 작업 상태 관리, 배치 다운로드 같은 API 중심 구조에 적합함
- 실무형 CRUD + 인증 + 파일 처리 흐름을 안정적으로 구성하기 쉬움

### Worker / 운영 확장 구조

- Python image-agent
- Redis Queue

선택 이유:

- 이미지 처리량이 커질 경우 `API 서버`와 `이미지 처리 워커`를 분리하는 구조가 필요하다고 판단함
- 워커 기반 비동기 처리, 객체 스토리지, 재시도 정책 등 운영 확장 포인트를 설명하기 좋음

## 시스템 구조

```text
Frontend (React)
  -> Backend API (Spring Boot)
    -> Auth / Upload / Job / Download
    -> PostgreSQL (job, user, usage)
    -> Local storage or object storage
    -> Optional Redis Queue + Python worker
```

현재 구현 기준에서는:

- 업로드 요청만 API 서버에서 수신
- Redis Queue에 작업 등록
- Python worker가 이미지 처리 수행
- Local storage 기반 처리, 이후 R2/S3로 이전 가능한 설정 구조 유지
- API 서버가 상태 및 결과 제공
- health endpoint에서 processing mode, queue depth, storage provider, rate limit 상태 제공

## 내가 집중한 포인트

### 1. 단순 편집기가 아니라 SaaS형 서비스로 접근

처음에는 이미지 최적화 기능 자체에 집중했지만, 프로젝트 방향을 다음처럼 다시 정의했습니다.

- 무엇을 만들었는가: 이미지 최적화 툴
- 무엇을 해결하는가: 대용량 상품 이미지 업로드로 인한 비용과 반복 운영 문제를 줄이는 SaaS형 워크스페이스

이렇게 문제 중심으로 재정의하고, 로그인 전 랜딩 / 로그인 후 앱 구조를 분리하면서 포트폴리오 설득력이 훨씬 좋아졌습니다.

### 2. 업로드 흐름을 실무형으로 구성

실제 사용자를 생각하면 한 장만 올리는 경우보다 여러 장을 묶어서 다루는 흐름이 더 중요했습니다.  
그래서 단일 업로드 외에도:

- 여러 장 드래그 앤 드롭
- ZIP 업로드
- 파일 형식 검증
- 선택 파일 목록 정리
- 배치 ZIP 다운로드

같은 흐름을 붙였습니다.

### 3. 결과를 숫자로 설명할 수 있게 설계

단순히 “최적화됨”이라고 보여주는 대신:

- 원본 파일 크기
- 결과 파일 크기
- 절감 바이트
- 절감률

을 응답과 UI에 함께 노출해, 이 프로젝트가 비용 관점에서 무엇을 해결하는지 설명할 수 있게 했습니다.

### 4. 로그인 후 화면을 운영 대시보드처럼 재구성

처음 로그인 후 화면은 기능 카드가 나열된 도구처럼 보여서, 실제 SaaS의 워크스페이스 느낌이 약했습니다.  
이를 개선하기 위해:

- Welcome back 영역
- current plan / credits / recent jobs / storage saved
- usage overview
- upgrade CTA
- ops snapshot

을 먼저 보여주고, 그 아래에서 실제 업로드/결과 워크플로우로 내려가는 구조로 바꿨습니다.

### 5. 워터마크 기능을 선택적 적용 흐름으로 정리

초기에는 워터마크가 기능적으로 붙어 있는 상태였지만, 실제 서비스에서는 모든 배치에 자동 적용되는 UX가 어색했습니다.
그래서:

- 텍스트 또는 이미지 업로드 기반 워터마크만 유지
- 사용자가 `이번 배치에 워터마크 적용`을 켠 경우에만 반영

되도록 정리해, 실제 운영 플로우에 더 가깝게 다듬었습니다.

## 구현하면서 어려웠던 점

### 1. 툴 느낌과 서비스 느낌의 차이

기능은 빠르게 붙었지만, 처음 화면은 데모 툴처럼 보여서 실제 서비스처럼 느껴지지 않는 문제가 있었습니다.  
그래서 UI를 단순히 화려하게 만드는 대신:

- 업로드
- 규칙 선택
- 결과 검수
- 최근 작업

이 자연스럽게 이어지도록 정보 구조를 다시 잡았습니다.

### 2. 업로드 UX와 내부 상태 동기화

드래그 앤 드롭 업로드를 붙이는 과정에서, 화면상으로는 삭제됐는데 내부 컴포넌트 상태가 남아 보이는 문제가 있었습니다.  
이를 해결하기 위해 업로드 컴포넌트를 제어형으로 바꾸고, 앱 상태와 화면 표시를 맞췄습니다.

### 3. 데모 배포 구조와 실서비스 구조의 균형

포트폴리오용 무료 배포를 고려하면 전체 비동기 구조를 그대로 운영하기가 부담이 있었습니다.  
그래서 현재는:

- 데모 배포: 동기 처리 기반
- 확장 구조: Redis + Python worker 고려

처럼 분리해, 시연 가능성과 아키텍처 확장성을 둘 다 설명할 수 있게 했습니다.

### 4. 배치 처리에서 메모리 사용량과 ZIP 안전성 문제

배치 업로드와 ZIP 다운로드를 붙이고 나니, 단순히 기능이 되는 것만으로는 부족했습니다.  
특히 다음 두 가지가 실제 서비스 관점에서 리스크였습니다.

- 결과 ZIP 다운로드 시 파일 전체를 메모리에 올리는 방식
- ZIP 업로드 시 압축 해제량 제한이 없는 방식

이를 보완하기 위해:

- 결과 ZIP 다운로드는 스트리밍 방식으로 변경
- ZIP 업로드는 엔트리 수, 엔트리당 최대 바이트, 전체 압축 해제 바이트를 제한

하도록 하드닝했습니다. 이 부분은 “대용량 이미지 처리 서비스”라는 문제 정의와도 직접 연결되는 개선이었습니다.

### 6. 데모 모드와 운영 모드의 경계를 명확히 한 점

초기에는 데모용 처리 경로와 비동기 워커 경로가 섞여 있어 실제로 어떤 모드가 운영 기준인지 설명하기 애매했습니다.
이를 정리하면서:

- worker 중심 처리 모드로 정리
- health endpoint에 processing mode, queue depth, storage provider, rate limit 노출
- queue publish retry, 업로드 rate limit 기본 구조 추가
- 프론트에서는 로그아웃 시 polling 요청 정리

하도록 바꾸었습니다. 이 작업은 기능 추가보다도 “어떤 모드에서 무엇이 실제로 운영 기준인가”를 분명하게 만드는 안정화 작업이었습니다.

## 성과로 보여줄 수 있는 부분

- SaaS형 랜딩 페이지, Pricing, Get Started 흐름, 로그인 후 대시보드형 워크스페이스 구현
- 이미지 업로드부터 결과 다운로드까지 하나의 흐름으로 연결된 서비스형 화면 구현
- 원본/결과 파일 크기와 절감률을 함께 제공하는 비용 관점의 결과 설계
- 단일 파일이 아닌 배치/ZIP 업로드 중심의 실무형 워크플로우 반영
- 마켓플레이스별 프리셋과 선택적 워터마크 적용 흐름 반영
- 인증, 업로드 보호, 작업 이력, 배치 다운로드까지 이어지는 구조 설계
- 스트리밍 다운로드와 ZIP 안전장치 추가로 대용량 파일 처리 시 메모리 리스크를 줄이도록 개선
- queue retry, 업로드 rate limit, health 기반 운영 상태 노출 등 운영형 보강

## 최적화한 지점과 끌어올린 수준

### 1. 이미지 최적화 결과를 숫자로 증명할 수 있게 설계

- 작업 결과 응답에 `sourceFileSizeBytes`, `resultFileSizeBytes`, `savedBytes`, `reductionRate`를 포함해, 단순히 "압축됨"이 아니라 `얼마나 줄었는지`를 수치로 설명할 수 있게 만들었습니다.
- 프런트 대시보드와 결과 패널에서도 같은 수치를 노출해, 비용 절감 포인트가 바로 보이도록 연결했습니다.
- 즉, 이 프로젝트는 이미지 편집 기능보다 `전송량과 저장 용량을 얼마나 줄였는가`를 설명하는 구조까지 포함합니다.

코드 근거:

- [ImageJobResponse.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/dto/ImageJobResponse.java:46)
- [ResultPanel.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/components/ResultPanel.jsx:177)
- [WorkspaceDashboard.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/components/WorkspaceDashboard.jsx:38)

### 2. 대용량 업로드 시 메모리와 압축 폭탄 리스크를 방어

- 배치 업로드는 최대 `10개` 파일로 제한했습니다.
- ZIP 업로드는 최대 `32개 엔트리`, 엔트리당 최대 `20MB`, 전체 압축 해제 기준 최대 `50MB`로 제한했습니다.
- ZIP 다운로드는 결과 파일을 한 번에 메모리에 모으지 않고 `StreamingResponseBody + ZipOutputStream`으로 바로 흘려보내도록 바꿨습니다.

이 부분은 포폴에서 `대용량 처리`를 어떻게 고민했는지 보여주는 핵심입니다. 단순히 업로드가 되는 수준이 아니라, 많이 올렸을 때 서버가 어디서 터질 수 있는지를 먼저 보고 제한과 스트리밍을 넣었습니다.

코드 근거:

- [application.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/resources/application.yaml:43)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:63)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:419)
- [ImageJobService.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/domain/image/ImageJobService.java:611)

### 3. 동기형 데모 툴이 아니라 비동기 운영 구조로 끌어올림

- 이미지 처리를 API 서버에서 직접 끝내지 않고, `Redis queue + Python worker`로 분리했습니다.
- 큐 publish는 `3회 retry`, `200ms` 간격으로 재시도하도록 넣어 일시적인 Redis 오류에 조금 더 버틸 수 있게 했습니다.
- health 응답에는 `processing mode`, `queue enabled`, `queue depth`, `storage provider`, `upload rate limit`를 포함해 운영 상태를 바로 확인할 수 있게 했습니다.

즉, "기능이 된다"에서 끝나지 않고 `운영 중 어떤 정보가 필요할지`까지 포함한 구조로 끌어올렸습니다.

코드 근거:

- [ImageJobQueuePublisher.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/queue/ImageJobQueuePublisher.java:15)
- [HealthController.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/api/HealthController.java:24)
- [worker.py](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/image-agent/worker.py:22)

### 4. 무제한 호출이 아닌 운영형 보호 장치를 추가

- 업로드 API에는 분당 `30회` 기본 rate limit를 두었습니다.
- 워터마크는 모든 배치에 강제하지 않고, 사용자가 `이번 배치에 워터마크 적용`을 켠 경우에만 요청에 포함되도록 바꿨습니다.
- 프런트에서는 로그아웃 시 polling을 정리해 불필요한 상태 조회 요청이 남지 않도록 처리했습니다.

이 개선은 성능 최적화라기보다 `불필요한 요청과 혼선을 줄이는 안정화`에 가깝습니다.

코드 근거:

- [application.yaml](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/resources/application.yaml:50)
- [ImageJobController.java](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/backend/src/main/java/com/imageflow/backend/api/ImageJobController.java:45)
- [App.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/App.jsx:448)
- [WatermarkStudio.jsx](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/frontend/src/components/WatermarkStudio.jsx:424)

## 서류 합격용으로 강조할 수 있는 노력

- 기능 추가보다 먼저 `어디서 비용이 발생하고 어디서 장애가 날 수 있는지`를 기준으로 구조를 재정리했습니다.
- 화면도 단순 업로드 툴이 아니라 `랜딩 -> Pricing -> Get Started -> 로그인 후 운영 대시보드` 흐름으로 바꿔, 제품 사고와 UX 설계 역량이 드러나도록 다듬었습니다.
- 비동기 워커와 로컬/스토리지 분리 구조를 붙이면서, 실제로 Docker 워커와 호스트 저장 경로가 어긋나는 문제까지 직접 디버깅하고 수정했습니다.
- 결과적으로 이 프로젝트는 "이미지 압축 기능 구현" 수준이 아니라, `커머스 이미지 운영 워크플로우를 서비스 형태로 재설계하고 운영 리스크까지 보완한 작업`이라고 설명할 수 있습니다.

## 이력서용 핵심 문장

- 커머스 이미지 운영 과정에서 발생하는 전송량·저장소 비용 문제를 줄이기 위해, 원본 대비 결과 파일 크기와 절감률을 노출하는 이미지 최적화 SaaS 워크스페이스를 설계·구현했습니다.
- 대량 업로드 리스크를 줄이기 위해 배치 10개 제한, ZIP 32엔트리/20MB/50MB 제한, 스트리밍 ZIP 다운로드를 적용해 메모리 및 압축 해제 부담을 제어했습니다.
- Redis queue + Python worker 기반 비동기 처리 구조, 3회 retry, 분당 30회 업로드 rate limit, health 기반 운영 상태 노출을 통해 데모 수준을 넘는 운영형 구조를 정리했습니다.

## 제출용 최종 정리

이 프로젝트는 `이미지 압축 기능 구현`보다 `커머스 이미지 운영 과정에서 발생하는 비용과 반복 작업 문제를 서비스 구조로 정리한 작업`으로 설명하는 편이 가장 강합니다.

- 비용 문제는 `savedBytes`, `reductionRate`, 절감 예시 수치로 설명
- 대용량 처리 리스크는 `10개 배치`, `32엔트리`, `20MB`, `50MB`, `30rpm`, `3회 retry`로 설명
- 제품 완성도는 `랜딩 -> 로그인 -> 워크스페이스 -> 배치 처리 -> 결과 검수 -> ZIP 다운로드` 흐름으로 설명

즉, 포트폴리오에서 이 프로젝트의 핵심 메시지는:

`대용량 상품 이미지 운영 문제를 사용자 워크플로우와 시스템 보호 장치까지 포함해 SaaS 형태로 재설계했다`

로 정리하는 것이 가장 적절합니다.

## 아쉬운 점 / 다음 단계

- 최근 작업 히스토리의 상태 필터링과 별도 Jobs 화면이 더 필요함
- 이미지 비교 뷰를 더 정교하게 다듬을 필요가 있음
- 로컬 저장 대신 Cloudflare R2 / S3 중심 구조를 완전히 마이그레이션할 필요가 있음
- retry / rate limit는 기본 구조까지 반영했지만, observability는 더 확장할 여지가 있음
- billing placeholder 또는 실제 결제 연동을 붙이면 SaaS 완성도가 더 높아질 수 있음

## 이력서 / 포트폴리오용 문장 예시

### 한 줄 소개

- 대용량 상품 이미지 업로드 환경에서 트래픽 및 스토리지 비용 절감을 목표로 한 이미지 최적화 워크스페이스를 설계하고 구현했습니다.
- 로그인 전 SaaS 랜딩, Pricing, Get Started 흐름과 로그인 후 운영 대시보드형 워크스페이스를 분리해 서비스형 제품 경험을 구성했습니다.

### 핵심 bullet

- React, Ant Design, Spring Boot, PostgreSQL 기반으로 인증형 이미지 최적화 서비스 프로토타입을 개발했습니다.
- JWT 인증, 다중 이미지 업로드, ZIP 업로드, 수동 크롭, 선택적 워터마크, 결과 ZIP 다운로드 기능을 구현했습니다.
- 원본 대비 결과 파일 크기, 절감량, 절감률을 함께 제공해 비용 관점에서 최적화 효과를 설명할 수 있도록 설계했습니다.
- Redis Queue + Python worker 기반 비동기 처리 구조와 queue retry, upload rate limit, health monitoring 항목을 포함한 운영 확장 구조를 설계하고 보강했습니다.

## 제출 시 함께 보여주면 좋은 것

- 서비스 메인 화면 캡처
- 업로드 -> 최적화 -> 결과 검수 -> 다운로드 흐름 캡처
- 원본/결과 크기 비교 예시
- 간단한 시스템 구조도
