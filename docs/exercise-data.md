# 종목 데이터 조사

조사일: 2026-11-17. 이슈 [#5](https://github.com/zzunsik/next-set/issues/5).

종목 목록을 앱에 내장할 수 있는지, 쓸 수 있는 공개 데이터가 무엇인지, 한국어 이름을 어떻게 붙일지 확인했다. 숫자는 조사일에 데이터를 직접 내려받아 센 값이다.

## 추천안

이 조사가 내는 추천이다. 무엇을 넣을지는 요구사항 정의([#6](https://github.com/zzunsik/next-set/issues/6))에서 확정한다.

- 공개 데이터를 그대로 내장하지 않기를 추천한다. 자주 쓰는 종목 100~150개를 **직접 작성**해 내장하고, 없는 종목은 사용자가 추가하게 한다.
- 한국어 종목 이름은 어느 공개 데이터에도 없다. 내장하려면 직접 붙여야 한다.
- 종목 이미지는 v1에 넣지 않기를 추천한다. `free-exercise-db`의 이미지는 출처와 저작권이 확인되지 않았다.
- `free-exercise-db`는 종목을 고르고 부위·장비 분류를 대조하는 참고 자료로만 쓰기를 추천한다.

이 문서의 판단은 법률 자문이 아니다. 공개 앱에 넣어도 문제가 생기지 않을 쪽을 골랐다.

## 후보 비교

| 후보 | 종목 수 | 라이선스 | 한국어 | 이미지 | 판단 |
|---|---|---|---|---|---|
| [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | 876 | Unlicense | 없음 | 1,746장(94.1MB), 저작권 불명 | 참고 자료로만 쓴다 |
| [wger](https://wger.de/api/v2/exerciseinfo/) | 973 | 종목마다 다름. CC-BY-SA가 대부분 | 없음 | 287종목 394장, CC-BY-SA 위주 | 쓰지 않는다 |
| [ExerciseDB](https://exercisedb.dev/) | 1,500(무료 판) | 독점 | 미확인 | GIF 제공 | 제외한다 |

## free-exercise-db

`dist/exercises.json` 하나에 876종목이 들어 있다. 파일 크기는 약 1MB다.

필드는 `id`, `name`, `force`, `level`, `mechanic`, `equipment`, `primaryMuscles`, `secondaryMuscles`, `instructions`, `category`, `images`다.

| 항목 | 값 |
|---|---|
| 분류(`category`) | strength 584, stretching 123, plyometrics 61, powerlifting 38, olympic weightlifting 35, strongman 21, cardio 14 |
| 장비(`equipment`) | barbell 170, dumbbell 123, other 122, body only 111, cable 81, machine 67, kettlebells 56, bands 20 외. 비어 있는 종목 77 |
| 주동근(`primaryMuscles`) | 17종. quadriceps 148, shoulders 129, abdominals 93, chest 84, hamstrings 79, triceps 73, biceps 53 순 |
| 난이도(`level`) | beginner 525, intermediate 294, expert 57 |
| 비어 있는 값 | `force` 30, `mechanic` 87, `equipment` 77, `instructions` 5 |
| 언어 | 영어뿐이다. 한글이 들어간 항목은 0개다 |

쓰기 어려운 점이 세 가지 있다.

**이미지를 쓸 수 없다.** 종목마다 사진이 두 장씩 있지만 출처가 확인되지 않았다.

- 원본 저장소 `wrkout/exercises.json`의 기여 안내는 이미지를 인터넷에서 긁어 왔고 저작권이 없으니 상업 프로젝트에 쓰지 말라고 적고 있다.
- `free-exercise-db` 관리자도 이미지가 어디서 왔는지 모르고 쓰려면 각자 위험을 감수해야 한다고 답했다([#2](https://github.com/yuhonas/free-exercise-db/issues/2)).
- 2026년 8월에는 이미지를 자리 표시로 바꾸는 방안이 논의됐다([#13](https://github.com/yuhonas/free-exercise-db/issues/13)).

**설명 문장의 출처도 확인하지 못했다.** 저장소는 Unlicense로 공개돼 있지만, 동작 설명(`instructions`)을 누가 썼는지는 알 수 없다. 영어 설명은 한국어 앱에 넣을 일도 없으므로 가져오지 않는다.

**이름이 그대로 쓰기에 맞지 않다.** "Barbell Bench Press - Medium Grip", "Wide-Grip Lat Pulldown"처럼 변형까지 이름에 들어가 있고, "Lat Pulldown"이나 "Hip Thrust" 같은 기본 이름은 없다. 스트롱맨·플라이오메트릭처럼 헬스장에서 드물게 하는 종목도 많다.

번역은 2023년부터 논의 중이고 아직 들어오지 않았다([#9](https://github.com/yuhonas/free-exercise-db/issues/9)).

## wger

공개 API(`/api/v2/exerciseinfo/`)에서 973종목을 받았다. 인증은 필요 없다.

| 항목 | 값 |
|---|---|
| 종목 라이선스 | CC-BY-SA 4.0 815, CC-BY-SA 3.0 132, CC0 26 |
| 번역 | 영어 973, 스페인어 644, 독일어 638, 프랑스어 584 순. 한국어는 언어 목록에 있지만 번역이 0건이다 |
| 이미지 | 287종목에 394장 |
| 영상 | 78개 |
| 부위 분류 | Legs 231, Back 174, Arms 168, Shoulders 125, Abs 118, Chest 90, Cardio 53, Calves 14 |

CC-BY-SA 데이터를 앱에 넣으면 종목마다 작성자와 라이선스를 표시해야 하고 그 데이터를 고쳐 배포할 때 같은 조건을 유지해야 한다. 한국어 이름을 어차피 직접 써야 하는데 이 의무를 질 만큼 얻는 것이 없다.

이미지가 필요해지면 다시 볼 만하다. wger의 이미지는 이미지마다 라이선스와 작성자가 기록돼 있어 출처 표기를 지킬 수 있다.

## ExerciseDB

약관이 데이터 소유권을 제공자에게 두고, 구독이 끝나면 쓸 수 없으며 저장도 금지한다. 내장할 수 없어 제외한다.

## 한국어 이름

한국어 종목 이름이 들어 있는 공개 데이터는 찾지 못했다. 위 두 데이터에는 없고 GitHub와 공공데이터를 검색해도 종목 사전으로 쓸 만한 것은 나오지 않았다. 국민체력100 운동 영상 같은 공공기관 자료도 있지만 운동 처방 영상이지 종목 목록이 아니고, 이용 조건은 확인하지 않았다.

따라서 이름은 직접 붙인다. 정해야 할 기준은 셋이다.

- **표기**: 헬스장에서 통용되는 외래어 표기를 쓰고 영어 단어 단위로 띄어 쓴다. 예) 벤치 프레스, 랫 풀다운, 데드리프트.
- **별칭**: 붙여 쓴 형태("벤치프레스"), 줄임말("벤치"), 영어 이름("bench press")을 검색어로 함께 둔다.
- **변형**: 그립 너비 같은 세부 변형은 종목으로 나누지 않는다. 장비가 다르면 다른 종목으로 둔다. 예) 바벨 벤치 프레스, 덤벨 벤치 프레스.

## 내장 범위

| 안 | 내용 | 문제 |
|---|---|---|
| 전체 내장 | `free-exercise-db` 876종목을 그대로 넣는다 | 한국어 이름이 없고 이름 체계가 맞지 않는다. 설명과 이미지의 출처가 불확실하다 |
| **선별 내장(추천)** | 자주 쓰는 종목 100~150개를 직접 작성하고 커스텀 종목을 허용한다 | 목록과 한국어 이름을 직접 만들어야 한다 |
| 내장 없음 | 모든 종목을 사용자가 직접 추가한다 | 첫 실행에서 할 일이 많다. 종목 이름과 부위를 사용자가 다 입력해야 한다 |

선별 내장을 추천한다. 종목 이름, 주로 쓰는 부위, 장비는 널리 알려진 사실이라 직접 정리하면 출처 문제가 없다. 한국어 이름과 검색어도 처음부터 우리 기준으로 맞출 수 있다.

## 추천 데이터 형태

종목 하나에 다음을 둔다. 정확한 구조는 설계 이슈에서 정한다.

| 필드 | 설명 |
|---|---|
| 식별자 | 영문 슬러그. 예) `barbell-bench-press` |
| 한국어 이름, 영어 이름 | |
| 별칭 | 검색용 |
| 부위 | 큰 분류 6개(가슴, 등, 어깨, 팔, 하체, 코어)와 세부 근육 |
| 보조 부위 | |
| 장비 | 바벨, 덤벨, 머신, 케이블, 맨몸, 케틀벨, 밴드, 기타 |
| 기록 방식 | 중량과 횟수, 횟수만(맨몸), 시간(플랭크) |

기록 방식은 두 공개 데이터 어디에도 없는 값이다. 세트 입력 화면이 종목에 따라 달라지므로 처음부터 넣는다.

## 라이선스 표기

- 직접 작성한 종목 데이터는 저장소 라이선스(MIT)를 따른다.
- `free-exercise-db`는 참고만 하므로 표기 의무가 없다. README에 참고한 자료로 적는다.
- 나중에 wger 이미지를 쓰게 되면 앱 안에 출처 화면을 만들어 이미지별 작성자와 라이선스를 표시한다.

## 확인이 남은 것

- 내장할 종목의 실제 목록과 개수. 구현 이슈에서 정한다.
- 스미스 머신처럼 두 공개 데이터가 구분하지 않는 장비를 따로 둘지.
- 유산소 종목을 넣을지. 요구사항([#6](https://github.com/zzunsik/next-set/issues/6))에서 근력만 다룰지 정한 뒤에 본다.
- ExerciseDB 한국어 지원 여부는 확인하지 않았다. 약관 때문에 제외했으므로 더 보지 않는다.

## 출처

- [free-exercise-db](https://github.com/yuhonas/free-exercise-db), [dist/exercises.json](https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json)
- [free-exercise-db #2 License of Images?](https://github.com/yuhonas/free-exercise-db/issues/2)
- [free-exercise-db #13 images has copyright?](https://github.com/yuhonas/free-exercise-db/issues/13)
- [free-exercise-db #9 Translations](https://github.com/yuhonas/free-exercise-db/issues/9)
- [wrkout/exercises.json 기여 안내](https://github.com/wrkout/exercises.json/blob/master/CONTRIBUTING.md#exercise-images)
- [wrkout/exercises.json #305](https://github.com/wrkout/exercises.json/issues/305)
- [wger API](https://wger.de/api/v2/exerciseinfo/), [wger 언어 목록](https://wger.de/api/v2/language/)
- [ExerciseDB](https://exercisedb.dev/), [이용 약관](https://exercisedb.notion.site/ExerciseDB-API-Terms-of-Use-226983b728ca8090bf7be79564e4b356)
