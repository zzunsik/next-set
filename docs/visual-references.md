# 시각 디자인 레퍼런스

조사일: 2026-11-21. 이슈 [#87](https://github.com/zzunsik/next-set/issues/87).

[`design-references.md`](design-references.md)(#4)는 세트 입력, 직전 기록, 휴식 타이머 같은 기능 패턴을 조사했다. 이 문서는 같은 종류의 앱이 색, 모양, 글자, 그림, 움직임을 어떻게 쓰는지 조사한다. v0.1.0을 실기기에서 본 뒤 화면이 밋밋하고 앱만의 인상이 없다는 피드백이 있었고, 문제는 넷으로 정리됐다.

1. 바탕, 카드, 목록이 거의 같은 색이라 층이 보이지 않는다. 어두운 화면에서 특히 평평하다.
2. 설정·기록·종목 목록이 줄과 구분선뿐이고, 버튼·칩·대화상자가 Material 기본 모양 그대로다.
3. 큰 숫자 말고는 글자 크기·굵기 차이가 작아 강약이 약하다.
4. 빈 상태가 글 한 줄뿐이고, 완료나 최고 기록 같은 순간의 연출이 적다.

이 문서는 값을 정하지 않는다. 시안은 #88, [`DESIGN.md`](DESIGN.md)와 코드 반영은 #89에서 한다. 화면을 복제하지 않고 원칙과 수치의 범위만 가져온다.

근거는 #4와 같은 이름으로 나눠 적었다.

- **스토어**: App Store·Google Play 등록 스크린샷을 내려받아 직접 봤다. 색 값은 그 이미지의 픽셀을 찍어 읽은 근삿값이다. 홍보용으로 다듬은 JPEG라 실제 앱의 값과 다를 수 있다.
- **공식**: 제작사 사이트·도움말, 플랫폼 공식 문서와 릴리스 노트, 라이선스 페이지. 라이브러리 소스를 Google Maven에서 받아 직접 읽은 것은 "공식(소스)"라고 적었다.
- **제3자**: 리뷰 글, 디자인 모음 사이트의 시안과 설명문.
- **계산**: 이 문서를 쓰며 직접 계산한 값. 방법을 함께 적었다.

앱을 설치해 확인하지는 않았다. 다른 앱의 화면 이미지는 저장소에 넣지 않았다.

## 지금 앱의 값

견줄 기준으로 `DESIGN.md`의 지금 값을 옮겨 둔다.

| 요소 | 지금 값 |
|---|---|
| 층 | 바탕과 표면 두 단. 밝은 화면 `#F6F7F9` / `#FFFFFF`(대비 1.07), 어두운 화면 `#101116` / `#191B22`(대비 1.09) |
| 색의 뜻 | 강조색 남보라(`#3F3DB8` / `#A9A8FF`)는 누를 수 있음, 초록(`#1A7A48` / `#6FD39A`)은 완료 |
| 모서리 | 8dp 하나와 완전히 둥긂. 대화상자와 시트는 M3 기본 |
| 글자 | Pretendard. 크기 13 / 14 / 16 / 18 / 20 / 22 / 28 / 40sp, 굵기 400 / 600 / 700 |
| 목록 | 줄과 구분선. 화면 코드에서 M3 `TextButton` 호출이 32곳, `HorizontalDivider`가 9곳, `Button`이 1곳이다(소스를 세어 봄) |
| 아이콘 | Material Symbols Rounded 굵기 400, 채움 0, 24px. 16개 |
| 빈 상태 | 문구 한 줄과 버튼 |
| 연출 | 최고 기록 배지와 운동 요약 두 곳에 expressive 스프링과 진동 |

## 앱별 정리

대상은 이슈가 든 다섯(Hevy, Strong, Fitbod, Gravl, Ladder)과 그림 방식이 다른 Liftin' 하나다. 플릭과 번핏은 #4에서 본 내용으로 충분해 다시 보지 않았다.

| 앱 | 한눈에 알아보게 하는 것 | 가져올 것 | 피할 것 | 근거 |
|---|---|---|---|---|
| Hevy | 흰 바탕의 피드 같은 화면. 파랑은 행동, 연두 행은 완료, 금색 메달은 기록 | 색마다 뜻이 하나다. 최고 기록은 세트 번호 자리의 메달과 화면 위 알림 한 줄로 알린다. 마침 화면은 "몇 번째 운동"이라는 문장으로 시작한다 | 공유 카드 묶음과 피드. 목록이 실선 구분선뿐이라 층이 없다 | 스토어(App Store 10장, Google Play 8장), 공식 화면 9장, 공식(기능 소개 글) |
| Strong | 표 계산기 같은 공책. 납작한 행, 작은 모서리, 대문자 라벨. 색은 상태에만 나온다 | 어두운 화면은 검정 바탕에 한 단 밝은 채움(대비 1.44)으로 칸을 띄운다. 카드는 그림자 없이 1dp 테두리다 | 숫자와 본문이 같은 크기·굵기라 강약이 없다. 지금 우리 앱의 문제와 같다 | 스토어(App Store 6장, Google Play 7장), 공식 화면 3장, 제3자(screensdesign) |
| Fitbod | 색조가 도는 어두운 바탕, 기울인 굵은 숫자, 육각형 | 어두운 화면을 네 단으로 나눈다. 색 하나에 뜻 하나(분홍빛 빨강은 행동, 민트는 완료, 노랑은 기록). 육각형 하나를 번호 배지·추가 버튼·진행 링·트로피에 되풀이한다 | 종목 사진과 영상이 화면 위쪽을 차지한다. 어두운 화면만 있다 | 스토어(App Store 10장, Google Play 8장) |
| Gravl | 남색 기운의 검정 계기판. 라임색은 "여기를 눌러라" | 카드를 약 10pt 틈으로 띄운 목록. 채움만으로 층을 만들고 그림자가 없다. 알약 모양 주 버튼. 숫자가 라벨의 1.6~1.8배 | 라임이 행동·선택·링크·라벨에 모두 쓰여 뜻이 넓다. 점수 등급마다 색이 더 붙는다 | 스토어(App Store 10장, Google Play 8장을 한 장에 모아 봄), 공식(사이트 글) |
| Ladder | 검정 방송 스튜디오. 넓은 대문자 제목과 라임 잉크 하나 | 주간 줄의 채운 동그라미와 체크로 "한 날"을 보여 준다. 굵은 값 옆에 작고 가는 단위 | 강조색 하나가 행동과 완료를 함께 뜻한다. 우리 규칙(누름과 완료를 나눈다)과 어긋난다. 코치 영상이 화면을 채운다 | 스토어(App Store 10장), 공식(사이트 글), 제3자(Mobbin 2장) |
| Liftin' | 숯색 바탕에 분홍, 동그라미 안 캐릭터 그림 | 동그라미 하나를 세트 번호·달력 날짜·원판 칩에 되풀이한다. 사진 없이도 종목을 그림으로 구분한다 | 바탕 가장자리의 색 번짐 | 스토어(App Store 10장을 한 장에 모아 봄, 1장은 원래 크기) |

## 요소별 관찰

색 값은 스토어 이미지에서 찍은 근삿값이다. 크기(dp, pt)는 기기 틀이 그려진 홍보 이미지에서 어림한 값이다. 대비는 그 색 값으로 WCAG 공식을 적용해 계산했다.

### 1. 색과 층

| 앱 | 화면 | 단계(근삿값) | 바탕과의 대비(계산) | 색조 |
|---|---|---|---|---|
| Fitbod | 어두움 | 바탕 `#181822` → 행 `#222130` → 카드 `#2D2C3B` → 칩·선택 `#343342` | 1.11 → 1.28 → 1.42 | 청보라 기운 |
| Gravl | 어두움 | 바탕 `#0D1017` → 칸 `#141821` → 칩·선택 `#1F2732` | 1.07 → 1.26 | 남색 기운 |
| Ladder | 어두움 | 바탕 `#000000`~`#131313` → 카드 `#232323` → 칩 `#333333` | 카드 1.18~1.33, 칩은 카드 대비 1.24 | 무채색 |
| Strong | 어두움 | 바탕 `#000000` → 채움 `#292929` | 1.44 | 무채색 |
| Hevy | 어두움(2022년 이미지) | 바탕 `#434343` → 카드 `#343434`. 카드가 더 어둡다 | 1.25 | 무채색 회색 |
| Strong | 밝음 | 바탕 `#FFFFFF` → 입력 칸·메모 `#E9EAEA` | 1.20 | 무채색 |
| Hevy | 밝음 | 바탕 `#FFFFFF` → 버튼·검색·칩 `#F2F4F7` | 1.10 | 찬 회색 |
| 우리 앱 | 어두움 / 밝음 | 바탕 → 표면 한 단 | 1.09 / 1.07 | 남보라 기운이 조금 |

- 어두운 화면이 잘 읽히는 앱은 단계가 셋이나 넷이고, 카드가 바탕과 1.25 이상 벌어진다(Fitbod 1.28, Ladder 1.18~1.33, Strong 1.44, Hevy 1.25). 우리 앱은 1.09다(스토어, 계산).
- 층은 채움의 밝기로 만든다. 본 화면에서는 어느 앱에서도 앱 화면의 그림자를 찾지 못했다. Hevy의 그림자는 홍보용 말풍선에만 있다. 테두리는 Strong의 카드(1dp)와 Fitbod의 입력 칸(1dp)에 있다(스토어).
- 어두운 화면의 바탕은 둘로 갈린다. 색조를 넣은 짙은 색(Fitbod, Gravl)과 무채색 검정(Ladder, Strong)이다. 색조가 있는 쪽이 앱마다 다른 인상을 준다(스토어).
- 강조색의 자리: Hevy 파랑 `#267FE8`은 주 버튼, 종목 제목, 링크, 그래프 선, 선택된 칩이다. Fitbod 분홍빛 빨강 `#FF3058`은 행동과 아이콘에만 쓴다. Gravl 라임 `#D2FE07`은 알약 버튼(검은 글자), 선택, 글자 링크, 분류 라벨이다(스토어).
- 완료색의 자리: Hevy는 행 전체를 연두 `#C7FFA0`로 채우고 체크 상자를 초록 `#56BD23`으로 바꾼다. Strong은 아주 옅은 초록 행 `#EBFAF1`(흰 바탕 대비 1.07)에 선명한 체크 `#2ECD70`이다. 어두운 화면은 `#0C331C`다. Fitbod는 행을 칠하지 않고 적은 숫자를 민트 `#91EDD2`로 바꾼다. Ladder는 행을 한 단 밝게 하고 왼쪽에 흰 막대를 붙인다(스토어).
- 그 밖의 뜻 있는 색: 최고 기록은 Hevy 금색 `#F7D000`, Fitbod 옅은 노랑 `#FEF196`이다. 세트 종류 글자는 Hevy와 Strong 모두 워밍업 주황, 실패 빨강이다. Strong의 고정 메모는 노란 띠다(스토어, 공식 화면).
- Strong은 밝은 화면에서도 아래 탭과 숫자판을 짙은 남회색 `#293236`으로 둔다. 입력 도구가 내용과 다른 층으로 읽힌다(스토어).

시사점: 층은 그림자가 아니라 밝기 단계로 만든다. 단계는 적어도 셋(바탕, 카드, 카드 안의 칸)이 있어야 하고, 어두운 화면에서는 바탕과 카드가 1.25 안팎으로 벌어져야 눈에 들어온다. 색의 뜻을 하나씩만 준 앱(Hevy, Fitbod)이 읽기 쉽다. 우리 규칙과 같다.

### 2. 모양

| 앱 | 입력 칸·세트 칸 | 버튼 | 칩 | 카드 | 목록 | 행 높이 |
|---|---|---|---|---|---|---|
| Hevy | 약 8dp | 약 8dp | 완전히 둥긂 | 약 12dp(홍보 말풍선) | 실선 구분선, 글 사이 회색 띠 | 약 44~48dp |
| Strong | 약 6~7dp | 약 2~3dp | 작은 색 칩 | 1dp 테두리, 약 5dp | 구분선 없는 납작한 표 | 약 42dp |
| Fitbod | 1dp 테두리, 약 4dp, 높이 약 50dp | | 완전히 둥긂 | 약 8~12dp, 채움만 | 화면 폭을 채우는 띠 | 넉넉하다 |
| Gravl | 약 10pt | 완전히 둥긂 | 완전히 둥긂 | 약 12~16pt, 채움만 | 약 10pt 틈으로 띄운 카드 | 약 44pt |
| Ladder | | 완전히 둥긂, 높이 약 52pt | | 통계 칸 약 6~8pt, 사진 카드 약 12pt | 실선 구분선의 납작한 행 | 넉넉하다 |

- 둥근 정도를 요소 크기에 따라 달리한다. 작은 칸은 4~10, 카드는 12~16, 주 버튼과 칩은 완전히 둥글다. 한 값만 쓰는 앱은 Strong뿐이고 Strong은 그 대신 테두리와 대문자 라벨로 표를 만든다(스토어).
- 선택은 테두리로 보인다. Gravl은 선택된 칸에 2px 테두리를 두르고, Fitbod는 입력 중인 칸의 테두리를 흰색으로 바꾼다(스토어).
- 도형 하나를 되풀이해 인상을 만든다. Fitbod는 육각형(세트 번호, 추가 버튼, 진행 링, 트로피), Liftin'은 동그라미(세트 번호, 달력 날짜, 원판)다(스토어).
- 주 버튼은 화면 아래 알약 모양이다(Gravl, Ladder). 디자인 모음에서도 출시된 앱에 남아 있는 패턴이다(스토어, 제3자).
- 디자인 모음의 출시 앱은 모서리가 둘로 갈린다. 6pt 이하의 각진 쪽과 16pt 이상의 둥근 쪽이다. 가운데 값은 드물다. 시안은 20~28pt로 쏠린다(제3자, Mobbin 33장과 Layers 24장).

시사점: 8dp 하나로는 요소의 크기와 종류가 모양에서 갈리지 않는다. 작은 칸, 카드, 큰 면, 알약의 서너 단이 있어야 한다. 줄과 구분선뿐인 목록은 Hevy와 Ladder에도 있지만 그 앱들은 사진과 색이 화면을 채운다. 사진이 없는 우리 앱은 Gravl처럼 카드로 묶고 틈으로 띄우는 쪽이 맞다.

### 3. 타이포

| 앱 | 숫자와 본문 | 가장 큰 글자 | 라벨 | 글꼴 느낌 |
|---|---|---|---|---|
| Hevy | 적은 값이 굵고 본문의 약 1.2배. 직전 기록은 회색 보통 굵기 | 요약 카드의 숫자, 본문의 약 2배 굵게 | 작은 회색 대문자(SETS / PREVIOUS / KG / REPS) | 중립적인 산세리프 |
| Strong | 숫자가 본문과 같은 크기, 보통 굵기 | 화면 제목, 본문의 약 2배 보통 굵기 | 대문자에 자간을 넓힘 | 시스템 글꼴로 보인다 |
| Fitbod | 칸 안 숫자가 본문의 약 1.5배, 대표 숫자는 3~4배. 굵은 기울임, 폭이 같은 숫자 | 대표 숫자("68", "43%") | 작은 대문자 | 굵은 기울임 제목 글꼴 |
| Gravl | 세트 숫자가 라벨의 약 1.6~1.8배, 세미볼드. 안 한 값은 흐린 색 | 회복 "94%", 본문의 약 4배 | 작은 대문자 | Inter와 비슷한 산세리프 |
| Ladder | 굵은 값 옆에 작고 가는 단위. 타이머와 횟수는 폭이 좁은 굵은 숫자 | 사진 위 운동 제목과 타이머 | 넓은 대문자 | 폭이 넓은 굵은 대문자 |

- 인상이 뚜렷한 세 앱(Fitbod, Gravl, Ladder)은 대표 숫자가 본문의 3~4배다. 우리 앱의 `numeralLarge`(40sp)는 본문(16sp)의 2.5배다. 밋밋하다고 한 Strong은 숫자와 본문이 같다(스토어).
- 강약은 세 가지를 함께 쓴다. 크기, 굵기, 색(흐린 회색과 본문 색)이다. Hevy는 크기 차이가 작은 대신 굵기와 색으로 나눈다(스토어).
- 라벨은 작고 흐리게, 값은 크고 굵게 둔다. 영어 앱은 라벨을 대문자로 써서 본문과 다른 층으로 만든다. 한글에는 대문자가 없으므로 크기·굵기·색으로 같은 일을 해야 한다(스토어에서 본 것과 그에 따른 판단).
- 숫자 전용 처리: Fitbod는 폭이 같은 기울임 숫자에 빗금 친 0을 쓴다. Ladder는 폭이 좁은 굵은 숫자다. 시안에서는 전광판·고정폭 숫자가 24장 가운데 3장이다(스토어, 제3자).
- 글꼴 이름은 어느 앱도 확인하지 못했다.

시사점: 대표 숫자를 본문의 3배 이상으로 키우고, 그 사이 단계는 줄여서 단마다 차이가 보이게 한다. 굵기 단계를 하나 더 두면 크기를 키우지 않고도 제목과 숫자가 선다. 라벨은 한 가지 작은 스타일로 통일한다.

### 4. 아이콘과 그림

- 아이콘은 가는 외곽선이 기본이고 선택되면 채운다(Hevy, Fitbod, Gravl). Strong과 Ladder의 탭 아이콘은 채운 모양이다(스토어).
- Strong은 설정·대시보드 행 앞에 옅은 파랑 바탕의 작은 아이콘 칩을 둔다. 글자뿐인 행에 머리가 생긴다(스토어).
- 종목 표현은 넷으로 갈린다. 3D 해부 그림(Hevy는 동그란 썸네일, Strong은 상세 화면에만), 실제 사진과 영상(Fitbod, Gravl, Ladder), 평면 캐릭터 그림(Liftin'), 원판 색을 칠한 바벨 그림(Gravl, Liftin'의 세트 행)이다(스토어).
- 근육 표시: Fitbod는 전신 그림에 빨강~회색 네 단계, Hevy는 파랑으로 칠한 3D 앞뒤 그림, Gravl은 작은 상체 썸네일이다(스토어, 공식 화면).
- 빈 상태 그림은 이번에 본 스토어 화면 어디에도 없었다. #4에서 본 Hevy의 빈 운동 화면(덤벨 아이콘, 제목, 한 문장, 버튼)이 확인한 전부다.
- 디자인 모음의 출시 앱 33장 가운데 사진·영상이 약 17장, 일러스트가 3장, 3D가 1장(메달)이다(제3자).

시사점: 잘 만든 앱의 온기는 대부분 사진과 영상에서 온다. 우리 앱은 종목 이미지가 없고 기록 화면에 그림을 두지 않기로 했으므로, 아이콘의 변형(크기, 채움)과 아이콘을 담는 도형으로 같은 일을 해야 한다. 빈 상태는 큰 아이콘 하나, 제목, 한 문장, 버튼 하나면 Hevy 수준이 된다.

### 5. 움직임과 피드백

정지 화면과 공식 글로 본 것이다. 움직이는 모습은 보지 못했다.

- 세트 완료: 네 앱 모두 색이 바뀌는 것으로 끝난다. Hevy 연두 행, Strong 옅은 초록 행, Fitbod 민트 숫자와 육각형 체크, Ladder 밝아진 행과 왼쪽 막대다(스토어).
- 최고 기록: Hevy는 화면 위에 흰 알약 알림(썸네일, 종목 이름, 주황 글씨 "Best 1RM – 112.36 kg")이 뜨고 그 세트의 번호 자리가 금색 메달로 바뀐다(공식 화면). Fitbod의 스토어 이미지에는 "New Strength Record!"와 햇살 무늬 위 노란 육각형 트로피가 있다. 홍보용으로 짠 장면이라 앱 화면 그대로인지는 모른다(스토어).
- 운동 마침: Hevy는 "Nice work! This is your 472nd workout"이라는 문장, 축하 버튼, 넘겨 보는 공유 카드(근육 그림, 7일 점, 숫자와 메달 수, "트럭 한 대를 든 셈" 같은 비유 그림)다(공식 화면).
- 휴식: Hevy는 아래 붙은 판에 파란 진행 선과 큰 숫자, Strong은 세트 행 사이 파란 진행 막대이고 끝난 휴식은 가는 선과 초록 시간으로 접힌다. Ladder는 눈금이 있는 반원 게이지다(스토어, 공식 화면).
- 소리: Hevy는 체크, 최고 기록, 타이머 소리의 크기를 따로 정하게 한다. Strong에도 타이머 소리 설정이 있다(공식).
- 꾸준함 표시: Gravl의 불꽃 알약("8-week streak")과 "+14 this week" 같은 변화량, Ladder의 주간 동그라미가 있다. 디자인 모음의 출시 앱 33장 가운데 약 11장에 요일 줄이 있어 가장 흔한 진행 표시다(스토어, 제3자).
- 꽃가루: Fitbod가 요약 뒤에 꽃가루를 뿌린다는 것은 제3자 설명문에만 있다. 축하 전용 화면은 출시 앱 33장 가운데 3장이다(제3자).

시사점: 반복하는 동작(세트 완료)은 어느 앱도 요란하지 않다. 연출은 최고 기록과 운동 마침 두 곳에 모인다. #4의 원칙 6과 같다. 최고 기록은 "그 자리의 표시가 바뀜 + 화면 위 한 줄", 운동 마침은 "문장 한 줄 + 숫자 + 이번 주 표시"가 공통된 뼈대다.

## 디자인 모음에서 반복되는 패턴

Mobbin의 건강·운동 분류에서 출시된 앱 화면 33장(21개 앱), Layers의 "workout" 검색에서 시안 24장을 봤다. Dribbble과 Behance는 자동 접근을 막아 보지 못했다(아래 "확인하지 못한 것").

| 패턴 | 출시 앱 33장 | 시안 24장 |
|---|---|---|
| 어두운 화면 | 14장 | 13장 |
| 어두운 화면에 선명한 강조색 하나 | 14장 가운데 5장 | 13장 가운데 약 11장 |
| 숫자가 화면의 주인공 | 약 6장 | 7장 |
| 사진·영상이 화면 위쪽을 차지 | 약 17장 | 10장 |
| 일러스트 | 3장 | 0장 |
| 근육 그림 | 3장 | 0장 |
| 링·게이지 | 3~4장 | 2장 |
| 칸을 나눈 격자(벤토) | 0장 | 3장 |
| 눈에 띄는 그라데이션 | 약 8장 | 9장 |
| 축하 화면 | 3장 | 1장 |
| 요일 줄 | 약 11장 | 세지 않음 |

- 출시된 앱에 남아 있는 것: 채움만으로 만든 층, 강조색 하나, 화면 아래 알약 주 버튼, 라벨보다 큰 숫자, 요일 줄이다.
- 그래프는 두 쪽 모두 둥근 막대나 가는 선이고, 막대 하나나 끝점 하나만 강조하며, 눈금선이 거의 없다.
- 시안에만 있고 한 손 기록 화면에는 해로운 것: 기울이거나 잘라 낸 화면, 벤토 대시보드, 전광판 숫자, 돌리는 휠 입력, 데이터 뒤의 그라데이션 바탕, 32pt 이하의 누르는 영역이다.
- "어두운 바탕에 형광 강조색 하나"는 시안에서는 거의 표준이지만 출시 앱에서는 어두운 화면의 3분의 1쯤이다. 나머지는 무채색이거나 사진이 색을 맡는다.

## 플랫폼 기준: Material 3 Expressive

### 이 저장소가 쓰는 버전

- `gradle/libs.versions.toml`의 `composeBom`은 `2026.09.00`이다. `./gradlew :app:dependencies --configuration debugRuntimeClasspath`로 보면 `androidx.compose.material3:material3`는 **1.4.0**으로 풀린다. BOM의 pom 파일에도 1.4.0으로 적혀 있다(2026-11-21 확인).
- 1.4.0은 2025-09-24에 나온 안정판이다. 다음 안정판 1.5.0은 2026-11-18에 나왔고 BOM `2026.11.01`이 1.5.0을 가리킨다(공식, 릴리스 노트와 BOM pom).
- #4의 문서는 2026-11-17에 "1.5.0-rc01 기준"으로 적었다. 그 뒤 1.5.0이 안정판이 됐으므로 아래 표로 바꿔 읽는다.
- 확인 방법: Google Maven에서 `material3-android`의 1.4.0과 1.5.0 소스 묶음(`-sources.jar`)을 받아 선언을 읽었다. `internal`이면 앱에서 부를 수 없다. `@ExperimentalMaterial3ExpressiveApi`나 `@ExperimentalMaterial3Api`가 붙어 있으면 옵트인이 필요한 실험 API다. `DESIGN.md`의 판정 규칙("`@OptIn` 없이 컴파일되는 API만 쓴다")과 같은 기준이다.

### 안정판으로 쓸 수 있는 것과 없는 것

"지금"은 이 저장소가 쓰는 1.4.0, "올리면"은 BOM을 `2026.11.01`로 올려 1.5.0을 쓸 때다. 모두 공식(소스)로 확인했다.

Expressive에서 새로 나온 것

| 항목 | 지금(1.4.0) | 올리면(1.5.0) |
|---|---|---|
| `MaterialExpressiveTheme` | 쓸 수 없다. `internal`이다 | 쓸 수 있다 |
| `MotionScheme`(`standard()`, `expressive()`), `MaterialTheme.motionScheme` | 쓸 수 없다. `internal`이다 | 쓸 수 있다. API 문서에 "Added in 1.5.0"으로 나온다 |
| 강조 타이포 `Typography`의 `…Emphasized` 15개 | 쓸 수 없다. 속성과 생성자가 `internal`이다. 값은 토큰 파일에 있다 | 쓸 수 있다 |
| 큰 모서리 단계 `Shapes.largeIncreased`(20dp), `extraLargeIncreased`(32dp), `extraExtraLarge`(48dp) | 쓸 수 없다. `internal`이다 | 쓸 수 있다 |
| `ButtonGroup` | 없다. 토큰 파일만 있다 | 쓸 수 있다 |
| `SplitButtonLayout` | 없다 | 쓸 수 있다 |
| `HorizontalFloatingToolbar`, `VerticalFloatingToolbar` | 없다 | 쓸 수 있다 |
| `ToggleButton` 계열, 누르면 모서리가 바뀌는 `ButtonShapes` | 없다 | 쓸 수 있다 |
| 물결 진행 표시 `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator` | 없다 | 쓸 수 있다 |
| 묶음 목록 `SegmentedListItem` | 없다 | 쓸 수 있다 |
| `FloatingActionButtonMenu` | 없다 | 쓸 수 있다 |
| `LoadingIndicator`, `ContainedLoadingIndicator` | 없다 | **쓸 수 없다.** 실험 API다 |
| `MaterialShapes`(쿠키·꽃 같은 도형 35개), `RoundedPolygon.toShape()` | 없다 | **쓸 수 없다.** 실험 API다 |

전부터 있던 것

| 항목 | 지금(1.4.0) | 올리면(1.5.0) |
|---|---|---|
| `ColorScheme`의 표면 단계 `surfaceContainerLowest`·`Low`·`surfaceContainer`·`High`·`Highest`, `surfaceDim`, `surfaceBright` | 쓸 수 있다 | 쓸 수 있다 |
| `Shapes`의 `extraSmall`(4dp)·`small`(8dp)·`medium`(12dp)·`large`(16dp)·`extraLarge`(28dp) | 쓸 수 있다 | 쓸 수 있다 |
| `Card`, `ElevatedCard`, `OutlinedCard`, `ListItem`, `FilterChip`, `SegmentedButton`, `Badge`, `HorizontalDivider`, `AlertDialog` | 쓸 수 있다 | 쓸 수 있다 |
| `Text`의 `autoSize` | 쓸 수 있다 | 쓸 수 있다 |
| `TopAppBar` | 쓸 수 없다. 실험 API다(#28에서 컴파일로 확인) | 쓸 수 있다 |
| `BasicAlertDialog` | 쓸 수 없다. 실험 API다 | 쓸 수 있다 |
| `ModalBottomSheet` | 쓸 수 없다. 실험 API다(#28에서 컴파일로 확인) | 쓸 수 없다. 실험 API다 |

정리

- **지금 버전에서는 Expressive 전용 API를 하나도 쓸 수 없다.** 숨겨져 있거나 아직 없다. 1.4.0 소스에는 `@ExperimentalMaterial3ExpressiveApi`가 붙은 공개 선언이 없다. 1.4.0 릴리스 노트는 이 표시가 붙은 공개 API를 모두 뺐으니 계속 쓰려면 1.5.0 알파로 옮기라고 적는다(공식).
- 1.5.0으로 올리면 테마, 모션 스킴, 강조 타이포, 큰 모서리 단계, 버튼 묶음, 묶음 목록이 안정 API가 된다. `LoadingIndicator`와 `MaterialShapes`는 1.5.0에서도 실험 API다. 릴리스 노트의 "Remaining experimental APIs"에도 이 둘이 적혀 있다.
- `AGENTS.md`는 의존성을 작업 시점의 최신 안정 버전으로 쓰라고 적는다. 1.5.0은 조사 사흘 전에 나왔다. BOM을 올리는 일은 이 조사의 범위가 아니므로 #89 앞에 따로 이슈를 두는 것을 제안한다. 릴리스 노트가 적은 동작 변경 가운데 이 앱에 닿는 것은 `Checkbox`의 크기 보정(앱에 한 곳)이고, `Slider` 변경은 앱이 `Slider`를 쓰지 않아 닿지 않는다. 기준 스크린샷은 다시 봐야 한다.
- 올리지 않아도 Expressive의 **값**은 안정 API로 옮길 수 있다. 아래 표의 값은 1.4.0 소스의 토큰 파일에서 읽었다.

| 가져올 값 | 내용 | 지금 버전에서 옮기는 법 |
|---|---|---|
| 모서리 단계 | 0 / 4 / 8 / 12 / 16 / 20 / 28 / 32 / 48dp / 완전히 둥긂. 열 단계다 | 앱의 `NextSetRadius`에 단계를 더하고 `RoundedCornerShape`로 만든다 |
| 컴포넌트별 기본 모서리 | 카드 12dp, 대화상자 28dp, 하단 시트 위쪽 28dp, 칩 8dp. 버튼은 높이 56dp일 때 둥근 모양(완전히 둥긂)과 각진 모양(16dp) 둘이 있고 눌렀을 때 12dp가 된다 | 버튼에 모양을 직접 넘긴다(지금도 그렇게 한다) |
| 강조 타이포의 굵기 | 크기는 기본 스타일과 같고 굵기만 올린다. display·headline·titleLarge·body는 Regular(400) → Medium(500), titleMedium·titleSmall·label은 Medium(500) → Bold(700) | 앱의 타입 토큰에 굵기 단계를 더한다 |
| 표면 단계의 밝기 | 어두운 화면: 바탕 톤 6, 컨테이너 톤 4 / 10 / 12 / 17 / 22, 밝은 표면 톤 24. 밝은 화면: 바탕 톤 98, 컨테이너 톤 100 / 96 / 94 / 92 / 90, 어두운 표면 톤 87 | `ColorScheme`의 표면 단계 역할에 고정 색을 넣는다 |
| 카드의 기본 층 | 채운 카드는 `surfaceContainerHighest`에 그림자 없음, 올린 카드는 `surfaceContainerLow`에 그림자 1dp, 테두리 카드는 `surface`에 테두리 | 같은 역할을 쓴다 |
| 스프링 값 | standard와 expressive의 감쇠·강성 | 이미 `DESIGN.md` "모션"과 `ui/theme/Motion.kt`에 옮겨 두었다 |

M3 표면 단계 사이의 대비(계산, M3 기본 팔레트의 색 값으로 WCAG 대비 공식을 적용)

| | 바탕 대비 낮은 단계 | 중간 | 높은 단계 |
|---|---|---|---|
| 어두운 화면(바탕 톤 6) | 톤 12: 1.14 | 톤 17: 1.29 | 톤 22: 1.51 |
| 밝은 화면(바탕 톤 98) | 톤 94: 1.09 | 톤 92: 1.16 | 톤 90: 1.23 |

지금 앱은 `surface` / `bg`가 밝은 화면 1.07, 어두운 화면 1.09다. 톤으로 읽으면 밝은 화면은 97과 100, 어두운 화면은 5와 10이다(계산). M3 기준으로는 가장 낮은 한 단계만 쓰고 있는 셈이다.

### 디자인 지침에서 가져올 것

- 모양은 눈길을 끌고, 요소의 종류를 알려 주고, 상태를 전하고, 브랜드를 드러내는 데 쓴다. M3의 모양 단계는 타입 단계처럼 여러 단을 두어 화면 안에서 차이를 만든다(공식, Compose Material 3 문서).
- 강조는 색 조합과 글자 굵기 두 가지로 준다. 표면 색과 그 위 글자색의 조합을 바꾸거나, 타입 단계의 굵기를 달리한다(공식, 같은 문서의 "Emphasis").
- 높이는 그림자보다 톤 차이로 먼저 나타낸다. 더 높은 요소일수록 더 도드라진 톤을 쓴다(공식, 같은 문서의 "Elevation").
- expressive 모션 스킴은 눈에 띄는 요소와 중요한 순간에, standard는 실용적인 요소와 반복되는 동작에 쓴다(공식, `MotionScheme` API 문서). #4의 원칙 6과 같다.
- Google 연구 글의 경고는 그대로다. 표현적인 디자인이 기본 상호작용 틀을 깨면 사용성이 떨어진다(#4 문서의 "주의할 것").

## 도구

### 색 단계: Material Theme Builder와 material-color-utilities

- Material Theme Builder는 고른 색에서 밝은 화면·어두운 화면의 색 역할 전체를 만들고 Compose 테마 코드(`Color.kt`, `Theme.kt`)로 내보낸다(공식, Compose Material 3 문서). 웹 도구 주소는 조사일에 열렸다. 도구의 GitHub 저장소는 보관 처리돼 읽기 전용이다. 라이선스는 Apache-2.0이다(공식, GitHub).
- 저장소 README는 배경화면 대신 핵심 색을 직접 넣어 스킴을 만들 수 있고, 색 역할에 없는 색을 "확장 색"으로 더할 수 있다고 적는다(공식). 완료 색 초록은 확장 색 자리에 맞는다.
- 다이내믹 컬러를 쓰지 않으려면 내보낸 고정 값만 쓰고 `dynamicLightColorScheme`·`dynamicDarkColorScheme`을 부르지 않으면 된다. 지금 테마가 이미 그렇게 돼 있다(NF-05).
- 톤 계산만 필요하면 `material-color-utilities`를 쓴다. Theme Builder가 쓰는 색 계산 라이브러리이고 Apache-2.0이며 조사일에도 커밋이 올라온다(공식, GitHub). 이 문서의 톤 값은 npm 0.4.0 묶음으로 계산했다. 개발 중 계산에만 쓰고 앱에는 넣지 않는다.
- 도구가 낸 값도 그대로 쓰지 않는다. `DESIGN.md`의 대비 표를 다시 계산하고, 문서와 테마의 값이 어긋나면 실패하는 `DesignTokenSyncTest`를 통과해야 한다.

### 아이콘: Material Symbols 변형

- 지금 쓰는 것: Rounded 모양, 굵기 400, 채움 0, 등급 0, 24px 벡터 XML 16개(`DESIGN.md` "UI 라이브러리 선택").
- 축은 넷이다. 채움(FILL) 0~1, 굵기(wght) 100~700, 등급(GRAD) −50~200, 광학 크기(opsz) 20~48이다(공식, Material Symbols 안내).
  - 채움은 선택된 상태처럼 상태 변화를 나타내는 데 쓴다.
  - 등급은 굵기보다 잘게 두께를 조절한다. 어두운 바탕 위 밝은 아이콘은 번져 보이므로 낮은 등급으로 눈부심을 줄이라고 안내한다.
  - 광학 크기는 아이콘을 키우거나 줄일 때 선 굵기를 맞춰 준다.
- 인터넷 없이 쓰는 법은 지금과 같다. `google/material-design-icons` 저장소의 `symbols/android/<이름>/materialsymbolsrounded/`에 변형별 벡터 XML이 따로 있다. 파일 이름은 `<이름>[_wght100~700][_gradN25|_grad200][_fill1]_<20|24|40|48>px.xml` 꼴이다. 예: `trophy_fill1_24px.xml`, `trophy_wght300_48px.xml`(공식, 저장소 목록을 직접 봄). 굵기 400과 등급 0, 채움 0은 이름에 적지 않는다.
- 저장소에 있는 정적 변형은 굵기 일곱(100~700), 등급 셋(−25, 0, 200), 채움 둘(0, 1), 크기 넷(20, 24, 40, 48)이다. 안내 문서의 등급 범위 −50~200 가운데 −50은 정적 파일로는 없다.
- 가변 글꼴 파일(`MaterialSymbolsRounded[FILL,GRAD,opsz,wght].ttf`)은 15.2MB다. 아이콘 스무 개 남짓을 쓰려고 넣기에는 크다. Compose에서 가변 글꼴의 축을 정하는 `Font(…, variationSettings)`도 실험 API(`ExperimentalTextApi`)가 걸려 있다(공식(소스), ui-text 1.12.1). 벡터 XML을 변형별로 넣는 쪽이 맞다.
- 라이선스는 Apache-2.0이다. 저장소 README는 앱의 정보 화면에 출처를 적어 주면 좋지만 필수는 아니라고 적는다(공식). 이 앱은 이미 `README.md`의 "아이콘" 절과 설정의 "오픈소스 라이선스"에 적고 있다.

### 빈 상태 그림 세트

라이선스는 각 세트의 라이선스 페이지에서 조사일에 읽었다.

| 세트 | 라이선스 | 출처 표시 | 이 앱에 맞는가 |
|---|---|---|---|
| Material Symbols 큰 크기(40·48px 변형) | Apache-2.0 | 필수 아님. 이미 적고 있다 | 맞다. 새 라이선스도 새 파일 형식도 없다. 지금 아이콘과 같은 모양이라 화면이 어긋나지 않는다 |
| 직접 그린 벡터 | 저장소와 같은 MIT | 없음 | 맞다. 단순한 선 그림 몇 개면 된다. 그릴 사람이 필요하다 |
| Open Peeps | CC0 | 없음 | 쓸 수는 있다. 손으로 그린 사람 그림이라 지금 화면의 선 아이콘과 결이 다르다. SVG를 벡터 XML로 바꿔야 한다 |
| Open Doodles | CC0 | 없음 | 위와 같다 |
| Humaaans | CC0 | 없음 | 위와 같다. 색을 채운 사람 그림이다 |
| Lukasz Adam 일러스트 | 페이지가 "CC0 license (MIT license)"라고 둘을 함께 적는다 | 없음 | 보류. 어느 라이선스인지 페이지만으로는 갈리지 않는다 |
| unDraw | 자체 라이선스 | 없음 | 맞지 않는다. 무료로 고쳐 쓸 수 있지만 그림을 묶음으로 다시 배포하는 것을 막는다. 요약문은 그림을 "앱에 넣는" 경우를 하지 말아야 할 예로 든다. 공개 저장소에 파일로 올리면 MIT로 다시 배포할 수 없는 파일이 섞인다 |
| ManyPixels | 자체 라이선스(unDraw와 거의 같은 문장) | 없음 | 맞지 않는다. 이유도 같다 |
| Storyset(Freepik) | 자체 약관 | 필수. 표시를 빼려면 유료 구독이 필요하다 | 맞지 않는다. 배포·재라이선스·모음에 넣는 것을 막는다 |

- CC0 세트는 법적으로는 가장 자유롭다. 저작권 표시도 필요 없다. 다만 사람 그림은 기록 화면의 "장식을 두지 않는다" 규칙과 부딪히고 어두운 화면용 색을 따로 맞춰야 한다.
- 자체 라이선스 셋은 "앱 화면에 쓰는 것"은 허용하지만 파일 자체를 다시 나눠 주는 것을 제한한다. 공개 저장소는 파일을 그대로 나눠 주는 곳이라 피한다.

### 연출용 라이브러리

| 후보 | 라이선스 | 판단 |
|---|---|---|
| Compose 기본 애니메이션과 `Canvas`로 직접 그리기 | 없음 | 쓴다. 의존성이 늘지 않는다. 지금도 스프링 값을 한곳에 모아 두었다 |
| Konfetti(종이 꽃가루) | ISC | 쓰지 않는다. 연출 한 곳을 위해 의존성을 더하게 된다. 꽃가루 자체도 아래 "가져오지 않을 것"에 넣었다 |
| Lottie | Apache-2.0 | 쓰지 않는다. 애니메이션 파일을 따로 구해야 하고 그 파일마다 라이선스를 확인해야 한다 |

- 진동은 Compose의 `HapticFeedbackType`에 `Confirm`, `Reject`, `ToggleOn`, `SegmentTick` 등이 안정 API로 있다(공식(소스), ui 1.12.1). 최고 기록과 운동 완료의 진동을 고를 때 본다.

## 실기기 문제별 해법

값은 시안(#88)의 출발점이다. 색 예시는 지금 바탕과 같은 색조(색상 274, 채도 5)의 톤 단계를 `material-color-utilities`로 계산한 것이고, 대비는 `DESIGN.md`와 같은 공식으로 계산했다. 확정은 `DESIGN.md`의 대비 표를 다시 채운 뒤에 한다.

### 문제 1. 층이 보이지 않는다

빌려 올 것: Fitbod의 네 단 표면, Gravl의 채움만으로 만든 층, Strong의 1dp 테두리 카드, M3의 표면 단계 역할.

- 표면을 한 단에서 세 단으로 늘린다. 바탕, 카드, 카드 안의 칸(입력 칸·칩·아이콘 바탕), 그리고 화면 위에 뜨는 면(휴식 바, 숫자판, 대화상자)이다. `ColorScheme`의 `surfaceContainerLow`~`Highest`에 고정 색으로 넣으면 M3 컴포넌트도 같은 단계를 따른다(지금 버전에서 안정 API).
- 어두운 화면은 카드를 바탕과 1.25 이상 벌린다. 예시:

  | 단 | 톤 | 예시 색 | 바탕과의 대비 | 그 위 `text2` / `accent` 대비 |
  |---|---|---|---|---|
  | 바탕 | 6 | `#121317` | | 7.64 / 8.60 |
  | 낮은 면(화면을 나누는 넓은 구역) | 12 | `#1F1F24` | 1.13 | 6.75 / 7.60 |
  | 카드 | 17 | `#292A2E` | 1.29 | 5.90 / 6.64 |
  | 칸, 뜨는 면 | 22 | `#343439` | 1.49 | 5.09 / 5.73 |

  톤 24부터는 `text2`가 4.75로 기준(4.5)에 바짝 붙는다. 글자를 얹는 면은 톤 22를 넘기지 않는다.
- 밝은 화면은 두 길이 있다. (가) 바탕을 톤 94(`#EFEDF3`)로 내리고 카드를 흰색으로 둔다. 대비가 1.07에서 1.16이 된다. (나) 바탕은 그대로 두고 카드에 `line` 색 1dp 테두리를 두른다(Strong). 어느 쪽이든 카드 안의 칸은 톤 92(`#E9E7ED`, 흰 카드와 1.22)로 채운다. Strong의 밝은 화면 채움(1.20)과 같은 정도다. 톤 92 위에서 `text2`는 5.21, `accent`는 6.65, `done`은 4.36이다. `done`을 글자로 쓰려면 톤 96보다 밝은 면에만 둔다.
- 그림자는 쓰지 않는다. 본 앱들이 채움으로 층을 만들고, 어두운 바탕에서는 그림자가 잘 보이지 않는다.
- 바탕의 색조를 강조색 쪽(색상 286)으로 조금 더 준다. 지금 채도는 4~8이다. Fitbod와 Gravl처럼 색조가 느껴지되 무채색 글자와 부딪히지 않는 범위를 시안에서 비교한다.
- 함께 고칠 것: 카드를 톤 17로 올리면 지금의 `accentSoft`(어두운 화면 `#25254A`)가 카드와 1.01, `doneSoft`(`#163726`)가 1.09, `line`(`#2B2E39`)이 1.05로 묻힌다(계산). 카드 위에서 쓰는 옅은 색과 구분선을 같이 다시 정해야 한다. 세트 표는 지금처럼 바탕 위에 두면 `doneSoft`의 1.44가 유지된다.
- 강조색으로 면을 칠해 층을 만들지 않는다. 강조색은 누르는 요소에만 쓴다는 규칙을 지킨다.

### 문제 2. 줄과 구분선뿐인 목록, 기본 모양 그대로인 요소

빌려 올 것: Gravl의 틈으로 띄운 카드 목록과 알약 주 버튼, Strong의 행 앞 아이콘 칩, Fitbod·Liftin'의 되풀이하는 도형, M3의 모서리 단계와 묶음 목록.

- 모서리를 8dp 하나에서 네 단으로 늘린다. 큰 요소일수록 둥글게 한다.

  | 단 | 값 | 쓰임 | 근거 |
  |---|---|---|---|
  | 작게 | 8dp | 입력 칸, 세트 칸, 체크 버튼, 숫자판 키. 지금 그대로 | Hevy 약 8dp, Gravl 약 10pt, M3 `small` |
  | 가운데 | 16dp | 카드, 목록 묶음, 행 앞 아이콘 바탕 | Gravl 카드 약 12~16pt, M3 `large` |
  | 크게 | 28dp | 대화상자, 하단 바와 시트의 위쪽 | M3 대화상자·시트 기본값 |
  | 알약 | 높이의 절반 | 칩, 배지, 주 버튼 | Gravl·Ladder의 주 버튼, 세 앱의 칩 |

- 설정·기록·종목 목록은 구분선을 걷고 묶음 카드로 바꾼다. 묶음의 바깥 모서리는 16dp, 안쪽 행 사이는 가는 틈이나 안으로 들인 구분선으로 나눈다. M3 1.5.0의 묶음 목록 토큰은 바깥 16dp, 안쪽 4dp다(공식(소스)). 묶음 사이는 8~12dp를 띄우고(Gravl 약 10pt) 묶음 위에 작은 라벨을 둔다.
- 글자뿐인 행 앞에 아이콘 바탕(40dp, 카드보다 한 단 밝은 톤)을 둔다(Strong). 행은 누르는 요소이지만 아이콘 바탕은 무채색으로 두어 강조색 면적을 늘리지 않는다.
- 버튼을 세 단계로 나눈다. 지금은 글자 버튼이 대부분이다. 주 버튼은 채운 알약(높이 56dp, 지금 높이 그대로), 둘째는 톤 채움 바탕에 강조색 글자, 셋째는 글자 버튼이다. 톤 22 위 강조색 글자는 5.73으로 기준을 넘는다.
- 칩은 알약 모양에 테두리 대신 톤 채움을 쓰고, 선택되면 지금처럼 `accentSoft`와 강조색 글자다(Hevy의 회색 칩과 파란 선택 칩).
- 대화상자는 모서리 28dp를 유지하되 안의 버튼을 글자 버튼 둘에서 톤 버튼과 채운 버튼으로 바꿔 무게를 준다.
- 되풀이할 도형을 하나 정한다. 세트 번호, 최고 기록 배지, 요일 동그라미, 빈 상태 아이콘 바탕에 같은 도형을 쓴다. `MaterialShapes`는 1.5.0에서도 실험 API이므로 원, 알약, 둥근 사각형처럼 `RoundedCornerShape`로 만드는 도형에서 고른다.
- 누르는 영역 48dp와 요소 사이 8dp는 그대로다. 모양만 바꾸고 크기는 줄이지 않는다.

### 문제 3. 글자의 강약이 약하다

빌려 올 것: Fitbod·Gravl의 본문 3~4배 대표 숫자, Hevy의 작은 라벨과 굵은 값, Ladder의 굵은 값과 가는 단위, M3 강조 타이포의 굵기 올리기.

- 대표 숫자를 본문의 3배 이상으로 키운다. `numeralLarge`를 40sp에서 56sp 안팎으로 올리면 3.5배다. 지금도 `autoSize`로 줄어들게 돼 있어(하한 28sp) 글자 크기 200%에서 넘치지 않는다. 한 화면 한 곳 규칙은 그대로다.
- 가운데 단계를 벌린다. 지금은 22 / 20 / 18 / 16sp가 2sp씩만 다르다. 이웃한 단이 1.2배 이상 벌어지게 한다. 예시: 13 / 16 / 20 / 24 / 32 / 56sp.

  | 쓰임 | 지금 | 예시 |
  |---|---|---|
  | 대표 숫자 | 40sp Bold | 56sp ExtraBold |
  | 화면의 결론 문장, 휴식 남은 시간 | 28sp Bold | 32sp ExtraBold |
  | 화면 제목 | 22sp Bold | 24sp Bold |
  | 구역·카드 제목, 종목 머리 | 18sp SemiBold | 20sp Bold |
  | 목록 행 제목 | 16sp Regular | 16sp SemiBold |
  | 본문 | 16sp Regular | 그대로 |
  | 구역 라벨, 표 머리 | 14sp SemiBold | 13sp SemiBold, `text2` 색 |
  | 보조 글자, 단위 | 13sp Regular | 그대로 |

  세트 행의 값(`numeralRow` 20sp Bold)은 그대로 둔다. 기록 화면의 밀도를 바꾸지 않는다.
- 굵기를 한 단 더한다. ExtraBold(800)를 대표 숫자와 결론 문장에만 쓴다. Pretendard v1.3.9의 `Pretendard-ExtraBold.otf`는 1,568,512바이트다(공식, 저장소 목록). 지금 세 파일이 APK 안에서 약 3.2MB이므로 약 1MB가 더 든다고 어림한다.
- M3 강조 타이포처럼 크기는 두고 굵기만 올리는 자리를 만든다. 목록 행 제목을 Regular에서 SemiBold로 올리면 제목과 보조 줄이 갈린다.
- 영어 앱의 대문자 라벨이 하는 일을 한글에서는 "작게, SemiBold, `text2` 색" 한 가지 스타일로 한다. 구역 라벨, 표 머리, 숫자 아래 라벨에 똑같이 쓴다.
- 단위는 값보다 작고 가늘게 한다(Ladder). 지금 `numeralLarge` 옆 단위는 SemiBold다. Regular에 `text2` 색으로 내리면 숫자가 더 선다.
- 지킬 것: 모든 스타일의 `tnum`, 같은 열 숫자의 굵기 통일, sp 단위, 어절 단위 줄바꿈. 32sp 문장은 200%에서 두 줄 이상이 되므로 줄바꿈을 막지 않는다.

### 문제 4. 빈 상태와 순간의 연출

빌려 올 것: Hevy의 빈 운동 화면 구성과 최고 기록 알림, Hevy 마침 화면의 첫 문장, Ladder의 주간 동그라미, Gravl의 변화량 표시, Material Symbols의 큰 크기 변형.

빈 상태

- 틀을 하나로 정한다. 위에서부터 도형 바탕(약 96dp, 카드 톤) 안의 큰 아이콘, 제목(`heading`), 한 문장(`body`, `text2` 색), 버튼 하나다. Hevy의 빈 운동 화면과 같은 구성이다.
- 아이콘은 Material Symbols Rounded의 48px 변형을 쓴다. 큰 크기에서는 굵기 300이 덜 둔하다(`<이름>_wght300_48px.xml`). `fitness_center`, `exercise`, `history`, `calendar_month`, `search_off`, `event_available`이 저장소에 이 변형으로 있다(공식, 파일을 직접 확인).
- 홈(루틴 없음), 기록 목록(기록 없음), 종목 고르기(검색 결과 없음)에 쓴다. 운동 중 화면과 기록 수정 화면의 빈 상태는 지금처럼 문구와 버튼만 둔다. 기록 화면에 그림을 두지 않는 규칙을 지킨다.
- 첫 화면이 비어 보이지 않게 예시 루틴을 깔아 두는 방법(Strong, #4)은 기능 변경이라 이 문서의 범위가 아니다.

최고 기록

- 지금 설계(세트 행의 배지, 화면 아래 알림 한 줄, 진동 한 번)는 Hevy와 뼈대가 같다. Hevy는 세트 번호 자리를 메달로 바꾸고 화면 위에 알약 알림을 띄운다.
- 더할 곳은 운동 요약이다. 최고 기록을 줄 하나가 아니라 카드 하나로 올린다. 도형 바탕 안의 트로피 아이콘(채움 변형 `trophy_fill1`)이 작게 시작해 살짝 넘쳤다가 제 크기가 된다. 스프링은 이미 정해 둔 expressive 빠름(감쇠 0.6, 강성 800)이다. 진동은 `HapticFeedbackType.Confirm`을 후보로 본다.
- Fitbod처럼 트로피 뒤에 햇살 무늬를 둘 수 있다. `Canvas`로 무채색 선을 그리면 색과 라이브러리가 늘지 않는다. 요약 화면에만 둔다.
- 색은 정해야 할 것이 남는다. Hevy(금색)와 Fitbod(노랑)는 기록에 셋째 색을 쓴다. 우리는 셋째 색을 쓰지 않고 반전 배지로 구분하기로 했다. 시안에서 반전 배지만으로 충분히 눈에 띄는지 보고, 모자라면 요약 화면에 한해 색을 더할지 소유자가 정한다.

운동 마침

- 첫 줄은 문장이다. Hevy의 "472번째 운동"처럼 횟수를 넣으면 숫자 없이도 쌓인 것이 보인다. 지금 설계의 결론 문장 자리에 맞는다.
- 문장 아래에 이번 주 요일 줄을 둔다. 운동한 날은 완료 색으로 채운 동그라미와 체크, 안 한 날은 빈 동그라미다(Ladder). 출시 앱에서 가장 흔한 진행 표시다. 초록은 "끝남"만 뜻한다는 규칙에 맞고, 체크 모양을 함께 쓰므로 NF-06도 지킨다.
- 숫자 옆에 직전 대비 변화량을 글자로 붙인다(Gravl의 "+14 this week"). 색은 본문 색이다. 오르내림을 초록과 빨강으로 칠하지 않는다.
- 숫자와 문장이 차례로 올라오는 지금의 expressive 연출은 그대로 둔다.

세트 완료와 휴식 끝

- 세트 완료는 지금처럼 색만 바꾼다. 본 앱 넷이 모두 그렇게 한다.
- 휴식이 끝나는 순간을 화면에서 어떻게 보여 주는지는 어느 앱에서도 확인하지 못했다. 지금 설계(REST-04의 진동과 알림)를 유지한다.

## 우리 앱에 가져올 것

| # | 가져올 것 | 어디서 | 왜 |
|---|---|---|---|
| 1 | 표면 단계를 셋 이상으로 늘리고, 어두운 화면의 카드를 바탕과 1.25 이상 벌린다 | Fitbod, Gravl, Strong, M3 표면 단계 | 문제 1. 지금은 1.09라 카드가 보이지 않는다 |
| 2 | 층은 채움으로 만든다. 밝은 화면에서 모자라면 1dp 테두리를 쓴다 | 본 앱 전부, Strong의 카드 | 그림자는 어두운 화면에서 보이지 않는다 |
| 3 | 모서리 네 단(8 / 16 / 28dp / 알약) | M3 모서리 단계, Gravl | 문제 2. 요소의 크기와 종류가 모양으로 갈린다 |
| 4 | 구분선 목록을 묶음 카드로 바꾸고 묶음 위에 작은 라벨을 둔다 | Gravl, M3 묶음 목록 | 문제 2. 사진이 없는 앱은 면으로 덩어리를 만들어야 한다 |
| 5 | 버튼 세 단계(채운 알약, 톤 채움, 글자) | Gravl, Ladder, 출시 앱 모음 | 문제 2. 글자 버튼만으로는 무게 차이가 없다 |
| 6 | 대표 숫자를 본문의 3배 이상으로, 굵기 한 단(ExtraBold) 추가 | Fitbod, Gravl, Ladder | 문제 3 |
| 7 | 가운데 글자 단계를 1.2배 이상씩 벌리고 목록 행 제목의 굵기를 올린다 | M3 강조 타이포, Hevy | 문제 3 |
| 8 | 라벨 스타일 하나(작게, SemiBold, 보조 색) | Hevy·Strong·Fitbod의 대문자 라벨 | 문제 3. 한글에서 대문자가 하는 일을 대신한다 |
| 9 | 되풀이하는 도형 하나 | Fitbod의 육각형, Liftin'의 동그라미 | 앱만의 인상. 색을 늘리지 않고 만든다 |
| 10 | 아이콘 변형 쓰기: 선택은 채움(`_fill1`), 빈 상태는 48px | Hevy·Fitbod·Gravl의 아이콘, Material Symbols 안내 | 문제 4. 새 라이선스와 인터넷 의존이 없다 |
| 11 | 빈 상태 틀(도형 안 아이콘, 제목, 한 문장, 버튼) | Hevy | 문제 4 |
| 12 | 운동 요약에 횟수 문장, 요일 줄, 최고 기록 카드, 글자 변화량 | Hevy, Ladder, Gravl | 문제 4. 연출을 드문 순간에만 모은다 |
| 13 | Compose BOM을 `2026.11.01`로 올리는 이슈를 따로 둔다 | 릴리스 노트, `AGENTS.md`의 최신 안정 버전 규칙 | `MotionScheme`, 강조 타이포, 큰 모서리 단계, 묶음 목록, `TopAppBar`가 안정 API가 된다 |

어두운 화면의 아이콘 등급을 낮추는 것(`_gradN25`)은 Material Symbols 안내가 권하지만 본 앱에서 확인한 것은 아니다. 시안에서 번져 보일 때만 쓴다.

## 가져오지 않을 것

| 가져오지 않을 것 | 어디에 있나 | 왜 |
|---|---|---|
| 종목 사진, 영상, 3D 해부 그림, 근육 그림 | Fitbod, Gravl, Ladder, Hevy, Strong | 기록 화면에 그림을 두지 않기로 했다. 용량이 크고 종목마다 라이선스를 확인해야 한다. 인터넷 없이 모두 앱에 넣어야 한다 |
| 형광 라임 같은 새 강조색 | Gravl, Ladder, 시안 13장 가운데 약 11장 | 강조색은 남보라로 정했다(NF-05). 시안에서는 거의 표준이라 오히려 앱을 구분해 주지 못한다 |
| 강조색 하나로 행동과 완료를 함께 표시 | Ladder | 누름과 완료를 색으로 나누는 규칙과 어긋난다 |
| 점수 등급, 세트 종류, 강도마다 다른 색 | Gravl, Hevy, Strong | 기록 화면의 색은 두 가지 뜻만 가진다(#4 원칙 4) |
| 그라데이션 바탕, 가장자리 색 번짐 | Liftin', 시안 | 글자 대비를 화면 위치마다 보장하기 어렵다. 밝은 화면과 어두운 화면을 따로 맞춰야 한다 |
| 그림자로 만든 높이 | M3 `ElevatedCard` | 어두운 화면에서 보이지 않고, 본 앱들도 쓰지 않는다 |
| 기울임 숫자, 폭이 좁거나 넓은 제목 글꼴, 전광판 숫자 | Fitbod, Ladder, 시안 | Pretendard 정적 파일에 기울임이 없다. 글꼴을 더하면 용량과 라이선스가 늘고 한글은 그 모양을 따라가지 못한다 |
| 대문자 라벨 | Hevy, Strong, Fitbod, Gravl | 한글에 대문자가 없다. 굵기와 색으로 대신한다 |
| 꽃가루, 축하 전용 화면 | Fitbod(제3자 설명문), 시안 | 여러 색이 있어야 꽃가루로 읽히는데 색을 늘리지 않기로 했다. 출시 앱에서도 드물다(33장 가운데 3장) |
| 공유 카드, 피드, 응원 | Hevy, Ladder | 기록 앱의 범위 밖이다(#4). 인터넷 권한도 없다 |
| 연속 기록 불꽃, 배지 모음 | Gravl, Ladder | 요구사항에 없는 기능이다. 쉬는 날을 실패로 읽히게 한다 |
| 소리 | Hevy, Strong | 요구사항에 없다. 진동과 알림으로 충분한지 먼저 본다 |
| `MaterialShapes`, `LoadingIndicator` | M3 Expressive | 1.5.0에서도 실험 API다(NF-07) |
| Material Symbols 가변 글꼴 | Google Fonts | 15.2MB이고 축을 정하는 Compose API가 실험 API다 |
| unDraw, ManyPixels, Storyset 그림 | | 파일을 다시 배포하는 것을 제한하거나 출처 표시가 필수다. 공개 저장소에 맞지 않는다 |
| Konfetti, Lottie | | 연출 한두 곳을 위해 의존성을 늘리지 않는다 |
| 벤토 대시보드, 휠 입력, 32pt 이하의 누르는 영역 | 시안 | 한 손 입력과 48dp 규칙에 맞지 않는다 |
| 운동 중 화면과 기록 화면의 장식 | | 지금 규칙 그대로다. 이 문서의 해법은 홈, 목록, 설정, 요약, 빈 상태에 먼저 쓴다. 운동 중 화면에는 층(문제 1)과 버튼 단계(문제 2)만 닿는다 |

## 시안에서 견줄 방향

조사에서 인상이 갈리는 길이 셋 보였다. 셋 모두 강조색과 완료색의 뜻, 화면 구성, 누르는 영역은 같다.

| 방향 | 무엇으로 인상을 만드나 | 닮은 앱 | 값의 예 |
|---|---|---|---|
| 가. 층이 있는 기록장 | 남보라 기운의 표면 서너 단과 묶음 카드 | Fitbod, Gravl | 어두운 화면 톤 6 / 12 / 17 / 22, 카드 16dp, 대표 숫자 56sp |
| 나. 굵은 숫자판 | 글자의 크기와 굵기. 면은 두 단만, 구분은 가는 선 | Ladder, Strong | 대표 숫자 64sp ExtraBold, 결론 문장 32sp, 카드 8dp에 1dp 테두리 |
| 다. 둥근 도형 | 되풀이하는 동그라미·알약과 큰 모서리 | Liftin', M3 Expressive | 카드 20~28dp, 세트 번호·요일·배지가 같은 동그라미, 대표 숫자 48sp |

"가"는 지금 방향(차분한 기록장)을 가장 적게 바꾼다. "나"는 색 단계를 거의 늘리지 않아 대비 계산이 단순하다. "다"는 인상이 가장 크게 바뀌고 한 화면에 들어가는 행 수가 줄 수 있다.

## 확인하지 못한 것

화면으로 직접 보지 못한 항목

- 움직임, 진동, 소리 전부. 정지 화면과 공식 글만 봤다. "움직임과 피드백"의 내용은 화면 구성까지다.
- 빈 상태 그림. 이번에 본 스토어 화면에는 빈 상태가 없었다. Hevy의 빈 운동 화면은 #4에서 본 것이다.
- 휴식이 끝나는 순간의 화면. 어느 앱에서도 보지 못했다.
- Gravl, Ladder, Liftin'의 세트 완료 연출, 최고 기록 화면, 운동 마침 화면. 스토어 화면에 없다.
- Fitbod의 실제 기록 축하 화면과 꽃가루. 스토어 이미지는 홍보용으로 짠 장면이고 꽃가루는 제3자 설명문에만 있다.
- Hevy의 지금 어두운 화면. 2022년 iOS 홍보 이미지에서만 봤다. Hevy에서 꽃가루가 실제로 뿌려지는지도 모른다(축하 버튼만 봤다).
- Strong의 새 iOS 화면(v6), 마침 화면, 테마 목록. App Store 이미지는 옛 화면이고 Google Play 이미지가 지금 Android 화면이다. 테마 목록은 제3자 글에만 있다.
- Fitbod의 밝은 화면. 본 화면은 모두 어두웠다. 밝은 화면이 있는지 모른다.
- 밝은 화면 전반. Fitbod, Gravl, Ladder, Liftin'은 어두운 화면만 봤다. 밝은 화면의 층은 Hevy와 Strong 두 앱에서만 봤다.
- 글꼴 이름. 여섯 앱 모두 모양만 보고 적었다.
- 정확한 크기. 모서리, 행 높이, 글자 배율은 기기 틀이 그려진 홍보 이미지에서 어림했다. 색 값도 그 이미지의 근삿값이다.

접근이 막힌 곳

- Dribbble(`/tags/workout-app`, `/search/workout-tracker`): 빈 본문만 돌아왔다. Behance 검색: 403. 두 곳의 시안은 보지 못했다. 시안 쪽 집계는 Layers 한 곳에서만 나왔다.
- Figma 커뮤니티 검색, pageflows.com: 본문이 자바스크립트로 그려지거나 쓸 이미지가 없었다.
- fitbod.me와 그 블로그·도움말: 403. Fitbod는 공식 화면을 하나도 보지 못했고 스토어 화면만 봤다. Hevy 도움말(help.hevyapp.com) 한 건도 403이었다.
- Ladder의 Google Play 페이지를 찾지 못했다. App Store 화면만 봤다.
- m3.material.io의 모양·색 역할 페이지: #4 때와 같이 본문이 자바스크립트로 그려져 읽지 못했다. 모서리 단계, 표면 단계, 강조 타이포의 값은 라이브러리 소스의 토큰 파일로 대신했다. 지침의 글은 developer.android.com 문서에서 읽었다.
- Material Theme Builder 웹 도구: 주소가 열리는 것만 확인했다. 자바스크립트 앱이라 색을 넣고 내보내 보지는 못했다. 내보내는 파일 형식은 Android 공식 문서의 설명이다.

값이 어림이거나 뒤에 확인할 항목

- "해법"의 색 예시는 계산한 값이고 기기 화면에서 본 것이 아니다. 대비 1.25가 실제로 층으로 보이는지는 시안과 기기에서 본다.
- ExtraBold를 더할 때 늘어나는 APK 크기(약 1MB)는 지금 세 파일의 압축 크기에서 어림했다. 이 파일에 `tnum`이 있는지도 넣을 때 확인한다.
- M3 1.5.0 묶음 목록에서 행 사이 틈의 값은 읽지 못했다. 바깥 16dp, 안쪽 4dp만 확인했다.
- 1.4.0의 `TopAppBar`와 `ModalBottomSheet`는 #28에서 컴파일로 확인한 결과를 따랐다. 소스에는 옵트인이 붙은 선언과 붙지 않은 선언이 함께 있다.
- BOM을 올렸을 때 기준 스크린샷이 얼마나 달라지는지는 해 보지 않았다.
- Lukasz Adam 일러스트의 라이선스. 페이지가 CC0와 MIT를 함께 적어 어느 쪽인지 갈리지 않는다.
- Konfetti와 Lottie는 저장소의 라이선스 표시만 확인했다. 매니페스트에 인터넷 권한이 있는지는 보지 않았다.

## 출처

스토어

- Hevy: [App Store](https://apps.apple.com/us/app/id1458862350), [Google Play](https://play.google.com/store/apps/details?id=com.hevy)
- Strong: [App Store](https://apps.apple.com/us/app/id464254577), [Google Play](https://play.google.com/store/apps/details?id=io.strongapp.strong)
- Fitbod: [App Store](https://apps.apple.com/us/app/id1041517543), [Google Play](https://play.google.com/store/apps/details?id=com.fitbod.fitbod)
- Gravl: [App Store](https://apps.apple.com/us/app/id6450921637), [Google Play](https://play.google.com/store/apps/details?id=com.liteup.getgains)
- Ladder: [App Store](https://apps.apple.com/us/app/id1502936453)
- Liftin': [App Store](https://apps.apple.com/us/app/id1445041669)

공식 사이트와 도움말

- Hevy 기능 소개: [Live PR](https://www.hevyapp.com/features/live-pr/), [Workout Log](https://www.hevyapp.com/features/workout-log/), [Shareable](https://www.hevyapp.com/features/shareable/), [Set Types](https://www.hevyapp.com/features/workout-set-types/), [Rest Timer](https://www.hevyapp.com/features/workout-rest-timer/), [Workout Settings](https://www.hevyapp.com/features/workout-settings/)
- Strong: [strong.app](https://www.strong.app/), 도움말 [Set Tags](https://help.strongapp.io/article/166-set-tags), [About Rest Timer](https://help.strongapp.io/article/231-rest-timer), [Exercise Records](https://help.strongapp.io/article/216-exercise-records-screen)
- Gravl: [gravl.ai](https://gravl.ai/)
- Ladder: [joinladder.com](https://www.joinladder.com/)

디자인 모음과 제3자 글

- [Mobbin 건강·운동 분류](https://mobbin.com/explore/mobile/app-categories/health-fitness)
- [Layers "workout" 검색](https://layers.to/search?q=workout)
- screensdesign: [Strong](https://screensdesign.com/apps/strong-workout-tracker-gym-log/), [Fitbod](https://screensdesign.com/showcase/fitbod-gym-fitness-planner)

플랫폼 공식 문서와 소스

- [Compose Material 3 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/compose-material3)(1.4.0, 1.5.0)
- Google Maven의 소스 묶음: [material3-android 1.4.0](https://dl.google.com/android/maven2/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0-sources.jar), [material3-android 1.5.0](https://dl.google.com/android/maven2/androidx/compose/material3/material3-android/1.5.0/material3-android-1.5.0-sources.jar), [ui-text-android 1.12.1](https://dl.google.com/android/maven2/androidx/compose/ui/ui-text-android/1.12.1/ui-text-android-1.12.1-sources.jar), [ui-android 1.12.1](https://dl.google.com/android/maven2/androidx/compose/ui/ui-android/1.12.1/ui-android-1.12.1-sources.jar)
- Compose BOM pom: [2026.09.00](https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom), [2026.11.01](https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.11.01/compose-bom-2026.11.01.pom)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)(색 스킴, 모양, 강조, 높이, Theme Builder)
- [MotionScheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme)
- [Compose의 Material 아이콘](https://developer.android.com/develop/ui/compose/graphics/images/material)
- m3.material.io(본문을 읽지 못함): [Corner radius scale](https://m3.material.io/styles/shape/corner-radius-scale), [Color roles](https://m3.material.io/styles/color/roles)

도구와 라이선스

- Material Theme Builder: [웹 도구](https://material-foundation.github.io/material-theme-builder/), [저장소](https://github.com/material-foundation/material-theme-builder)(Apache-2.0, 보관 처리됨)
- [material-color-utilities](https://github.com/material-foundation/material-color-utilities)(Apache-2.0), [npm 0.4.0 묶음 정보](https://registry.npmjs.org/@material/material-color-utilities/0.4.0)
- Material Symbols: [안내](https://developers.google.com/fonts/docs/material_symbols), [저장소](https://github.com/google/material-design-icons)(Apache-2.0), [변형 파일 목록의 예](https://github.com/google/material-design-icons/tree/master/symbols/android/trophy/materialsymbolsrounded), [가변 글꼴 폴더](https://github.com/google/material-design-icons/tree/master/variablefont)
- Pretendard: [v1.3.9의 정적 파일 목록](https://github.com/orioncactus/pretendard/tree/v1.3.9/packages/pretendard/dist/public/static)
- 그림 세트 라이선스: [unDraw](https://undraw.co/license), [Open Peeps](https://www.openpeeps.com/), [Open Doodles](https://www.opendoodles.com/about), [Humaaans](https://www.humaaans.com/), [Lukasz Adam](https://lukaszadam.com/illustrations), [ManyPixels](https://www.manypixels.co/gallery/license), [Storyset 약관](https://storyset.com/terms)
- 연출 라이브러리: [Konfetti](https://github.com/DanielMartinus/Konfetti)(ISC), [lottie-android](https://github.com/airbnb/lottie-android)(Apache-2.0)
