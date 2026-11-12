# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다.
형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르고, 버전은 `app/build.gradle.kts`의 `versionName`과 맞춥니다.

## [Unreleased]

### Added
- ktlint 코드 스타일 검사(`./gradlew ktlintCheck`, `./gradlew ktlintFormat`)
- `.editorconfig`, `.gitattributes`, `CODEOWNERS`, `CHANGELOG.md`

## [1.0.0] - 2026-11-12

### Added
- 할 일 조회·추가·저장·수정·삭제 (F1.1 ~ F1.5)
- 상태별 표시: 완료는 취소선 + 흐리게, 진행 중은 `[진행 중]` (F1.6)
- 할 일 상태 3단계 (할 일 / 진행 중 / 완료)와 상태별 필터 (개수 표시)
- 항목별 이동 버튼 `시작 ▶` / `완료 ▶`
- 체크박스로 여러 개 선택 후 일괄 삭제
- 마감일 달력(DatePicker), 오늘 이전 날짜 선택 금지 (편집 시 이미 지난 원래 마감일은 유지 가능)
- 저장·수정·삭제·입력 오류 Toast
- 추가·편집 화면 상단 `<` 뒤로 버튼
- 테마 색상 (`#52677A`, 삭제 `#A6533C`)
- 단위 테스트 22개, `docs/` 설계 문서
