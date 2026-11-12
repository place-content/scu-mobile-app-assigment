# 06. 개발 규칙

코드를 고치거나 기능을 추가할 때 지키는 규칙입니다.

## 1. 폴더(패키지) 구조
교안의 `data / ui / vm` 구조를 그대로 씁니다.

| 패키지 | 넣는 것 | 넣지 않는 것 |
|---|---|---|
| `data.local` | Room 엔티티, DAO, DB, enum | 화면 코드 |
| `data` | Repository | Activity, View |
| `vm` | ViewModel, 그리고 **안드로이드에 의존하지 않는 순수 로직** (필터, 검증 등) | `Context`, View, 문자열 리소스 |
| `ui` | Activity, Adapter, 화면 보조 함수(달력, 다이얼로그, 여백) | DB 직접 접근 |

의존 방향은 `ui → vm → data` 한 방향만 허용합니다. (`data`가 `ui`를 import하면 안 됨)

## 2. 이름 규칙
| 대상 | 규칙 | 예 |
|---|---|---|
| 화면 | `~Activity` | `TodoListActivity` |
| 레이아웃 | `activity_~.xml`, 목록 한 줄은 `item_~.xml` | `activity_add_todo.xml`, `item_todo.xml` |
| View ID | **교안과 같은 이름** + 종류 접두사 | `rvTodos`, `btnAddTodo`, `etTitle`, `etNote`, `etDue`, `btnSave`, `tvItemTitle` |
| ID 접두사 | `btn` 버튼, `et` 입력칸, `tv` 텍스트, `rv` 목록, `cb` 체크박스, `rb` 라디오, `rg` 라디오 그룹, `sp` 스피너 | `cbSelect`, `rgFilter`, `spStatus` |
| 화면 루트 ID | 교안처럼 `~View` | `todoListView`, `addTodoView`, `editTodoView` |
| Intent 키 | `EXTRA_~` 상수 (화면 클래스의 `companion object`) | `EditTodoActivity.EXTRA_ID` |
| 문자열 키 | `title_` 화면 제목, `btn_` 버튼, `hint_` 입력 힌트, `label_` 라벨, `msg_` Toast, `confirm_` 확인 창, `menu_` 메뉴 | `msg_saved`, `confirm_delete` |

## 3. 리소스 규칙
- **화면 글자는 모두 `res/values/strings.xml`** 에 둡니다. 코드·XML에 한글을 직접 쓰지 않음 (React의 i18n 메시지 파일처럼)
  - 예외: 미리보기용 `tools:text`는 직접 써도 됨 (앱 실행 시엔 안 보임)
- **색은 `res/values/colors.xml` + `themes.xml`** 에서만 정합니다

| 색 이름 | 값 | 쓰는 곳 |
|---|---|---|
| `primary` | `#52677A` | 테마 `colorPrimary`, `colorSecondary` → 버튼, 체크박스, 라디오, 스피너 |
| `primary_dark` | `#3E4F5E` | 테마 `colorPrimaryVariant`, `colorSecondaryVariant` |
| `danger` | `#A6533C` | 삭제 버튼(`app:backgroundTint`), 삭제 확인 창의 `삭제` 글자 |
| `white` | `#FFFFFF` | 버튼 위 글자 (`colorOnPrimary`) |

- 새 버튼은 기본 스타일을 그대로 쓰고, 삭제처럼 위험한 동작만 `danger`를 씁니다
- UI는 기본 위젯 위주로 단순하게 (그라데이션, 커스텀 배경, 그림자 등은 쓰지 않음)

## 4. 코드 규칙
- **Activity에는 로직을 넣지 않습니다.** 버튼을 누르면 ViewModel 함수를 부르기만 함
- **계산·판단 로직은 순수 함수로 분리**해서 `vm/`에 두고 단위 테스트를 씁니다
  - 예: `TodoFilter.apply()`, `TodoRow.of()`, `TodoInput.validate()`, `TodoStatus.next()`
- DB 작업은 `suspend` 함수 + `viewModelScope.launch` 안에서 (메인 스레드에서 DB 호출 금지 — Room이 막음)
- 목록 갱신은 직접 하지 않고 Flow 구독으로 자동 반영 (DB가 바뀌면 화면이 따라옴)
- 삭제처럼 되돌릴 수 없는 동작은 **반드시 확인 창**을 거치고, 확인 창은 `showDeleteConfirm()`으로 띄웁니다
- 사용자 동작의 결과는 Toast로 알립니다 (저장·수정·삭제·입력 오류)
- 입력 검증은 화면(저장 버튼)과 입력 수단(달력 `minDate`) 양쪽에서 같은 규칙을 씁니다
- 주석은 "왜"가 필요한 곳에만 짧게
- 교안 패턴을 유지: `registerForActivityResult` + Intent extra로 화면 간 데이터 전달, `TodoDatabase.get()` 싱글톤

## 5. 빌드 · 테스트 · 린트
프로젝트 루트에서 실행합니다. (`./gradlew` = `yarn` 같은 실행기)

| 명령 | 하는 일 | React로 치면 |
|---|---|---|
| `./gradlew :app:assembleDebug` | 디버그 APK 빌드 (`app/build/outputs/apk/debug/app-debug.apk`) | `next build` |
| `./gradlew :app:testDebugUnitTest` | 단위 테스트 | `yarn test` |
| `./gradlew :app:lintDebug` | 린트 (`app/build/reports/lint-results-debug.html`) | `yarn lint` |

변경 후에는 세 개 모두 통과해야 합니다. 린트는 **오류 0개**가 기준입니다. (라이브러리 새 버전 안내, 버튼 스타일 권장 같은 경고는 남아 있음)

Android Studio에서는 Gradle Sync 후 상단 ▶ Run으로 에뮬레이터/기기에 설치합니다.

## 6. 새 기능 추가 절차
1. **데이터가 필요하면** `Todo.kt`에 필드 추가 → `TodoDatabase`의 `version` 올리기
   - 지금은 마이그레이션 코드가 없으므로 버전을 올리면 기존 데이터 처리가 필요함 (개발 중엔 앱 삭제 후 재설치로 해결)
2. 쿼리가 필요하면 `TodoDao` → `TodoRepository`에 함수 추가
3. 판단 로직은 `vm/`에 순수 함수로 만들고 **테스트 먼저 작성**
4. `TodoListViewModel`에 상태(`StateFlow`)나 이벤트 함수 추가
5. 화면 글자는 `strings.xml`, 레이아웃은 `res/layout`, 연결은 Activity에서
6. 빌드 · 테스트 · 린트 통과 확인
7. `README.md`와 `docs/`(특히 [07-testing.md](07-testing.md) 체크리스트, [08-decisions.md](08-decisions.md)) 갱신
