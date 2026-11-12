# 05. 데이터 모델

## 저장 위치
- Room 라이브러리로 기기 내부 SQLite 파일 `todo.db`에 저장 (`TodoDatabase.get(context)`)
- 앱을 껐다 켜도 남아 있고, 앱을 삭제하면 같이 지워짐
- 서버/네트워크 없음 (교안의 심화 F2.x는 미구현)

## `todos` 테이블 (`data/local/Todo.kt`)

```kotlin
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueDate: String = "",
    val status: TodoStatus = TodoStatus.TODO,
    val createdAt: Long = System.currentTimeMillis()
)
```

| 컬럼 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `id` | Long | 자동 | 자동 증가 | 기본 키 |
| `title` | String | 예 | – | 제목. 앞뒤 공백 제거 후 저장 |
| `note` | String | 아니오 | `""` | 메모 |
| `dueDate` | String | 아니오 | `""` | 마감일 `YYYY-MM-DD`. 없으면 빈 문자열 |
| `status` | TodoStatus | 예 | `TODO` | 상태. DB에는 enum 이름(`"TODO"` 등) 문자열로 저장됨 |
| `createdAt` | Long | 자동 | 현재 시각(ms) | 만든 시각 |

정렬: `ORDER BY id DESC` → 최근에 추가한 것이 위.

마감일을 문자열로 둔 이유: `YYYY-MM-DD`는 문자열 비교 순서가 날짜 순서와 같아서 `"2026-11-01" < "2026-12-01"`처럼 바로 비교할 수 있고, 화면에도 그대로 보여주면 되기 때문입니다.

## 상태 (`data/local/TodoStatus.kt`)

| 값 | 화면 라벨 | `next()` |
|---|---|---|
| `TODO` | 할 일 | `IN_PROGRESS` |
| `IN_PROGRESS` | 진행 중 | `DONE` |
| `DONE` | 완료 | `null` (다음 없음) |

## DAO 쿼리 (`data/local/TodoDao.kt`)

| 함수 | SQL / 동작 | 쓰는 곳 |
|---|---|---|
| `observeAll(): Flow<List<Todo>>` | `SELECT * FROM todos ORDER BY id DESC` (구독형) | 목록 (F1.1) |
| `insert(todo)` | INSERT | 추가 (F1.3) |
| `update(todo)` | UPDATE | 수정, 상태 변경 (F1.4) |
| `delete(todo)` | DELETE 한 건 | 삭제 (F1.5) |
| `deleteByIds(ids)` | `DELETE FROM todos WHERE id IN (:ids)` | 선택 일괄 삭제 |
| `getById(id)` | `SELECT * FROM todos WHERE id = :id` | 편집 화면 기존 값, 수정 전 조회 |

## 입력 검증 규칙 (`vm/TodoInput.kt`)

`TodoInput.validate(title, dueDate, earliestDue)` → 문제가 없으면 `null`, 있으면 오류 종류.

| 순서 | 규칙 | 오류 | 안내 문구 |
|---|---|---|---|
| 1 | 제목이 비어 있으면 안 됨 (공백만 있어도 비어 있는 것) | `TITLE_REQUIRED` | 제목을 입력하세요 |
| 2 | 마감일이 비어 있으면 통과 (선택 항목) | – | – |
| 3 | 마감일은 `YYYY-MM-DD` 형식이고 실제 있는 날짜여야 함 (2/30 같은 날짜 거부) | `DUE_INVALID` | 마감일은 YYYY-MM-DD 형식으로 입력하세요 |
| 4 | 마감일이 `earliestDue`보다 이전이면 안 됨 | `DUE_PAST` | 마감일은 오늘부터 선택할 수 있습니다 |

`earliestDue`(고를 수 있는 가장 이른 날짜)
| 화면 | 값 |
|---|---|
| 추가 | 오늘 (`todayDue()`) |
| 편집 | `TodoInput.earliestDue(오늘, 원래 마감일)` → 원래 마감일이 이미 지났으면 원래 마감일, 아니면 오늘 |

예시 (오늘 = 2026-11-12)
| 원래 마감일 | 고를 수 있는 가장 이른 날 | 2026-10-01로 저장 | 2026-09-30으로 저장 |
|---|---|---|---|
| (추가 화면) | 2026-11-12 | ✗ | ✗ |
| 2026-12-01 | 2026-11-12 | ✗ | ✗ |
| 2026-10-01 (이미 지남) | 2026-10-01 | ✓ (그대로 유지) | ✗ |

같은 규칙이 두 군데서 적용됩니다.
1. 달력: `DatePicker.minDate`로 이전 날짜를 회색 처리 (`DuePicker.kt`)
2. 저장 버튼: `TodoInput.validate()`로 한 번 더 검사

Repository도 제목이 비면 저장하지 않아서(`if (t.isBlank()) return`) 화면 검증을 건너뛰더라도 빈 제목이 DB에 들어가지 않습니다.
