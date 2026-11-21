# 디자인 레퍼런스

조사일: 2026-11-17. 이슈 [#4](https://github.com/zzunsik/next-set/issues/4).

화면을 그리기 전에, 잘 만든 운동 기록 앱이 세트 입력·직전 기록·휴식 타이머를 어떻게 보여 주는지 실제 화면으로 확인했다. 대상은 기록 중심 앱 둘(Strong, Hevy), 국내 앱 둘(플릭, 번핏), 디자인 수상작 둘(Gentler Streak, Any Distance)과 Android 플랫폼 기준 둘(Material 3 Expressive, Live Updates)이다. 화면을 복제하지 않고 원칙만 가져온다.

근거는 세 종류로 나눠 적었다.

- **스토어**: App Store·Google Play 등록 스크린샷을 내려받아 직접 봤다.
- **공식**: 제작사 사이트·도움말, 플랫폼 공식 문서. 문서에 실린 화면 이미지를 직접 본 경우 "공식 화면"이라고 적었다.
- **제3자**: 사용자 리뷰, 리뷰 글, 제3자 사이트의 화면 설명문.

스토어 스크린샷은 제작사가 고른 홍보 장면이다. 앱을 설치해 확인하지는 않았다.

색, 모양, 타이포, 그림, 연출 같은 시각 언어는 [`visual-references.md`](visual-references.md)(#87)에서 따로 조사했다.

## 앱별 정리

| 앱 | 가져올 것 | 피할 것 | 근거 |
|---|---|---|---|
| Strong | 세트 표에 직전 기록 열이 있다. 아직 안 한 세트의 무게·횟수 칸에는 흐린 값을 미리 채운다. 완료한 행은 옅은 초록 배경으로 바뀐다. Android 화면에서는 휴식 타이머가 방금 끝낸 세트 바로 아래 가로 막대로 흐른다. 빈 시작 화면은 버튼 하나와 예시 템플릿으로 채운다 | 종목 그래프 카드를 유료 잠금으로 덮는 것. iOS 화면에서 운동 이름·경과 시간·메모가 위쪽을 넓게 차지해 첫 세트가 화면 중간에서 시작하는 배치 | 스토어(App Store 6장, Google Play 9장), 공식 화면(도움말 3장), 제3자(screensdesign 설명문·미리보기) |
| Hevy | 직전 기록(PREVIOUS)을 탭하면 현재 세트에 값이 들어간다. 휴식 타이머는 화면 아래 얇은 바(−15 / 남은 시간 / +15 / Skip)로 뜨고 숫자 키패드가 올라와도 그 위에 남는다. 잠금 화면 카드에 종목과 다음 세트(몇 번째, 무게×횟수)를 함께 적는다. 빈 운동 화면은 한 문장과 버튼 하나다 | 소셜 피드·팔로우·비교 화면. 스토어 10장 가운데 3장이 이 내용이라 기록 앱의 범위를 넘는다. RPE 칸까지 켜면 한 행에 열이 6개가 된다 | 스토어(App Store 10장, Google Play 14장 묶음), 공식 화면(기능 소개 8장), 공식(기능 소개 글) |
| 플릭 | 강조색을 하나만 쓴다(검정 바탕에 주황빛 빨강). 요약 화면의 숫자 격자는 큰 숫자, 작은 색 단위, 회색 라벨 세 단으로 읽힌다. 한 종목씩 넘기는 화면 위쪽에 종목 진행 표시가 있다 | 종목 그림이 화면 절반을 차지해 세트 표가 아래로 밀린다. 세트 표에 직전 기록이 없어 다른 탭으로 가야 한다. 세트추가·세트삭제·모든 세트완료 글자 버튼이 색만 다른 채 한데 모여 있다. 기기를 바꾸면 로그인이 막힌다는 불만 | 스토어(App Store 8장), 제3자(App Store 리뷰 최근 50건) |
| 번핏 | 휴식 타이머 카드에 "다음 세트" 칸이 있어 남은 시간과 다음에 할 종목·무게·횟수를 한눈에 본다. 주간 리포트는 결론 한 줄("등 운동 비중이 높았던 한 주")로 시작한다. 세트 번호 옆 작은 막대로 강도 단계를 표시한다 | 기록과 관계없는 요소가 같은 화면에 많다(식단, 커뮤니티, AI 인사이트, 업그레이드·PRO 배지). 휴식 타이머가 팝업 카드라 세트 표를 가린다 | 스토어(App Store 8장, Google Play 11장 묶음), 공식(사이트, App Store 설명과 업데이트 기록) |
| Gentler Streak | 숫자마다 평소와 견준 해석을 붙인다("Normal", "Above Typical Sunday"). 화면 첫 줄이 상태를 말하는 문장이다("Great Effort Today"). 초록은 적정 범위라는 뜻으로만 쓴다 | 큰 일러스트와 색 번짐 배경이 화면 위쪽 절반을 쓴다. 한 손으로 빠르게 입력해야 하는 기록 화면에는 맞지 않는다 | 스토어(App Store 10장), 공식(사이트, Apple Design Awards 2024) |
| Any Distance | 큰 고정폭 숫자와 작은 대문자 라벨. 색을 입힌 숫자는 한 번에 하나뿐이다. 카운트다운 숫자마다 햅틱을 준다 | 3D 메달·공유 카드 같은 볼거리 기능. 소스가 공개돼 있어도 코드나 화면을 그대로 옮기지 않는다 | 스토어(App Store 7장), 제3자(개발자 글, screensdesign 설명문) |

Any Distance는 종료되지 않았다. 개발자 글에 따르면 2023년 중반부터 유지보수 상태였고 2025-06-20에 iOS 소스를 공개했으며 앱은 App Store에 남겨 두었다. 조사일에도 스토어에 등록돼 있었다. 마지막 버전은 2024-05-14에 올라온 4.48이다.

## 화면 요소별 관찰

### 1. 세트 입력 방식

- Strong: 표의 열은 Set / Previous / kg / Reps / 체크다. 아직 안 한 세트는 kg·Reps 칸이 회색 상자이고 그 안에 흐린 값이 들어 있다. 완료하면 행 전체가 옅은 초록이 되고 체크가 초록 버튼으로 바뀐다(스토어, 공식 화면). Android 화면에는 입력 칸 하나가 선택된 장면이 있는데 화면 아래에 시스템 키보드 대신 자체 숫자판이 보인다. 숫자판에는 − / + 증감 키와 NEXT 버튼, 원판 조합을 그려 주는 플레이트 계산기가 붙어 있다(스토어). 워밍업 세트는 번호 자리에 주황색 W를 쓴다(스토어).
- Hevy: 열은 SET / PREVIOUS / KG / REPS / (RPE) / 체크다. 입력은 시스템 숫자 키패드로 한다. 횟수 칸에는 목표 범위("8-12")가 흐리게 들어 있다. 완료한 행은 선명한 연두색이고 값이 굵은 검정으로 바뀐다(스토어, 공식 화면). 워치 화면은 kg와 Reps를 휠로 돌려 맞추고 가운데 체크 버튼을 누른다(스토어, 작은 묶음 이미지로 봄).
- 플릭: 세트 / KG / 회 / 완료 네 열이다. 값은 둥근 사각 칩에 들어 있고 완료 칸은 빨간 테두리 체크 상자다. 표 아래에 세트추가, 세트삭제, 모든 세트완료, 다음운동이 글자 버튼으로 놓인다(스토어).
- 번핏: 세트 / kg / 회 / 완료 네 열이다. 입력 칸은 테두리 상자이고 완료는 넓은 사각 체크 버튼이다. 표 아래에 세트 삭제와 세트 추가가 있다(스토어). 워치 화면은 "#1 60kg × 5" 한 줄과 체크 상자다(스토어).
- Gentler Streak, Any Distance: 세트 입력이 없다. 심박·거리 기반 앱이다(스토어).

시사점: 네 앱 모두 한 행에 세트 하나를 두고 오른쪽 끝에서 체크한다. 이 틀은 바꾸지 않는다. 차이는 값을 얼마나 덜 치게 하느냐에서 난다. 흐린 값을 미리 채워 두면 지난번과 같은 세트는 체크 한 번으로 끝난다. 무게를 바꿀 때는 증감 키가 숫자 입력보다 빠르다. 휠은 손목 화면에서만 보였다.

### 2. 직전 기록의 위치와 형태

- Strong: Previous 열에 "25 kg × 5"를 회색으로 적는다. 기록이 없으면 "—" 또는 "No Previous"다(스토어, 공식 화면). 어느 운동의 기록을 직전으로 볼지 고르는 설정이 있다(제3자, screensdesign 설명문).
- Hevy: PREVIOUS 열에 "20kg x 18"을 회색으로 적는다. 탭하면 현재 세트에 값이 들어간다. 처음 하는 종목이면 빈칸이다. 직전 기록의 기준을 "어느 운동에서든 마지막" 또는 "같은 루틴에서 마지막"으로 고른다(공식). 워치 화면은 입력 휠 아래에 "prev. 50.0 kg x 10"을 적는다(스토어).
- 플릭: 세트 표에 직전 기록이 없다. 종목 상세의 "나의 기록" 탭에서 최고 기록·최근 기록·차트·상위 백분율을 칩으로 골라 본다(스토어).
- 번핏: 세트 표에 직전 기록 열이 없다. 종목 머리에 "최근 기록" 버튼이 있다. 운동 전체의 총 볼륨 옆에 "+129kg"가 파란 글씨로 붙는데 무엇과 견준 값인지는 화면만으로 알 수 없다(스토어).

시사점: 직전 기록이 입력 칸 바로 왼쪽에 있으면 화면을 옮기지 않고 오늘 값을 정할 수 있다. 버튼이나 탭 뒤에 두면 세트마다 한 번씩 더 눌러야 한다. 기록이 없는 종목은 빈칸으로 두는 편이 "없음" 문구보다 조용하다.

### 3. 휴식 타이머

- Strong: 세트를 완료하면 바로 시작한다. 기본값은 2분이다(공식). iOS 화면에서는 왼쪽 위 타이머 버튼이 파란 칩("1:59")으로 바뀐다. 이 칩을 탭하면 원형 진행 링과 큰 남은 시간, −10s / +10s / Skip이 있는 전체 화면이 열린다(공식, 공식 화면, 스토어). Android 화면에서는 완료한 세트 행 아래에 가로 진행 막대와 남은 시간("0:34")이 뜬다. 남은 세트 사이에는 예정 휴식 시간("2:00")이 얇은 선과 함께 적혀 있다(스토어). 종목마다 본 세트와 워밍업 세트의 휴식 시간을 따로 정한다(공식 화면).
- Hevy: 세트를 체크하면 그 종목에 정해 둔 시간으로 시작한다(공식). 종목 머리 아래 "Rest Timer: 1min 30s" 줄을 탭하면 시간을 고르는 휠이 열린다. 5초부터 5분까지 고를 수 있고 맨 위에 off가 있다(공식, 공식 화면). 실행 중에는 화면 아래에 진행 막대와 −15 / 남은 시간 / +15 / Skip이 뜬다. 키패드가 올라오면 그 위로 붙는다(공식 화면). 끝나면 "Rest time is over!" 알림이 온다. 잠금 화면 카드에는 종목 이름, "Next: set 5 of 6 (90 kg x 3 reps)", 경과 시간, 남은 휴식, −15s / +15s / Skip이 있다(공식 화면). 공식 글은 이 카드를 iOS와 Android 모두에서 쓸 수 있고 잠금을 풀지 않고 세트를 완료 처리할 수 있다고 적는다(공식).
- 번핏: 팝업 카드로 뜬다. 큰 파란 남은 시간 아래에 전체 시간이 작게 있다. 그 밑 "다음 세트" 칸에는 종목과 무게×횟수를 적는다. −10초 / +10초, 건너뛰기 / 닫기 버튼이 있다. 화면 맨 아래 바에는 휴식 타이머 ON 표시와 운동 시간, 운동 완료 버튼이 있다(스토어). 종목별 기본값은 App Store 업데이트 기록(2.298)에 적혀 있고 화면으로는 보지 못했다(공식).
- 플릭: 스토어 화면에 휴식 타이머가 없다. 화면 위쪽의 00:12는 운동 경과 시간으로 보인다(스토어).
- 휴식 타이머 알림이 잠금 화면에서 오지 않는다는 불만이 다른 운동 기록 앱의 스토어 리뷰에 있다(제3자).

시사점: 시작은 네 앱 모두 세트 완료에 묶여 있어 따로 누를 버튼이 없다. 표시 위치는 세트 행 아래 막대(Strong Android), 화면 아래 바(Hevy), 팝업 카드(번핏) 셋으로 갈린다. 앞의 둘은 세트 표를 가리지 않는다. 휴식 중에 보여 줄 내용은 번핏과 Hevy 잠금 화면이 같다. 남은 시간과 다음 세트다. 조절은 ±10초나 ±15초 버튼 둘과 건너뛰기면 충분하다.

### 4. 운동 중 화면의 정보 순서

- Strong: 타이머 버튼과 Finish → 운동 이름(가장 큰 글씨), 경과 시간, 메모 → 종목 이름(파란 글씨)과 종목 메뉴 → 노란 고정 메모 → 세트 표 → Add Set. 스토어 화면 한 장에 종목 하나와 다음 종목의 이름까지 보인다(스토어). 종목은 세로로 이어지는 목록이다.
- Hevy: 접기 화살표, 타이머 아이콘, Finish → Duration / Volume / Sets 세 칸 → 종목 카드(썸네일, 파란 이름, 메모, Rest Timer 줄, 세트 표, Add Set). 스크롤하면 세 칸 요약은 올라가고 상단 바에 경과 시간만 남는다. 한 화면에 종목 하나에서 둘이 보인다(스토어, 공식 화면).
- 플릭: 한 화면에 종목 하나다. 위에서부터 경과 시간과 종목 진행 표시, 큰 종목 그림, 종목 이름, 접을 수 있는 메모, 세트 표다. 세트 표는 화면 중간 아래에서 시작한다(스토어).
- 번핏: 날짜와 회차 제목 → 타이머·워치 전송 카드 → 총 볼륨 → 종목 카드(번호, 부위 | 종목 이름, 최근 기록 버튼, 메모, 세트 표) → 다음 종목 카드. 맨 아래에 휴식 타이머 상태와 운동 완료 버튼이 고정돼 있다(스토어). 한 종목만 보는 포커스 모드는 App Store 업데이트 기록(2.298)에만 있고 화면으로는 보지 못했다(공식).
- Any Distance: 지도 아래에 숫자 넷을 2×2로 놓고 지금 중요한 숫자 하나만 색을 입힌다(스토어).

시사점: 목록형(Strong, Hevy, 번핏)은 전체 흐름이 보이는 대신 지금 할 세트를 눈으로 찾아야 한다. 한 종목형(플릭)은 찾을 필요가 없지만 큰 그림에 자리를 내주면 세트 표가 엄지에서 멀어진다. 어느 쪽이든 맨 위에는 운동 이름보다 지금 할 세트가 와야 한다. 총 볼륨이나 경과 시간은 작게 두어도 된다.

### 5. 기록 요약과 그래프

- Strong: 프로필에 카드형 그래프를 쌓는다. 주당 운동 횟수는 보라색 막대, 종목 최고 세트는 보라색 선이다. 눈금은 최댓값과 0 정도만 적는다. 홈 화면 위젯에는 목표선이 있고 목표에 못 미친 주는 옅은 색이다(스토어). 종목 상세는 About / History / Charts / Records 네 탭이다(스토어).
- Hevy: 종목 그래프 아래 칩(Heaviest Weight, One Rep Max, Best Volume, # of Reps)으로 지표를 바꾼다. 최고 기록에는 PR 배지가 붙는다. 프로필에는 "3 hours this week" 같은 한 줄과 주별 막대가 있다. 운동 요약은 Time / Volume / Sets 세 숫자와 종목별 세트 수 목록이다(스토어).
- 플릭: 운동 요약은 사용 근육 그림 아래 숫자 여덟 개를 4×2로 놓는다. 종목별로는 세트 무게를 원형 배지로 늘어놓는다. 종목 기록 탭에 같은 조건 사용자 가운데 상위 몇 %인지 보여 주는 곡선이 있다. 달력은 날짜 아래 색 점으로 기록 종류를 구분한다(스토어).
- 번핏: 주간 리포트가 결론 문장으로 시작하고 그 아래 숫자 카드 넷(운동 시간, 운동 개수, 총 볼륨, 칼로리)이 온다. 종목 분석에는 예상 1RM·최고 무게·최대 볼륨과 과부하도 그래프가 있다. 근육 피로도는 인체 그림의 파란색 농도와 부위별 표로 보여 준다(스토어).
- Gentler Streak: 숫자 아래에 평소와 견준 낱말을 붙인다. 그래프에는 적정 범위를 띠로 깔고 그 위에 내 값을 점으로 찍는다(스토어). Apple은 이 앱의 월간 요약이 엄격한 비교보다 진전에 초점을 둔다고 평했다(공식).

시사점: 요약 화면의 첫 줄은 숫자보다 문장이 읽기 쉽다. 숫자는 견줄 값(직전, 평소, 목표)이 옆에 있을 때 뜻이 생긴다. 그래프는 지표 하나에 선 하나로 두고 칩으로 바꾸는 방식이 여러 선을 겹치는 것보다 단순하다. 남과 견주는 백분율은 이 앱의 범위가 아니다.

### 6. 빈 상태

- Strong: 시작 화면 맨 위에 "Start an Empty Workout" 버튼이 있다. 그 아래 "My Templates (0)"에는 "Tap to Add New Template" 카드가, "Example Templates (5)"에는 예시 템플릿이 놓인다. 첫 실행에는 버튼을 가리키는 말풍선이 뜬다. 빈 운동 화면에는 "Add Exercises" 버튼만 있다(제3자, screensdesign의 흐림 없는 미리보기 화면을 직접 봄). 기록이 없는 종목의 Previous 칸은 "—"다(공식 화면).
- Hevy: 빈 운동 화면은 덤벨 아이콘, "Get started", "Add an exercise to start your workout" 한 문장, 파란 "Add Exercise" 버튼이다. 위쪽 요약 칸은 0으로 채워져 있다(공식 화면).
- 플릭, 번핏, Gentler Streak, Any Distance: 빈 상태 화면을 보지 못했다.

시사점: 두 앱 모두 빈 화면에 설명을 길게 쓰지 않고 다음 행동 하나를 버튼으로 준다. 루틴이 없는 첫 화면은 예시 루틴을 깔아 두면 비어 보이지 않는다.

### 7. 시각 언어

- 색: Strong과 Hevy는 색에 뜻을 붙였다. 파랑은 누를 수 있는 것, 초록·연두는 완료다. Strong은 메모에 노랑, 취소에 빨강을 더 쓴다(스토어). 플릭은 주황빛 빨강 하나로 강조·단위·근육 그림을 모두 칠한다(스토어). 번핏은 파랑 하나에 강도 막대만 여러 색이다(스토어). Gentler Streak는 초록을 적정 범위에만 쓰고 지표 화면마다 배경 색조를 바꾼다(스토어). Any Distance는 강조 숫자 하나에만 색을 준다(스토어).
- 숫자 타이포: Strong과 Hevy의 세트 표 숫자는 본문 크기다. 크기 차이보다 굵기와 색(흐린 회색 → 검정)으로 상태를 나눈다(스토어). 플릭 요약, Gentler Streak, Any Distance는 큰 숫자 옆에 작은 단위를 붙이고 라벨을 따로 작게 둔다(스토어). Any Distance는 카운트다운 숫자를 89pt로, 목표 값을 73pt 전광판 글꼴로 쓴다(제3자, 개발자 글).
- 밀도: Strong과 Hevy는 한 행 높이가 낮아 한 화면에 세트 서너 개와 다음 종목 이름까지 들어간다. 번핏도 비슷하지만 카드와 배지가 더 많다. 플릭은 그림 아래로 세트 세 개와 버튼 줄까지 보인다(스토어).
- 밝기: Strong과 Hevy의 스토어 화면은 대부분 밝고 어두운 화면을 한 장씩 따로 보여 준다. 번핏은 밝은 화면만 있다. 플릭과 Any Distance는 어두운 화면만 보여 준다. Gentler Streak는 대부분 밝고 수면 화면이 어둡다(스토어).

시사점: 기록 화면에서 색이 맡을 일은 누를 수 있다는 표시와 끝났다는 표시 둘이다. 그 밖의 색은 요약 화면으로 미룬다. 큰 숫자는 지금 할 세트와 남은 휴식 시간에만 쓴다. 헬스장 조명과 땀 묻은 손을 생각하면 밝은 화면과 어두운 화면을 둘 다 처음부터 맞춰야 한다.

## 플랫폼 기준

### Material 3 Expressive

쓸 것

- 강조 타이포: `Typography`에 기본 15개 스타일마다 Emphasized 짝이 있다(`displayLargeEmphasized` 등). 공식 문서는 내용 구분을 또렷하게 하고 눈길을 끌 곳에 이 스타일을 쓰라고 적는다. 기본 `displayLarge`는 57sp다. 지금 할 세트의 무게·횟수와 남은 휴식 시간에 쓴다(공식).
- 버튼 그룹: `ButtonGroup`은 버튼을 가로로 나란히 놓는 레이아웃이다. Material 사이트 요약문은 버튼을 묶고 버튼 사이에 상호작용을 더한다고 설명한다. 휴식 시간 ± 버튼과 건너뛰기, 무게 증감 키에 맞는다(공식).
- 모션 스킴: `MotionScheme.expressive()`는 눈에 띄는 요소와 중요한 순간에 권하는 스킴이다. `standard()`는 실용적인 요소와 반복되는 동작에 쓰는 기본 스킴이다. 모양이나 크기가 바뀌는 애니메이션은 spatial 스펙, 색·투명도만 바뀌는 애니메이션은 effects 스펙을 쓴다(공식). 세트 완료 체크는 한 번 운동에 수십 번 반복하니 standard에 맞춘다. expressive는 최고 기록 갱신과 운동 완료에만 쓴다.
- 모양: `MaterialShapes`는 미리 정의한 도형을 그대로 쓰거나 다른 도형으로 변형(Morph)하게 해 준다(공식). 완료 체크나 기록 갱신 배지에 쓸 수 있다.
- API 상태: `androidx.compose.material3` 1.5.0-rc01(2026-11-04) 기준으로 MotionScheme·ButtonGroup·SplitButton·FloatingToolbar·MaterialExpressiveTheme는 안정 API다. LoadingIndicator와 MaterialShapes는 실험 API다. 안정 버전은 1.4.0이다(공식).
- 2026-11-21 덧붙임(#87): 1.5.0이 2026-11-18에 안정판으로 나왔다. 이 저장소는 BOM `2026.09.00`으로 1.4.0을 쓰고 있고, 1.4.0에서는 위 API가 공개돼 있지 않다. 버전별로 쓸 수 있는 것과 없는 것은 [`visual-references.md`](visual-references.md)의 "플랫폼 기준"에 표로 정리했다.

주의할 것

- Google 연구진은 46건의 연구와 18,000명이 넘는 참가자로 이 디자인을 검증했다고 밝혔다. 같은 글은 기본 상호작용 틀을 깨면 표현적인 디자인이 사용성을 떨어뜨리거나 반감을 살 수 있다고 경고한다. 글자 라벨을 뺀 버튼은 사용성이 내려갔다고 한다. 표현적인 디자인이 기본 기능을 이기지 못한다고도 적었다(공식). 세트 표의 행·열 틀과 체크 위치는 익숙한 모양 그대로 둔다. ± 버튼에는 아이콘만 두지 않고 "+10초"처럼 값을 적는다.
- LoadingIndicator는 짧은 대기를 알리는 요소다(공식). 휴식 타이머처럼 남은 양을 보여 줘야 하는 곳에는 진행 표시(progress indicator)를 쓴다.
- 실험 API는 버전이 오르면서 바뀔 수 있다. MaterialShapes를 쓰는 곳은 한곳으로 모아 둔다.
- 다이내믹 컬러를 켜면 사용자 배경화면에 따라 색이 바뀐다. 완료를 뜻하는 색은 배경화면과 상관없이 같은 뜻으로 읽혀야 하므로 고정할지 먼저 정한다(공식 문서의 다이내믹 컬러 설명에서 끌어낸 판단이다).

### Android Live Updates

쓸 것

- 상태바 칩에 카운트다운을 띄운다(`setUsesChronometer`와 `setChronometerCountDown`). 공식 문서가 운동 시작을 적합한 사례로 든다(공식).
- 칩 글자는 7자 미만이면 전부 보인다. "1:30" 같은 남은 시간만 넣는다(공식).
- 알림 본문에는 Hevy 잠금 화면 카드처럼 종목 이름과 다음 세트(몇 번째, 무게×횟수)를 적는다. 앱을 열지 않아도 다음에 할 일이 보인다(공식 화면에서 본 구성을 옮긴 판단이다).

주의할 것

- 커스텀 RemoteViews를 쓸 수 없고 ProgressStyle 같은 표준 스타일만 된다. 앱 안의 타이머 모양을 알림에 그대로 옮길 수 없다(공식).
- `POST_PROMOTED_NOTIFICATIONS` 권한이 필요하다. 권한이 없거나 꺼진 기기에서도 보통 알림으로 휴식 종료를 알려야 한다(공식). 타이머 알림 누락은 운동 기록 앱 리뷰에 보이는 불만이다(제3자).
- 휴식이 끝나면 칩과 알림을 바로 내리거나 다음 세트 안내로 바꾼다. 끝난 타이머를 상태바에 남겨 두지 않으려는 이 문서의 판단이다. Live Updates 문서가 종료 처리를 어떻게 정해 두었는지는 구현 전에 다시 확인한다.

## 정리한 원칙

1. **다음 세트를 가장 크게 보여 준다.** 운동 중 화면 맨 위와 휴식 화면에 다음에 할 종목·무게·횟수를 가장 큰 글씨로 둔다. 번핏 휴식 타이머의 "다음 세트" 칸, Hevy 잠금 화면과 워치의 "Next" 줄에서 나왔다(3번, 4번 관찰).
2. **직전 기록은 입력 칸 옆에 두고 값을 미리 채운다.** 지난번과 같은 세트는 체크 한 번으로 끝나야 한다. 기록이 없으면 빈칸으로 둔다. Strong과 Hevy의 Previous 열, 흐린 값 채우기, Hevy의 탭해서 채우기에서 나왔다(1번, 2번 관찰).
3. **휴식 타이머는 세트 완료가 켠다. 세트 표를 가리지 않는다.** 행 아래 막대나 화면 아래 바로 보여 주고 조절은 ± 버튼 둘과 건너뛰기만 둔다. 앱 밖에서는 Live Updates 칩과 알림이 같은 내용을 잇는다. Strong Android의 행 아래 막대, Hevy의 하단 바, 번핏 팝업이 표를 가리는 장면, Live Updates 문서에서 나왔다(3번 관찰, 플랫폼 기준).
4. **기록 화면의 색은 두 가지 뜻만 가진다.** 누를 수 있음과 완료다. 강조색은 하나로 정하고 장식 배경과 큰 그림은 기록 화면에 두지 않는다. Strong·Hevy의 완료 행, 플릭의 단일 강조색과 그림에 밀린 세트 표, Gentler Streak의 초록 범위에서 나왔다(4번, 7번 관찰).
5. **큰 숫자는 한 화면에 하나, 견줄 값과 함께 둔다.** 숫자는 크게, 단위와 라벨은 작게 쓴다. 요약 화면은 결론 문장으로 시작하고 숫자 옆에 직전이나 평소 값을 붙인다. Any Distance의 강조 숫자, Gentler Streak의 평소 대비 낱말, 번핏 주간 리포트 제목, M3 강조 타이포에서 나왔다(5번, 7번 관찰, 플랫폼 기준).
6. **반복하는 동작은 조용하게, 드문 순간만 크게 움직인다.** 세트 체크와 값 입력은 standard 모션과 익숙한 표 모양을 지킨다. 최고 기록과 운동 완료에만 expressive 모션, 도형 변형, 햅틱을 쓴다. MotionScheme 문서의 구분, Google 연구 글의 경고, Any Distance의 카운트다운 햅틱에서 나왔다(플랫폼 기준, 7번 관찰).

## 확인하지 못한 것

화면으로 직접 보지 못한 항목

- Strong의 "두 번 탭으로 세트 기록"은 리뷰 글에만 있다(제3자). 스토어 화면으로 확인한 것은 아직 안 한 세트 칸에 흐린 값이 들어 있다는 데까지다. 그 값이 탭 없이 그대로 기록되는지는 보지 못했다.
- Strong iOS 숫자판의 키 배열. screensdesign 미리보기는 기록 흐름 화면이 흐림 처리돼 있어 아래에 어두운 숫자판과 파란 버튼이 있다는 정도만 보였다. Android 숫자판도 플레이트 계산기에 가려진 장면만 봤다.
- screensdesign의 화면 녹화 영상. 페이지가 영상 내려받기를 막아 두어 받지 않았다. 챕터 목록, 화면 설명문, 흐림 없는 앞부분 화면만 썼다. 설명문은 그 사이트가 쓴 글이라 제3자 근거로만 다뤘다. Gentler Streak·플릭·번핏은 이 사이트에서 페이지를 찾지 못했다.
- 번핏의 포커스 모드와 세트 어시스턴트(5×5, 탑세트) 화면. 스토어 화면에는 "어시스트 방식" 라벨과 강도 막대만 보인다.
- 플릭의 휴식 타이머.
- 플릭·번핏·Gentler Streak·Any Distance의 빈 상태.
- Hevy의 Android 알림 화면. 공식 글이 Android 지원을 적었지만 실린 이미지는 iOS 잠금 화면이다.
- Android 화면 전반. Android 기기 화면으로 본 것은 Strong의 Google Play 9장뿐이다. Hevy와 번핏의 Google Play 화면은 iOS 상태바가 보이는 같은 홍보 이미지였다.
- 탭 수. 어느 앱도 세트 하나를 기록하는 데 드는 탭 수를 직접 세지 못했다.

출처가 약한 항목

- Strong의 휴식 타이머 평과 두 번 탭 기록은 리뷰 글 두 편에서 왔다. 타이머 알림 누락 불만은 다른 앱(Rep Count)의 스토어 리뷰 한 곳에서 봤다. Strong·Hevy·번핏의 App Store 리뷰 피드는 조사 시점에 빈 응답이라 이 앱들의 리뷰는 직접 읽지 못했다.
- 플릭에 동작 시연이 없다는 App Store 리뷰가 있었지만 이번에 본 자료와 맞지 않는다. 스토어 화면에는 종목마다 근육을 강조한 그림이 있다. 2026-07-27 리뷰 한 건은 자세를 알려 주는 영상이 있다고 적었다. 그림이 움직이는지는 정지 화면이라 알 수 없다. 플릭 리뷰는 최근 50건(2026-07-13~2026-11-14)만 봤다.
- m3.material.io 본문은 자바스크립트로 그려져 읽지 못했다. 페이지 요약문(meta description)만 확인했다. 버튼 그룹의 연결형 변형, 모양·모션의 세부 지침은 developer.android.com의 API 문서로 대신했다.
- Live Updates로 승격된 알림에 ± 버튼과 건너뛰기를 액션으로 붙일 수 있는지는 구현 전에 확인해야 한다.
- Any Distance의 글꼴 크기와 햅틱은 개발자 글을 옮겨 실은 제3자 사이트에서 읽었다. 공개 저장소는 있는 것만 확인했고 코드를 읽지 않았다. 저장소에 표준 라이선스가 표시돼 있지 않아 코드를 가져다 쓰려면 라이선스부터 확인해야 한다.
- Gentler Streak 공식 사이트에는 App Store 링크만 있었다. Android 판이 있는지는 확인하지 않았다.

## 출처

스토어

- Strong: [App Store](https://apps.apple.com/us/app/id464254577), [Google Play](https://play.google.com/store/apps/details?id=io.strongapp.strong)
- Hevy: [App Store](https://apps.apple.com/us/app/id1458862350), [Google Play](https://play.google.com/store/apps/details?id=com.hevy)
- 플릭: [App Store](https://apps.apple.com/kr/app/id1576993198)
- 번핏: [App Store](https://apps.apple.com/kr/app/id1503464984), [Google Play](https://play.google.com/store/apps/details?id=com.bunnit.haja.android)
- Gentler Streak: [App Store](https://apps.apple.com/us/app/id1576857102)
- Any Distance: [App Store](https://apps.apple.com/us/app/id1545233932)

공식 사이트와 도움말

- Strong 도움말: [About Rest Timer](https://help.strongapp.io/article/231-rest-timer), [How do I perform a workout with Strong?](https://help.strongapp.io/article/229-my-first-workout)
- Hevy 기능 소개: [Rest Timer](https://www.hevyapp.com/features/workout-rest-timer/), [Previous Workout Values](https://www.hevyapp.com/features/track-exercises/), [Sets and Reps](https://www.hevyapp.com/features/how-to-write-sets-and-reps/), [Live Activity](https://www.hevyapp.com/features/live-activity/), [Start Empty Workout](https://www.hevyapp.com/features/start-empty-workout/)
- 번핏: [burnfit.io](https://burnfit.io/)
- Gentler Streak: [gentlerstories.com/gentlerstreak](https://gentlerstories.com/gentlerstreak/), [Apple Design Awards 2024](https://developer.apple.com/design/awards/2024/)

플랫폼 공식 문서

- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [MotionScheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme)
- [Typography API](https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography)
- [androidx.compose.material3 패키지 요약](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [Expressive Material Design: Google Research](https://design.google/library/expressive-material-design-google-research)
- m3.material.io(요약문만 확인): [Button groups](https://m3.material.io/components/button-groups/overview), [Loading indicator](https://m3.material.io/components/loading-indicator/overview), [Shape](https://m3.material.io/styles/shape/overview-principles), [Motion](https://m3.material.io/styles/motion/overview/how-it-works)
- [Live Updates](https://developer.android.com/develop/ui/views/notifications/live-update)
- [Compose Material 3 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/compose-material3)

리뷰와 제3자 글

- screensdesign 화면 녹화 페이지: [Strong](https://screensdesign.com/apps/strong-workout-tracker-gym-log/), [Hevy](https://screensdesign.com/apps/hevy-workout-tracker-gym-log/), [Any Distance](https://screensdesign.com/apps/any-distance-running-tracker/)
- Strong 리뷰 글: [lifting-apps](https://lifting-apps.pages.dev/reviews/strong), [repreturn](https://repreturn.com/?p=1625)
- 타이머 알림 불만: [Rep Count App Store 리뷰](https://apps.apple.com/us/app/rep-count-strive/id6449553638)
- 플릭 App Store 리뷰 피드: [최근순 JSON](https://itunes.apple.com/kr/rss/customerreviews/id=1576993198/sortBy=mostRecent/json)
- Any Distance: [Open Sourcing Any Distance](https://kuntz.io/blog/any-distance-oss.html)(개발자 글, 2025-06-20), [Any Distance Goes Open Source](https://www.spottedinprod.com/blog/any-distance-goes-open-source), [공개 저장소](https://github.com/Any-Distance/any-distance-ios)
