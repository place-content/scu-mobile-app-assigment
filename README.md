# 할 일 관리 앱

모바일앱제작프로그래밍 과제. 한 기기에서 할 일을 추가·조회·수정·삭제하는 Android 앱입니다.
필수 기능 F1.1~F1.6을 모두 구현했고, 추가 기능으로 **할 일 상태 3단계(할 일 / 진행 중 / 완료) + 상태별 필터 + 한 번에 다음 단계로 넘기는 이동 버튼**, 그리고 **체크박스 선택 일괄 삭제**를 넣었습니다.

> 📚 설계·흐름·개발 규칙·결정 기록·보고서 가이드 등 자세한 문서는 [docs/](docs/README.md)에 있습니다.

- 언어: Kotlin
- UI: XML 레이아웃 + ViewBinding
- 구조: MVVM (Activity → ViewModel → Repository → Room)
- 저장소: Room (기기 내부 SQLite)
- 최소 SDK 24 / 타깃 SDK 37

---

## 1. 기능 요구사항 구현 내역

| 코드 | 기능 | 구현 내용 | 관련 파일 |
|---|---|---|---|
| F1.1 | 할 일 조회 | DB 목록을 Flow로 관찰해서 변경될 때마다 RecyclerView에 자동 반영 | `TodoListActivity`, `TodoAdapter`, `TodoDao.observeAll()` |
| F1.2 | 할 일 입력 | 추가 화면에서 제목(필수), 메모, 마감일 입력. 마감일은 달력(DatePicker)에서 선택하며 오늘 이전 날짜는 고를 수 없음. 제목이 비었으면 저장하지 않고 Toast로 알림 | `AddTodoActivity`, `TodoInput` |
| F1.3 | 할 일 저장 | Repository를 거쳐 Room에 insert, "저장했습니다" Toast | `TodoRepository.add()`, `TodoDao.insert()` |
| F1.4 | 할 일 수정 | 편집 화면에 기존 값을 불러와 표시, 제목·메모·마감일·상태 수정 후 update, "수정했습니다" Toast | `EditTodoActivity`, `TodoRepository.update()` |
| F1.5 | 할 일 삭제 | 길게 누르기 메뉴 / 편집 화면 삭제 버튼 / 체크박스로 여러 개 선택 후 일괄 삭제 → 모두 확인 대화상자 후 delete | `TodoListActivity.confirmDelete()`, `confirmDeleteSelected()`, `EditTodoActivity.confirmDelete()` |
| F1.6 | 완료 여부 표시 | 완료: 취소선 + 흐리게 / 진행 중: 제목 앞에 `[진행 중]` / 할 일: 기본 | `TodoAdapter.VH.bind()` |

## 2. 추가 기능: 상태 3단계 + 필터 + 이동 버튼

### 내용
- 교안의 `done: Boolean`(완료/미완료) 대신 `status`를 세 가지로 관리합니다.
  - `TODO` 할 일 → `IN_PROGRESS` 진행 중 → `DONE` 완료
- 목록 상단의 필터 버튼으로 원하는 상태만 볼 수 있습니다.
  - `전체 (n)` `할 일 (n)` `진행 중 (n)` `완료 (n)` — 각 상태의 개수가 함께 표시됨
- 상태를 바꾸는 방법
  - **이동 버튼 (한 번 탭)**: 각 항목 오른쪽 버튼으로 다음 단계로 이동
    - 할 일 `시작 ▶` → 진행 중 `완료 ▶` → 완료 (완료된 항목은 버튼 없음)
    - 완료를 다시 되돌리려면 길게 누르기 → `상태 변경` 또는 편집 화면에서 변경
  - 길게 누르기 → `상태 변경` → 세 가지 중 선택
  - 편집 화면의 상태 선택(Spinner)

### 넣은 이유
완료/미완료 두 가지로만 나누면 "시작은 했지만 아직 안 끝난 일"을 구분할 수 없습니다.
팀 과제나 집안일처럼 여러 일을 동시에 진행할 때는 지금 손대고 있는 일만 따로 보는 게 편해서, 상태를 하나 더 두고 필터로 골라 볼 수 있게 했습니다.

### 구현 방식
- `TodoStatus` enum을 Room 엔티티의 컬럼으로 저장 (Room이 enum 이름 문자열로 저장)
- `TodoFilter.apply(list)`는 순수 함수로 분리해서 단위 테스트 작성
- ViewModel에서 `combine(전체 목록 Flow, 현재 필터 StateFlow)`로 화면에 보여줄 목록을 만듦
  → 필터를 바꾸거나 데이터가 바뀌면 목록이 자동으로 다시 계산됨
- 필터 값은 ViewModel에 있어서 화면을 회전해도 유지됨

## 3. 추가 기능: 체크박스 선택 일괄 삭제

### 내용
- 목록 왼쪽 체크박스로 할 일을 여러 개 선택할 수 있습니다.
- 하나라도 선택하면 하단 `+ 추가` 버튼 자리에 `[선택 해제] [선택 삭제 (n)]`이 나타납니다.
- `선택 삭제` → "선택한 n개를 삭제할까요?" 확인 → 한 번에 삭제
- 선택 중에 뒤로가기를 누르면 앱이 닫히지 않고 선택만 해제됩니다.
- 필터를 바꾸면 선택이 초기화됩니다. (화면에 안 보이는 항목이 같이 지워지는 것을 막기 위함)

### 구현 방식
- 선택된 id 집합을 ViewModel의 `StateFlow<Set<Long>>`로 보관 → 화면 회전해도 유지
- `combine(전체 목록, 필터, 선택)` → `TodoRow(할 일, 선택 여부)` 목록을 만들어 어댑터에 전달
- DAO에 `DELETE FROM todos WHERE id IN (:ids)` 쿼리를 추가해서 한 번의 쿼리로 삭제
- 교안에서는 체크박스가 완료/해제 용도였지만, 완료 처리는 이동 버튼(`완료 ▶`)으로 하도록 역할을 나눔

## 4. 화면 구성과 사용 흐름

| 화면 | 구성 |
|---|---|
| ① `TodoListActivity` (목록) | 제목, 필터 버튼 4개, 할 일 목록, 빈 목록 안내, `+ 추가` 버튼 (선택 중에는 `선택 해제` · `선택 삭제 (n)`) |
| ② `AddTodoActivity` (추가) | 상단 `<` 뒤로(취소) / 제목, 메모, 마감일(누르면 달력) / 저장 |
| ③ `EditTodoActivity` (편집) | 상단 `<` 뒤로(취소) / 제목, 메모, 마감일(누르면 달력), 상태 선택 / 삭제, 저장 |

| 코드 | 흐름 |
|---|---|
| UC01 | ①에서 `+ 추가` → ② |
| UC02 | ①에서 이동 버튼 클릭 또는 길게 누르기 → `상태 변경` (즉시 반영) |
| UC03 | ①에서 항목 클릭 또는 길게 누르기 → `수정` → ③ |
| UC04 | ①에서 길게 누르기 → `삭제` → 확인 후 삭제 |
| UC05 | ②/③에서 저장, 또는 상단 `<`·기기 뒤로가기(취소) → ①로 복귀 |
| UC06 (추가) | ①에서 필터 버튼 선택 → 해당 상태의 할 일만 표시 |
| UC07 (추가) | ①에서 체크박스로 여러 개 선택 → `선택 삭제 (n)` → 확인 후 일괄 삭제 |

화면 간 데이터는 `registerForActivityResult` + Intent extra로 주고받습니다.
편집 화면에는 id만 넘기고, 편집 화면이 DB에서 기존 값을 직접 읽어 옵니다.

## 5. 앱 구조

```
com.k0h5.todolist
├── data
│   ├── local
│   │   ├── Todo.kt            Room 엔티티 (할 일 한 건)
│   │   ├── TodoStatus.kt      상태 enum (할 일 / 진행 중 / 완료)
│   │   ├── TodoDao.kt         조회·추가·수정·삭제 쿼리
│   │   └── TodoDatabase.kt    Room DB 싱글톤
│   └── TodoRepository.kt      UI 계층에 데이터 제공
├── vm
│   ├── TodoListViewModel.kt   화면 상태(목록, 필터, 개수) 보관, 이벤트 처리
│   ├── TodoFilter.kt          필터 종류와 필터링 함수
│   ├── TodoRow.kt             목록 한 줄 (할 일 + 선택 여부)
│   └── TodoInput.kt           입력값 검증 (제목 필수, 마감일 형식, 과거 날짜 금지)
└── ui
    ├── TodoListActivity.kt    ① 목록
    ├── AddTodoActivity.kt     ② 추가
    ├── EditTodoActivity.kt    ③ 편집
    ├── TodoAdapter.kt         RecyclerView 어댑터 (ListAdapter + DiffUtil)
    ├── InputErrors.kt         검증 오류 → 문자열 리소스
    ├── DuePicker.kt           마감일 칸 → 달력(DatePickerDialog) 연결
    ├── Dialogs.kt             삭제 확인 창 ("삭제" 글자를 삭제 색으로)
    └── Insets.kt              상태바/키보드 영역 padding 처리

res/layout
├── activity_todo_list.xml
├── activity_add_todo.xml
├── activity_edit_todo.xml
└── item_todo.xml
```

데이터 흐름
```
사용자 입력 → Activity → ViewModel → Repository → DAO → Room(SQLite)
                 ↑                                         │
                 └──────── Flow로 변경 사항 전달 ───────────┘
```

## 6. 데이터 모델

`todos` 테이블

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | Long | 기본 키, 자동 증가 |
| title | String | 제목 (필수) |
| note | String | 메모 (선택, 기본 "") |
| dueDate | String | 마감일 `YYYY-MM-DD` (선택, 기본 "") |
| status | TodoStatus | `TODO` / `IN_PROGRESS` / `DONE` (기본 `TODO`) |
| createdAt | Long | 생성 시각 (ms) |

목록은 최근에 추가한 순서(id 내림차순)로 보여 줍니다.

## 7. 실행 방법

1. Android Studio에서 프로젝트 폴더 열기
2. Gradle Sync
3. 에뮬레이터 또는 기기 선택 후 Run (`app`)

터미널에서 빌드/테스트
```bash
./gradlew :app:assembleDebug        # 디버그 APK 빌드
./gradlew :app:testDebugUnitTest    # 단위 테스트
./gradlew :app:lintDebug            # 린트
./gradlew ktlintCheck               # Kotlin 코드 스타일 검사
./gradlew ktlintFormat              # Kotlin 코드 스타일 자동 정리
```

## 8. 테스트

### 단위 테스트 (`app/src/test`)
| 파일 | 확인 내용 |
|---|---|
| `TodoFilterTest` | 필터별 결과, 순서 유지, 빈 목록, 상태별 개수 |
| `TodoInputTest` | 빈 제목 거부, 마감일 형식·존재하지 않는 날짜(2/30 등) 거부, 과거 마감일 거부 (편집 시 이미 지난 원래 마감일은 유지 허용) |
| `TodoStatusTest` | 이동 버튼(다음 단계) 상태 변화, 완료 다음은 없음 |
| `TodoRowTest` | 선택한 id만 선택 표시, 목록에 없는 id 무시 |
| `DuePickerTest` | 달력에서 고른 날짜 ↔ `YYYY-MM-DD` 변환 |

### 직접 확인 체크리스트
- [ ] 앱 첫 실행 시 "할 일이 없습니다"가 보인다
- [ ] `+ 추가` → 제목 없이 저장하면 막히고 Toast가 뜬다
- [ ] 마감일 칸을 누르면 키보드 대신 달력이 뜨고, 고른 날짜가 `YYYY-MM-DD`로 채워진다
- [ ] 날짜가 있는 상태에서 다시 누르면 그 날짜가 선택된 채로 달력이 열린다
- [ ] 달력의 `지우기`를 누르면 마감일이 비워진다
- [ ] 추가 화면 달력에서 오늘 이전 날짜는 선택할 수 없다 (회색 처리)
- [ ] 목록에서 날짜가 `마감 2026-12-01`처럼 "마감" 라벨과 함께 보인다
- [ ] 제목/메모/마감일 입력 후 저장하면 목록 맨 위에 나타난다 + "저장했습니다" Toast (F1.2, F1.3)
- [ ] 추가 화면에서 상단 `<`이나 기기 뒤로가기를 누르면 아무것도 추가되지 않는다
- [ ] 앱을 완전히 종료했다가 다시 켜도 목록이 남아 있다 (F1.1, F1.3)
- [ ] 할 일 항목의 `시작 ▶`을 누르면 진행 중(`[진행 중]` 표시)이 되고 버튼이 `완료 ▶`로 바뀐다
- [ ] `완료 ▶`를 누르면 완료(취소선 + 흐리게, F1.6)가 되고 이동 버튼이 사라진다
- [ ] 길게 누르기 → 상태 변경 → 진행 중 선택 시 제목 앞에 `[진행 중]`이 붙는다
- [ ] 항목을 누르면 편집 화면에 기존 값이 채워져 있다
- [ ] 편집 화면에서 내용·상태를 바꾸고 저장하면 목록에 반영된다 + "수정했습니다" Toast (F1.4)
- [ ] 편집 화면에서 상단 `<`이나 기기 뒤로가기를 누르면 변경되지 않는다
- [ ] 길게 누르기 → 삭제 → 확인 대화상자에서 취소하면 그대로, 삭제하면 사라진다 (F1.5)
- [ ] 편집 화면의 삭제 버튼도 확인 후 삭제된다
- [ ] 체크박스를 하나 이상 선택하면 하단에 `선택 해제` · `선택 삭제 (n)`이 나오고 n이 선택 개수와 같다
- [ ] `선택 삭제` → 확인 → 선택한 항목만 한 번에 사라진다
- [ ] `선택 해제` 또는 뒤로가기를 누르면 선택이 풀리고 `+ 추가`가 다시 보인다
- [ ] 선택한 상태에서 필터를 바꾸면 선택이 초기화된다
- [ ] 필터 버튼을 누르면 해당 상태의 할 일만 보이고, 괄호 안 개수가 맞다
- [ ] "진행 중" 필터를 켠 상태에서 항목을 완료로 바꾸면 목록에서 빠지고 개수가 바뀐다
- [ ] 화면을 회전해도 선택한 필터와 목록이 유지된다
