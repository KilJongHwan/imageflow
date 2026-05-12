# Product Overview

이 문서는 [README.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/README.md:1) 내용을 기준으로 요약한 제품 개요 문서입니다.

## 프로젝트 성격

ImageFlow는 커머스 상품 이미지를 정해진 규격으로 빠르게 가공하기 위한 이미지 최적화 워크스페이스입니다.

## 현재 구현 범위

- 로그인 전 랜딩, Pricing, Get Started
- 로그인 후 대시보드형 워크스페이스
- 단일/다중 이미지 업로드
- ZIP 업로드
- 수동 크롭, center-crop, fit
- 리사이즈 및 품질 조절
- 선택적 워터마크
- 결과 미리보기와 배치 ZIP 다운로드
- Redis + Python worker 기반 비동기 처리 구조

## 보호 장치

- 배치 최대 `10개`
- ZIP 최대 `32개 엔트리`
- ZIP 엔트리당 최대 `50MB`
- ZIP 전체 압축 해제 기준 최대 `200MB`
- 업로드 rate limit `30 rpm`
- queue backlog 상한 `100`
- worker concurrency `3`

## 비고

세부 실행 방법은 [deploy-guide.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/deploy-guide.md:1),  
문제 해결 과정은 [portfolio-case-study.md](/abs/path/c:/Users/tsline/IdeaProjects/imageflow/docs/portfolio-case-study.md:1) 에 정리되어 있습니다.
