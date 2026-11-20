# 휴식 타이머의 백그라운드 동작과 Live Updates

조사일: 2026-11-18. 이슈 [#16](https://github.com/zzunsik/next-set/issues/16).

휴식 타이머는 앱이 화면에 없어도 돌고(REST-06) 끝나기 5초 전부터 1초마다 신호를 낸다(REST-04). 구현 전에 Android가 백그라운드 실행, 소리, 알림을 버전마다 어떻게 제한하는지 공식 문서로 확인했다. 대상은 minSdk 26(Android 8.0)부터 targetSdk 37(Android 17)까지다. 기기와 에뮬레이터 실험은 하지 않았다. 실험으로만 알 수 있는 것은 "확인하지 못한 것"에 모았다.

알림 권한을 묻는 방식, 방해 금지 모드의 동작, 알림에 보여 줄 내용, '알람 및 리마인더' 권한을 받지 않는다는 것은 2026-11-18 협의에서 정했다.

근거는 세 종류로 나눠 적었다.

- **문서**: 공식 문서에 적힌 내용이다. 링크를 함께 단다.
- **추론**: 문서 내용에서 끌어낸 판단이다. 문서에 그대로 적혀 있지 않다.
- **제3자**: 공식 문서가 아닌 글에서 읽었다.

## 결론

| 질문 | 결론 |
|---|---|
| 화면 밖에서 1초 간격 신호를 내는 방법(확인 항목 7) | 휴식 동안만 도는 포그라운드 서비스(종류 `specialUse`)가 부분 wake lock을 잡고 소리와 진동을 직접 낸다 |
| 정확한 알람 권한 | 필요 없다. `SCHEDULE_EXACT_ALARM`과 `USE_EXACT_ALARM` 모두 선언하지 않는다 |
| 승격된 알림의 버튼(확인 항목 8) | 붙인다. 버튼은 최대 3개라 시간 줄이기, 시간 늘리기, 건너뛰기가 꼭 맞는다. 화면에 실제로 어떻게 보이는지는 에뮬레이터로 확인해야 한다 |
| 알림에 보여 줄 내용 | 남은 시간, 다음 세트, 버튼 3개(시간 줄이기, 시간 늘리기, 건너뛰기). 시간은 15초씩 바뀐다 |
| 알림 권한을 묻는 때 | 앱이 먼저 묻지 않는다. 휴식 바의 '알림 켜기'를 눌렀을 때만 묻는다. 거절하면 버튼을 숨기고 앱 설정 화면에서만 안내한다 |
| 알림 권한이 없으면 | 타이머와 소리·진동은 동작하고 알림과 상태바 칩만 보이지 않는다(추론, 실기기 확인 필요) |
| 방해 금지 모드 | 켜져 있으면 소리와 진동을 내지 않는다. 휴식 바와 알림 표시는 그대로다 |
| 요구사항대로 되지 않는 경우 | 기기가 알림을 승격하지 않는 경우와 휴식 중에 앱이 강제로 꺼지는 경우다. 둘 다 받아들이고 요구사항 문구를 고쳤다. "요구사항에 대한 결론"에 적었다 |

## 화면 밖에서 1초 간격 신호 내기

### 포그라운드 서비스가 필요한 이유

- 액티비티가 화면에서 내려간 앱은 캐시 상태가 되고 시스템이 언제든 종료한다. Android 14부터는 캐시 상태가 된 직후 백그라운드 작업이 막힌다(문서: [프로세스 수명 주기][process], [Android 14 변경][a14-all]).
- 포그라운드 서비스를 돌리는 프로세스는 "보이는 프로세스"로 취급된다. 포그라운드 프로세스를 살리는 데 꼭 필요할 때만 종료된다(문서: [프로세스 수명 주기][process]).
- Android 17부터 백그라운드에서 소리를 내려면 조건이 붙는다(문서: [백그라운드 오디오 강화][bg-audio]).
  - Android 17에서 도는 모든 앱: 보이는 액티비티가 있거나 `shortService`가 아닌 포그라운드 서비스를 돌리고 있어야 한다.
  - targetSdk 37인 앱: 그 포그라운드 서비스에 사용 중(while-in-use, WIU) 권한이 있어야 한다. 앱이 화면에 보이는 동안 시작했거나 사용자 조작에 응답해 시작한 서비스가 이 권한을 받는다. 정확한 알람 권한이 있고 `USAGE_ALARM` 소리를 내는 앱만 예외다.
  - 조건이 안 맞으면 재생이 예외 없이 조용히 실패한다.
- 진동은 앱이 포그라운드에 있어야 울린다. 백그라운드 앱은 벨소리, 알림, 알람 가운데 하나로 용도를 지정해야 한다(문서: [Vibrator][ref-vib]).
- 화면이 꺼지면 기기는 곧 절전(suspend) 상태로 들어가 CPU가 멈춘다. 화면이 꺼진 동안 CPU를 깨워 두려면 부분 wake lock을 잡는다. 공식 문서는 wake lock을 포그라운드 서비스 안에서 잡으라고 적는다(문서: [기기를 깨워 두는 방법][awake], [wake lock 권장 사항][wl-bp]).

위 조건을 함께 만족하는 방법은 앱이 화면에 보이는 동안 시작한 포그라운드 서비스다.

### 방식 비교

| 방식 | 판정 | 이유 |
|---|---|---|
| A. 휴식 동안 포그라운드 서비스를 돌리고, 서비스가 wake lock을 잡은 채 시각을 재서 신호를 직접 낸다 | 채택 | 버전 26~37 전체에서 같은 코드로 동작한다. 정확한 알람 권한이 필요 없다 |
| B. 서비스 없이 신호 시각마다 정확한 알람을 건다 | 버림 | API 31부터 정확한 알람 권한이 필요하다. Doze 중 `setExactAndAllowWhileIdle()` 알람은 앱마다 9분에 한 번만 울린다(문서: [Doze][doze]). Android 17에서는 포그라운드 서비스 없이 백그라운드 소리가 막힌다(문서: [백그라운드 오디오 강화][bg-audio]) |
| C. 끝나기 10초 전에 정확한 알람으로 `shortService`를 띄운다 | 버림 | Android 17에서 `shortService`는 백그라운드 소리 조건에서 빠진다. 알람으로 백그라운드에서 시작한 서비스는 보통 WIU 권한을 받지 못한다(문서: [백그라운드 오디오 강화][bg-audio]) |
| D. 알림 채널의 소리와 진동에 맡긴다 | 버림 | 채널을 만든 뒤에는 앱이 동작을 바꾸지 못하고 사용자가 정한다(문서: [알림 채널][channels]). 알림을 1초에 여러 번 갱신하면 시스템이 갱신을 버린다(문서: [알림 만들기][build-notif]). 벨소리 모드와 방해 금지 모드에 따라 신호를 나누는 REST-05를 앱이 통제하지 못한다(추론) |

### 서비스 종류 후보(Android 14 이상)

targetSdk 34 이상인 앱은 포그라운드 서비스마다 종류를 선언하고 종류에 맞는 권한을 요청해야 한다(문서: [포그라운드 서비스 종류][fgs-types]).

| 종류 | 권한 | 시작 전 조건 | 실행 시간 제한 | 그 밖의 제한 | 판정 |
|---|---|---|---|---|---|
| `specialUse` | `FOREGROUND_SERVICE_SPECIAL_USE` | 없음 | 없음 | 다른 종류에 해당하지 않는 용도에 쓴다. `<service>` 안에 `<property>`로 용도를 적는다. Play Console에 올리면 이 용도를 심사한다 | 채택 |
| `health` | `FOREGROUND_SERVICE_HEALTH` | `HIGH_SAMPLING_RATE_SENSORS`를 선언하거나 `ACTIVITY_RECOGNITION`, 심박 등 런타임 권한을 하나 이상 받아야 한다 | 없음 | 설명은 "운동 추적 앱 같은 피트니스 범주의 장시간 작업"이다 | 버림. 이 앱은 센서와 활동 인식을 쓰지 않는다. 쓰지 않는 권한을 받아야 한다 |
| `mediaPlayback` | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 없음 | 없음 | 설명은 "백그라운드에서 오디오나 비디오 재생을 이어 간다"이다. targetSdk 35 이상은 `BOOT_COMPLETED`에서 시작하지 못한다 | 버림. 신호음은 미디어 재생이 아니다. Android 17에서 `specialUse`로 소리가 나지 않으면 다시 본다 |
| `shortService` | 없음(`FOREGROUND_SERVICE`만) | 없음 | 약 3분. 넘기면 ANR | sticky 불가, 다른 포그라운드 서비스 시작 불가. Android 17에서 백그라운드 소리 조건에서 빠진다 | 버림. 휴식이 3분을 넘는다 |
| `systemExempted` | `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` | `SCHEDULE_EXACT_ALARM`이나 `USE_EXACT_ALARM` 보유 등. 조건이 안 맞으면 `ForegroundServiceTypeNotAllowedException` | 없음 | 시스템 앱과 특정 시스템 통합용 | 버림. 정확한 알람 권한을 받지 않는다 |
| `dataSync` | `FOREGROUND_SERVICE_DATA_SYNC` | 없음 | targetSdk 35 이상은 24시간에 6시간 | 데이터 전송용 | 버림. 용도가 다르다 |

표의 내용은 모두 [포그라운드 서비스 종류][fgs-types]와 [포그라운드 서비스 시간 제한][fgs-timeout]에 적힌 것이다(문서). 판정은 이 문서의 판단이다.

Play 스토어 출시는 v1 범위 밖이다. 나중에 Play에 올리면 Play Console의 앱 콘텐츠 페이지에 포그라운드 서비스 종류를 신고해야 하고 `specialUse`는 `<property>`에 적은 용도를 심사받는다(문서: [포그라운드 서비스 종류][fgs-types]).

### 시작 조건

| 조건 | 적용 버전 | 근거 |
|---|---|---|
| `startForegroundService()`로 시작한 뒤 5초 안에 `startForeground()`를 부른다 | API 26 이상 | 문서: [Android 8.0 백그라운드 실행 제한][a8-bg] |
| `FOREGROUND_SERVICE` 권한을 선언한다. 없으면 `SecurityException` | targetSdk 28 이상 | 문서: [포그라운드 서비스 변경 이력][fgs-changes] |
| 앱이 백그라운드에 있으면 포그라운드 서비스를 시작하지 못한다. 예외 목록이 있다. 어기면 `ForegroundServiceStartNotAllowedException` | targetSdk 31 이상 | 문서: [백그라운드 시작 제한][fgs-bg] |
| 종류를 매니페스트에 선언하고 종류별 권한을 요청한다 | targetSdk 34 이상 | 문서: [포그라운드 서비스 종류][fgs-types] |
| 알림을 함께 넘긴다. 우선순위는 `PRIORITY_LOW` 이상 | 전체 | 문서: [포그라운드 서비스 시작][fgs-launch] |
| 알림 권한 없이도 포그라운드 서비스는 시작된다 | API 33 이상 | 문서: [알림 권한][notif-perm] |

이 앱은 세트 완료를 앱 화면에서 누르는 순간에만 휴식을 시작한다(REST-01). 그때 앱이 화면에 보이므로 백그라운드 시작 제한에 걸리지 않고 서비스가 WIU 권한을 받는다(추론). 공식 문서도 백그라운드에서 소리를 낼 가능성이 있으면 앱이 포그라운드에 있을 때 서비스를 시작하라고 적는다(문서: [백그라운드 오디오 강화][bg-audio]).

`ServiceCompat.startForeground()`에 넘기는 종류 값은 API 34 이상에서만 `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`로 하고 그 아래에서는 0으로 한다. API 29~33에서 허용되는 종류 목록에 `specialUse`가 없다(문서: [ServiceCompat][ref-svccompat]).

## 끝나는 시각 맞추기

### 방법

- 휴식을 시작할 때 끝나는 시각을 `SystemClock.elapsedRealtime()` 기준으로 정한다. 서비스는 다음 신호 시각까지 남은 시간을 매번 다시 계산해 기다린다. 한 번 밀린 오차가 다음 신호로 쌓이지 않는다(추론).
- 공식 문서는 앱이 살아 있는 동안 시간을 잴 때는 알람보다 `Handler`를 쓰라고 적는다. `Handler`의 `postDelayed()`는 기기가 깨어 있던 시간(uptime)만 센다(문서: [알람 예약][alarms]). CPU가 잠들면 멈추므로 wake lock이 필요하다(추론).
- wake lock은 `PARTIAL_WAKE_LOCK`을 쓴다. 화면이 꺼져도 CPU가 돈다(문서: [PowerManager][ref-pm]). 휴식을 시작할 때 잡고 끝나거나 건너뛰면 놓는다. 놓지 못하는 경우에 대비해 제한 시간을 건다.

### 정확한 알람 권한

| 권한 | 받는 방법 | 조건 | 근거 |
|---|---|---|---|
| `SCHEDULE_EXACT_ALARM` (API 31) | 사용자가 설정의 "알람 및 리마인더"에서 켠다. 사용자와 시스템이 회수한다 | targetSdk 33 이상 앱을 Android 14 이상에 새로 설치하면 기본으로 꺼져 있다. 회수되면 앱이 멈추고 걸어 둔 정확한 알람이 모두 취소된다 | 문서: [알람 예약][alarms], [Android 14 변경][a14-all] |
| `USE_EXACT_ALARM` (API 33) | 설치할 때 자동으로 받는다. 사용자가 회수하지 못한다 | 정확한 알람이 핵심 기능인 앱만 쓴다. 문서가 드는 예는 알람 시계와 캘린더 앱이다. Play는 정책에 맞는 앱만 이 권한을 넣어 게시하게 한다 | 문서: [Manifest.permission][ref-perm], [정확한 알람 기본 거부][a14-exact] |

결론: 둘 다 받지 않는다. 타이머가 서비스 안에서 돌기 때문이다. `USE_EXACT_ALARM`은 이 앱의 성격에 맞지 않고 `SCHEDULE_EXACT_ALARM`은 사용자가 설정 화면까지 가서 켜야 한다. 앱이 꺼진 뒤에도 끝을 알리는 용도로도 받지 않기로 했다(2026-11-18 협의).

참고로 `OnAlarmListener`를 받는 `setExact()`는 `SCHEDULE_EXACT_ALARM` 없이 쓴다(문서: [알람 예약][alarms]). 휴식 내내 wake lock을 잡지 않고 끝나기 10초쯤 전에 CPU를 깨우는 데 쓸 만하다. v1에서는 쓰지 않는다. 공식 문서가 wake lock 로직을 단순하게 두라고 권하고(문서: [wake lock 권장 사항][wl-bp]) 휴식이 몇 분으로 짧기 때문이다.

## 앱 프로세스가 죽었을 때

### 죽는 경우

| 경우 | 일어나는 일 | 근거 |
|---|---|---|
| 메모리 부족 | 포그라운드 서비스를 돌리는 동안에는 드물다 | 문서: [프로세스 수명 주기][process] |
| 사용자가 알림 창의 작업 관리자에서 중지(Android 13 이상) | 앱 전체가 메모리에서 내려가고 서비스 알림이 지워진다. 앱은 콜백을 받지 못한다. 걸어 둔 알람은 그대로 울린다 | 문서: [사용자가 앱을 중지할 때][fgs-user-stop] |
| 사용자가 설정에서 강제 종료 | Android 15부터 앱의 PendingIntent가 모두 취소된다 | 문서: [Android 15 변경][a15-all] |
| 사용자가 배터리 사용을 "제한됨"으로 설정 | 포그라운드 서비스를 시작하지 못하고 돌던 서비스는 포그라운드에서 내려간다. 알람도 울리지 않는다. 사용자가 앱을 열어 쓰는 동안에는 잠시 풀린다 | 문서: [Android 13 변경][a13-all], [백그라운드 최적화][bg-opt] |
| 최근 앱 목록에서 앱을 밀어 없앰 | 서비스가 `onTaskRemoved()`를 받는다. 그 뒤 서비스가 이어지는지는 문서에 없다 | 문서: [Service][ref-service] |
| 제조사의 자체 절전 기능 | 공식 문서에 없다 | 확인하지 못함 |

### 타이머의 운명

- 타이머의 실체는 "끝나는 시각" 값 하나다. 이 값을 저장해 두면 프로세스가 죽어도 남는다. 운동 기록을 바로 저장하는 NF-09, 앱이 꺼져도 운동을 이어 가는 LOG-13과 같은 저장소를 쓴다(추론).
- 신호와 알림은 프로세스가 살아 있어야 나온다. 프로세스가 죽으면 그 휴식의 남은 신호는 나오지 않는다(추론).
- 서비스가 멈추면 서비스 알림도 지워진다(문서: [포그라운드 서비스 중지][fgs-stop]).

### 되살리는 방법

| 방법 | 판정 | 이유 |
|---|---|---|
| 사용자가 앱을 다시 열면 저장한 끝나는 시각으로 화면을 복원한다. 시각이 지났으면 휴식이 끝난 상태로 보여 준다 | 채택 | 권한이 필요 없다. LOG-13의 복원과 한 흐름이다 |
| 서비스를 `START_STICKY`로 두어 시스템이 다시 만들게 한다 | 버림 | 시스템이 서비스를 다시 만든다는 것까지는 문서에 있다(문서: [Service][ref-service]). 다시 만들어진 서비스가 백그라운드에서 `startForeground()`를 부를 수 있는지, WIU 권한을 받는지는 문서에 없다. WIU가 없으면 Android 17에서 소리가 조용히 실패한다. 서비스는 `START_NOT_STICKY`로 둔다 |
| 끝나는 시각에 알람을 걸어 프로세스를 깨운다 | 버림 | 정확한 알람은 '알람 및 리마인더' 권한이 필요하고 이 권한은 받지 않기로 했다(2026-11-18 협의). 권한 없는 알람은 targetSdk 31 이상에서 전달 창이 10분으로 늘어나기도 한다(문서: [알람 예약][alarms]) |
| 재부팅 뒤 복원 | 버림 | 기기를 끄면 알람이 모두 취소되고 다시 걸려면 `RECEIVE_BOOT_COMPLETED`가 필요하다(문서: [알람 예약][alarms]). 몇 분짜리 휴식에는 의미가 없다 |

휴식 중에 앱이 강제로 꺼지면 그 휴식은 신호 없이 지나간다. 이 경우는 받아들이기로 했다(2026-11-18 협의, REST-06).

## 소리와 진동

### 신호 판정

신호를 낼 때마다 방해 금지 상태를 먼저 읽고 그다음 벨소리 모드를 읽는다. 휴식 중에 사용자가 모드를 바꿔도 다음 신호부터 따른다. 방해 금지 상태는 `NotificationManager.getCurrentInterruptionFilter()`로, 벨소리 모드는 `AudioManager.getRingerMode()`로 읽는다(문서: [NotificationManager][ref-nm], [AudioManager][ref-am]).

| 순서 | 읽는 값 | 문서의 설명 | 이 앱의 신호(REST-05) |
|---|---|---|---|
| 1 | 방해 금지가 켜짐. 필터가 `INTERRUPTION_FILTER_PRIORITY`, `INTERRUPTION_FILTER_ALARMS`, `INTERRUPTION_FILTER_NONE` 가운데 하나 | 알림을 일부 또는 전부 막고 오디오 스트림을 일부 또는 전부 끄는 상태 | 없음. 벨소리 모드를 보지 않는다 |
| 2 | `RINGER_MODE_NORMAL` | 소리가 나고 진동도 울릴 수 있는 모드 | 소리 |
| 2 | `RINGER_MODE_VIBRATE` | 소리가 나지 않고 진동이 울리는 모드 | 진동 |
| 2 | `RINGER_MODE_SILENT` | 소리도 진동도 없는 모드 | 없음 |

필터가 `INTERRUPTION_FILTER_ALL`이면 방해 금지가 꺼진 상태다. 값을 읽지 못했을 때 돌아오는 `INTERRUPTION_FILTER_UNKNOWN`도 꺼진 것으로 보고 벨소리 모드를 따른다. 신호가 없는 동안에도 앱 안의 휴식 바와 알림, 상태바 칩은 그대로 보인다.

### 소리

- 짧은 효과음은 `SoundPool`로 낸다. 미리 디코딩해 두어 지연이 짧다(문서: [SoundPool][ref-soundpool]). 파일을 싣지 않으려면 `ToneGenerator`의 내장 톤을 쓴다(문서: [ToneGenerator][ref-tonegen]). 어느 쪽이든 #26에서 들어 보고 정한다.
- `AudioAttributes`의 용도는 `USAGE_NOTIFICATION`으로 한다. 용도는 음량과 출력 경로를 정하는 데 쓰인다(문서: [AudioAttributes][ref-aa]). 벨소리·알림 음량을 따르게 하려는 선택이다(추론).
- 효과음 파일을 넣는다면 직접 만든 것만 쓴다. 외부 파일은 라이선스를 확인한다(`AGENTS.md`).
- 소리를 재생하는 동안에는 오디오 시스템이 wake lock을 대신 잡는다(문서: [기기를 깨워 두는 방법][awake]). 재생 사이의 빈 시간에는 해당하지 않으므로 서비스의 wake lock이 따로 필요하다(추론).
- 오디오 포커스를 요청할지는 정하지 않았다. targetSdk 35 이상은 최상위 앱이거나 포그라운드 서비스를 돌리는 중이어야 포커스 요청이 받아들여진다(문서: [Android 15 변경][a15-target]). 사용자가 음악을 듣는 중에 신호음이 어떻게 들리는지 들어 본 뒤 #26에서 정한다.

### 진동

- `VIBRATE` 권한이 필요하다. 보호 수준은 normal이라 사용자에게 묻지 않는다(문서: [Manifest.permission][ref-perm]).
- 용도를 알림으로 지정한다. API 33 이상은 `vibrate(VibrationEffect, VibrationAttributes)`에 `USAGE_NOTIFICATION`을 넘긴다. `VibrationAttributes`는 API 30에, 이 오버로드는 API 33에 추가됐다(문서: [Vibrator][ref-vib], [VibrationAttributes][ref-va]). 그 아래 버전은 `AudioAttributes`를 받는 오버로드를 쓴다.
- 프로세스가 끝나면 시작한 진동도 멈춘다(문서: [Vibrator][ref-vib]).

### 방해 금지 모드

방해 금지가 켜져 있으면 소리와 진동을 내지 않는다. 무음 모드와 같게 다룬다(2026-11-18 협의, REST-05). 휴식 바와 알림 표시는 그대로다.

읽는 방법

- `NotificationManager.getCurrentInterruptionFilter()`가 지금의 필터를 돌려준다(API 23). 이 필터는 어떤 알림이 소리와 진동으로 사용자를 방해해도 되는지를 정하고 기기 전체에 적용된다(문서: [NotificationManager][ref-nm]).
- 돌려주는 값은 다섯 가지다(문서: [NotificationManager][ref-nm]).

| 값 | 문서의 설명 | 이 앱의 판정 |
|---|---|---|
| `INTERRUPTION_FILTER_ALL` | 평소 상태. 막는 알림이 없다 | 꺼짐 |
| `INTERRUPTION_FILTER_PRIORITY` | 우선순위 조건에 맞는 알림만 통과한다. 일부 오디오 스트림을 끈다 | 켜짐 |
| `INTERRUPTION_FILTER_ALARMS` | 알람 범주 알림만 통과한다. 일부 오디오 스트림을 끈다 | 켜짐 |
| `INTERRUPTION_FILTER_NONE` | 모든 알림을 막는다. 통화용을 뺀 모든 오디오 스트림과 진동을 끈다 | 켜짐 |
| `INTERRUPTION_FILTER_UNKNOWN` | 값을 읽지 못했을 때 돌아온다 | 꺼짐으로 본다 |

권한

- 레퍼런스의 `getCurrentInterruptionFilter()` 설명에는 권한 요구가 없다. 같은 클래스에서 방해 금지 접근 권한이 없으면 `SecurityException`을 던진다고 적힌 것은 `setInterruptionFilter()`, `setNotificationPolicy()`, 자동 규칙을 다루는 메서드처럼 상태를 바꾸거나 규칙을 읽고 쓰는 쪽이다(문서: [NotificationManager][ref-nm]). 읽기에는 권한이 필요 없다고 본다(추론, 에뮬레이터 확인 필요).
- `ACCESS_NOTIFICATION_POLICY`를 선언하지 않는다. 방해 금지 상태를 바꾸지 않기 때문이다.
- 필터가 바뀌면 `ACTION_INTERRUPTION_FILTER_CHANGED` 방송이 온다. 코드에서 등록한 리시버는 권한 없이 받는다(문서: [NotificationManager][ref-nm]). 이 앱은 신호마다 값을 읽으므로 방송을 받지 않는다.

그 밖에 확인한 것

- 방해 금지를 뚫는 진동 플래그 `FLAG_BYPASS_INTERRUPTION_POLICY`는 특권 앱만 쓴다. 일반 앱이 붙이면 무시된다(문서: [VibrationAttributes][ref-va]).
- targetSdk 35 이상인 앱은 방해 금지의 전역 상태를 바꾸지 못한다(문서: [Android 15 변경][a15-target]). 이 앱은 바꿀 일이 없다.
- 방해 금지 중에 어느 스트림이 꺼지는지는 사용자의 방해 금지 설정에 따라 달라진다. 앱이 신호를 아예 내지 않으므로 이 차이에 기대지 않는다.

## 알림 권한(POST_NOTIFICATIONS)

### 문서로 확인한 동작

Android 13(API 33) 이상에서 알림을 띄우려면 런타임 권한 `POST_NOTIFICATIONS`가 필요하다. 아래 표에서 근거를 따로 적지 않은 줄은 [알림 권한][notif-perm] 문서의 내용이다(문서).

| 상황 | 동작 |
|---|---|
| Android 13 이상에 새로 설치 | 알림이 꺼진 채 시작한다 |
| 묻는 시점 | targetSdk 33 이상인 앱은 허용 창을 띄울 시점을 스스로 정한다. 채널을 만든다고 창이 뜨지 않는다 |
| 문서가 권하는 시점 | 앱에 익숙해진 뒤, 그 알림이 왜 필요한지 드러나는 맥락에서, 사용자 조작에 이어서 묻는다. 예로 드는 것은 알림 종 버튼을 눌렀을 때다 |
| 창에서 "허용"을 누름 | 알림을 보낸다. 포그라운드 서비스 알림이 알림 창에 보인다 |
| 창에서 "허용 안함"을 누름 | 모든 알림 채널이 막힌다. 사용자가 시스템 설정에서 앱 알림을 전부 끈 것과 비슷한 상태다 |
| 선택 없이 창을 닫음 | 알림 권한의 상태가 바뀌지 않는다. 거절로 기록되지 않는다 |
| "허용 안함"을 두 번 누름 | 영구 거절로 본다. 앱이 다시 요청해도 시스템이 창을 띄우지 않는다(문서: [런타임 권한 요청][perm-req]) |
| 권한 없이 포그라운드 서비스를 시작 | 시작된다. 알림 권한은 포그라운드 서비스를 띄우는 데 필요하지 않다. 서비스를 시작할 때 알림을 함께 넘겨야 하는 것은 그대로다. 그 알림은 알림 창에 나오지 않고 작업 관리자에만 보인다 |
| 알림을 보낼 수 있는지 확인 | 보내기 전에 `areNotificationsEnabled()`를 부른다. 앱의 알림이 켜져 있는지를 돌려준다(문서: [NotificationManager][ref-nm]). 권한을 거절한 경우와 시스템 설정에서 끈 경우를 모두 이 값으로 가른다(추론) |
| Android 12L 이하 | `POST_NOTIFICATIONS`는 API 33에 추가됐다(문서: [Manifest.permission][ref-perm]). 그 아래 버전에는 앱이 띄울 허용 창이 없다. 알림은 처음부터 켜져 있고 사용자가 시스템 설정에서만 끈다(추론) |

사용자가 한 번 거절한 권한을 앱이 다시 요청할 때는 `shouldShowRequestPermissionRationale()`이 true를 돌려주고 문서는 그때 이유를 설명하는 화면을 먼저 보이라고 적는다(문서: [런타임 권한 요청][perm-req]). 문서는 거절을 받아들이고 다시 생각해 보라고 거듭 조르지 말라고도 적는다(문서: [런타임 권한 요청][perm-req]).

### 권한이 없을 때 동작하는 것과 안 하는 것

| 기능 | 권한이 없을 때 | 근거 |
|---|---|---|
| 앱 안의 휴식 바 | 동작 | 알림과 무관하다 |
| 타이머, 소리, 진동 | 동작 | 서비스가 직접 낸다. 포그라운드 서비스는 알림 권한 없이 시작된다(추론, 실기기 확인 필요) |
| 휴식 중 알림과 버튼 | 보이지 않음 | 문서 |
| 상태바 칩(Live Updates) | 보이지 않음 | `POST_PROMOTED_NOTIFICATIONS`는 `POST_NOTIFICATIONS`에 더해 필요한 권한이다(문서: [Manifest.permission][ref-perm]) |
| 휴식 종료 알림 | 보이지 않음 | 문서 |

### 묻는 방식

앱은 알림 권한을 먼저 묻지 않는다. 휴식 바의 '알림 켜기'를 눌렀을 때만 묻는다(2026-11-18 협의, REST-09). 문서가 권하는 "사용자 조작에 이어서 묻기"에 맞는다.

상태는 넷이다. 알림이 켜져 있는지는 `areNotificationsEnabled()`로, 거절한 적이 있는지는 앱의 설정 테이블 값 `notification_prompt_declined`(0 또는 1, 기본 0)로 가른다.

| 상태 | 조건 | 휴식 바 | 앱 설정 화면 | 알림과 상태바 칩 |
|---|---|---|---|---|
| 아직 묻지 않음 | Android 13 이상, 알림 꺼짐, `notification_prompt_declined`가 0 | 다음 세트 줄 오른쪽 끝에 '알림 켜기' 텍스트 버튼 | '알림 꺼짐' 표시와 켜는 버튼 | 없음 |
| 허용 | 알림 켜짐 | 버튼 없음 | 표시 없음 | 나옴 |
| 거절 | Android 13 이상, 알림 꺼짐, `notification_prompt_declined`가 1, 시스템이 아직 창을 띄워 줌 | 버튼 없음 | '알림 꺼짐' 표시와 켜는 버튼. 누르면 시스템의 허용 창이 다시 뜬다 | 없음 |
| 시스템이 더는 묻지 않음 | Android 13 이상에서 두 번 거절했거나, Android 12L 이하에서 사용자가 시스템 설정으로 알림을 끔 | 버튼 없음 | '알림 꺼짐' 표시와 켜는 버튼. 누르면 기기의 앱 알림 설정 화면으로 간다 | 없음 |

어느 상태에서나 앱 안의 휴식 바와 소리·진동 신호는 같다.

'알림 켜기'를 눌렀을 때

| 결과 | 처리 |
|---|---|
| "허용"을 누름 | 버튼이 사라진다. 그 휴식부터 알림과 상태바 표시가 나온다 |
| "허용 안함"을 누름 | 거절이다. `notification_prompt_declined`에 1을 적고 버튼을 숨긴다. 그 뒤로는 앱 설정 화면에서만 안내한다 |
| 선택 없이 창을 닫음 | 거절로 치지 않는다. 값을 바꾸지 않고 버튼을 그대로 둔다 |

창을 닫기만 한 경우를 거절로 치지 않는 이유는 둘이다. 시스템도 이 경우에 권한 상태를 바꾸지 않는다(문서: [알림 권한][notif-perm]). 사용자가 고르지 않았는데 앱이 거절로 적으면 버튼이 사라져 다시 켤 길이 설정 화면뿐이 된다.

구현할 때 정할 것과 확인할 것

- 앱은 요청 결과로 허용 여부만 받는다. "허용 안함"과 닫기를 가르려면 결과를 받은 뒤 `shouldShowRequestPermissionRationale()`을 본다. true면 "허용 안함"을 누른 것으로, false면 닫은 것으로 본다(추론). 문서는 한 번 거절한 권한에 `USER_SET`, 두 번 거절한 권한에 `USER_FIXED` 표시가 붙는다고만 적고 이 메서드의 반환값이 언제 바뀌는지는 적지 않는다(문서: [런타임 권한 요청][perm-req]). 에뮬레이터로 확인한다.
- 닫은 것으로 판정한 결과가 연달아 두 번 나오면 거절과 같게 처리한다. 시스템이 창을 띄우지 않고 곧바로 실패를 돌려주는 경우와 닫기를 앱이 가르지 못하기 때문이다. 버튼이 눌러도 반응 없는 채로 남는 일을 막는다(이 문서의 판단).
- 앱 설정 화면의 켜는 버튼은 먼저 허용 창을 요청하고 창이 뜨지 않는 상태면 `Settings.ACTION_APP_NOTIFICATION_SETTINGS`로 기기의 앱 알림 설정을 연다. 이 액션은 API 26에 추가됐고 `EXTRA_APP_PACKAGE`로 앱을 지정한다(문서: [Settings][ref-settings]).
- 앱이 화면으로 돌아올 때마다 `areNotificationsEnabled()`를 다시 읽는다. 사용자가 기기 설정에서 알림을 켜고 돌아오면 허용 상태가 된다.
- 허용한 뒤 사용자가 기기 설정에서 알림을 다시 끄면 `notification_prompt_declined`가 0인 채라 휴식 바에 버튼이 다시 보인다. v1은 그대로 둔다.

휴식 도중에 허용한 경우

- 서비스는 권한이 없을 때도 알림을 넘겨 시작해 둔 상태다. 허용 결과를 받으면 서비스가 같은 알림 ID로 알림을 한 번 더 보낸다. 그 휴식에 바로 알림과 상태바 표시가 붙는다(추론, 에뮬레이터 확인 필요).
- 허용만으로 서비스 알림이 저절로 나타나는지는 문서에 없다. 그래서 다시 보낸다.

Android 12L 이하

- 허용 창이 없으므로 휴식 바에 '알림 켜기'를 보이지 않는다. 알림은 처음부터 켜져 있다.
- 사용자가 시스템 설정에서 알림을 끈 경우는 사용자가 직접 고른 것이므로 "시스템이 더는 묻지 않음" 상태로 다룬다. 앱 설정 화면에서만 안내한다.

## Live Updates

### 승격 조건

알림이 아래를 모두 만족하면 시스템이 Live Update로 승격한다(문서: [Live Updates][live]).

- 표준 스타일, `BigTextStyle`, `CallStyle`, `ProgressStyle`, `MetricStyle` 가운데 하나다.
- 매니페스트에 `android.permission.POST_PROMOTED_NOTIFICATIONS`를 선언한다. 런타임 권한이 아니다.
- `setRequestPromotedOngoing(true)`로 승격을 요청한다.
- ongoing이다(`setOngoing(true)`).
- `contentTitle`이 있다.
- 커스텀 뷰(`RemoteViews`)가 없다.
- 그룹 요약이 아니다.
- `setColorized(true)`가 아니다.
- 채널 중요도가 `IMPORTANCE_MIN`이 아니다.

조건을 다 맞춰도 승격이 보장되지는 않는다.

- 사용자가 앱별 설정에서 승격을 끄면 승격되지 않는다. `NotificationManager.canPostPromotedNotifications()`로 확인하고 `Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`로 설정 화면을 연다(문서: [NotificationManager][ref-nm], [Settings][ref-settings]).
- `hasPromotableCharacteristics()`가 true여도 승격을 보장하지 않는다(문서: [Notification][ref-notif]).
- 제조사가 승격 조건을 더 붙일 수 있다(문서: [Live Updates][live]).
- 실제로 승격됐는지는 시스템이 붙이는 `FLAG_PROMOTED_ONGOING`으로 읽는다. 앱이 직접 세우지 못한다(문서: [Notification][ref-notif]).

### 쓰임새가 맞는가

Live Update는 진행 중이고, 사용자가 시작했고, 시간에 민감한 활동에 쓴다. 문서는 사용자가 시작한 활동의 예로 운동 시작을 든다(문서: [Live Updates][live]). `MetricStyle` 문서는 지표의 예로 피트니스 정보와 타이머를 든다(문서: [MetricStyle][ref-metric]). 휴식 타이머는 시작과 끝이 뚜렷하고 세트 완료라는 사용자 조작으로 시작하므로 조건에 맞는다(추론). 문서가 휴식 타이머를 직접 예로 들지는 않는다.

### API가 추가된 버전

| API | 추가된 버전 | 근거 |
|---|---|---|
| `Notification.FLAG_PROMOTED_ONGOING`, `hasPromotableCharacteristics()` | API 36 | 문서: [Notification][ref-notif] |
| `NotificationManager.canPostPromotedNotifications()` | API 36 | 문서: [NotificationManager][ref-nm] |
| `Notification.Builder.setShortCriticalText()` | API 36 | 문서: [Notification.Builder][ref-builder] |
| `Notification.ProgressStyle` | API 36 | 문서: [ProgressStyle][ref-progress] |
| `Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS` | API 36 | 문서: [Settings][ref-settings] |
| `Notification.Builder.setRequestPromotedOngoing()`, `EXTRA_REQUEST_PROMOTED_ONGOING` | 36.1 | 문서: [Notification.Builder][ref-builder], [Notification][ref-notif] |
| `Manifest.permission.POST_PROMOTED_NOTIFICATIONS`(보호 수준 normal, appops) | 36.1 | 문서: [Manifest.permission][ref-perm] |
| `Notification.MetricStyle` | API 37 | 문서: [MetricStyle][ref-metric] |
| `NotificationCompat.Builder.setRequestPromotedOngoing()`, `setShortCriticalText()` | androidx.core 1.17.0 | 문서: [NotificationCompat.Builder][ref-compat] |
| `NotificationCompat`의 `MetricStyle` 지원 | androidx.core 1.19.0 | 문서: [androidx.core 릴리스 노트][core-rel] |

36.1은 Android 16의 첫 마이너 릴리스다(`VERSION_CODES_FULL.BAKLAVA_1`, 문서: [VERSION_CODES_FULL][ref-vcf]). 승격을 요청하는 플랫폼 API와 권한이 36.1에 추가됐으므로 Android 16의 첫 릴리스(36.0) 기기에서 칩이 나오는지는 문서로 확인하지 못했다. 이 저장소는 이미 androidx.core 1.19.1을 쓴다(`gradle/libs.versions.toml`).

앱 코드는 버전을 나눠 알림을 두 벌 만들지 않는다. `NotificationCompat`으로 알림 하나를 만들고 승격을 요청한다. 승격 여부는 기기가 정하고 승격되지 않으면 같은 알림이 일반 알림으로 보인다(추론).

### 상태바 칩에 카운트다운 띄우기

칩 내용은 아래 순서로 정해진다(문서: [NotificationCompat.Builder][ref-compat], [Live Updates][live]).

1. `setShortCriticalText()`로 넣은 글자. 가장 우선한다.
2. `MetricStyle`의 핵심 지표(플랫폼이 지원할 때).
3. `when` 시각. `setUsesChronometer(true)`면 크로노미터가 양수인 동안 칩에 타이머가 보인다. `setChronometerCountDown(true)`를 함께 쓰면 `when`까지 거꾸로 센다.

- `setWhen(끝나는 시각)`, `setUsesChronometer(true)`, `setChronometerCountDown(true)`를 쓴다. 카운트다운은 시스템이 세므로 앱이 알림을 1초마다 갱신하지 않는다. `setChronometerCountDown()`은 API 24에 추가됐다(문서: [Notification.Builder][ref-builder]).
- `setShortCriticalText()`는 쓰지 않는다. 넣으면 타이머보다 우선한다.
- 칩의 최대 너비는 96dp다. 글자가 7자 미만이면 전부 보인다. "1:30" 형태는 4자다(문서: [Live Updates][live]).
- `when`이 과거가 되면 칩에 글자가 보이지 않는다(문서: [Live Updates][live]).

### 알림 스타일

| 스타일 | 추가된 버전 | 휴식 타이머에 쓰면 | 판정 |
|---|---|---|---|
| 표준 | 전체 | 제목, 본문, 버튼, 머리의 카운트다운 | 채택 |
| `BigTextStyle` | 전체 | 본문이 길 때 여러 줄 | 본문이 한 줄을 넘으면 쓴다 |
| `ProgressStyle` | API 36 | 진행 막대가 생긴다. 막대는 앱이 알림을 갱신해야 움직인다. 배차, 배달, 내비게이션에 권하는 스타일이다(문서: [ProgressStyle][ref-progress]) | 버림. 막대를 움직이려고 알림을 계속 갱신해야 한다 |
| `MetricStyle` | API 37 | 지표를 3개까지 보여 준다 | v1에서는 버림. Android 17에서만 된다. 나중에 다시 본다 |
| `CallStyle` | 전체 | 통화용 | 해당 없음 |

커스텀 `RemoteViews`는 쓸 수 없다. 승격된 알림 카드는 처음부터 펼쳐져 있고 접히지 않는다(문서: [Live Updates][live]).

### 버튼(액션)

결론: 붙인다. 근거는 아래와 같다.

- Live Updates 문서의 조건 목록에 액션을 금지하는 항목이 없다. 같은 문서가 백그라운드 이벤트를 따라가는 Live Update에는 고정 해제(Unpin) 액션을 넣으라고 적는다(문서: [Live Updates][live]).
- 진행 중심 알림 문서의 구성 그림에 "액션 버튼(`addAction()`)"이 들어 있고 사용자 여정에 도움이 되는 액션을 넣으라고 권한다(문서: [진행 중심 알림][pcn]).
- `ProgressStyle` 레퍼런스의 예제 코드가 `addAction()`을 쓴다(문서: [ProgressStyle][ref-progress]).
- 공식 샘플 앱이 승격을 요청한 알림에 `addAction()`으로 버튼을 붙인다(문서: [Live Updates 샘플][sample]).
- 펼친 알림은 액션을 3개까지 보여 준다(문서: [Notification.Builder][ref-builder], [알림 만들기][build-notif]).
- 알림 디자인 가이드는 Google 시계 앱의 타이머 알림이 일시정지와 1분 추가 버튼을 주는 것을 좋은 예로 든다(문서: [알림 디자인 가이드][design-notif]).

버튼 구성은 시간 줄이기, 시간 늘리기, 건너뛰기 3개다. 한 번에 15초씩 바뀐다. 휴식 시간의 기본값은 2분이고 종목마다 값 하나를 따로 둔다(2026-11-18 협의, REST-02, REST-03).

구현할 때 지킬 것

- 버튼의 `PendingIntent`에는 `FLAG_IMMUTABLE`을 붙인다. targetSdk 31 이상은 변경 가능 여부를 반드시 적어야 한다(문서: [Android 12 변경][a12-target]).
- 버튼을 받는 서비스나 브로드캐스트 리시버에서 액티비티를 열지 않는다. targetSdk 31 이상에서 막힌다(문서: [Android 12 변경][a12-target]). 이 버튼들은 화면을 열 필요가 없다.
- 버튼을 누를 때 서비스가 이미 돌고 있으므로 새로 시작할 일이 없다. 시간이 바뀌면 `when`을 바꿔 알림을 한 번 갱신한다.

승격된 카드에서 버튼 3개가 실제로 다 보이는지, 잠금 화면에서 잠금을 풀지 않고 눌리는지는 문서에 없다. 에뮬레이터로 확인한다.

### 휴식이 끝났을 때

Live Updates 문서에는 끝내는 절차가 따로 없다. 아래 문장을 근거로 절차를 정했다.

- Live Update는 진행 중인 활동만 나타낸다. 지나간 일에는 일반 알림을 쓴다(문서: [Live Updates][live]).
- 포그라운드 서비스를 멈추면 그 알림이 지워진다(문서: [포그라운드 서비스 중지][fgs-stop]).
- 더는 유효하지 않은 알림은 지운다(문서: [알림 디자인 가이드][design-notif]).

절차

1. 끝나는 순간의 신호를 낸다.
2. 서비스가 `stopForeground(STOP_FOREGROUND_REMOVE)`와 `stopSelf()`를 부른다. 휴식 중 알림이 지워진다. 칩은 그 알림의 축약 표시이므로 함께 사라진다고 본다(추론, 에뮬레이터 확인 필요). 시스템이 지우는 것에 기대지 않고 앱도 그 알림을 지운다(#69, 아래 "실기기에서 진행 알림이 남은 문제").
3. 다른 알림 ID로 "휴식 끝" 일반 알림을 띄운다. ongoing이 아니고 승격을 요청하지 않는다. 본문에 다음 세트를 적는다. `setTimeoutAfter()`로 일정 시간 뒤 저절로 지워지게 한다(API 26, 문서: [Notification.Builder][ref-builder]).
4. 사용자가 앱으로 돌아오거나 다음 세트를 완료하면 종료 알림을 지운다.
5. 건너뛰기를 누르면 신호와 종료 알림 없이 2번만 한다.

사용자가 휴식 중 알림을 직접 지운 경우

- Android 13부터 포그라운드 서비스 알림을, Android 14부터 ongoing 알림을 사용자가 밀어서 지울 수 있게 됐다. 잠금 화면에서는 지워지지 않는다(문서: [Android 13 변경][a13-all], [Android 14 변경][a14-all]).
- 알림을 지워도 서비스는 계속 돈다. 타이머와 신호는 이어진다(추론).
- 사용자가 지운 Live Update는 다시 띄우지 않는다. `setDeleteIntent()`로 지운 것을 알아낸다(문서: [Live Updates][live]). 그 휴식이 끝날 때까지 알림을 갱신하지 않는다.

## Android 8.0~15의 일반 알림

같은 알림이 승격만 되지 않은 모습이다. 따로 만들지 않는다.

| 요소 | 내용 | API |
|---|---|---|
| 채널 | "휴식 타이머" 하나. 중요도 `IMPORTANCE_LOW`(소리 없음). 신호는 서비스가 직접 내므로 채널은 조용해야 한다. 포그라운드 서비스 알림은 `PRIORITY_LOW` 이상이어야 한다 | 26 |
| 머리의 남은 시간 | `setWhen()`, `setUsesChronometer(true)`, `setChronometerCountDown(true)` | 24 |
| 제목과 본문 | 제목은 "휴식 중", 본문은 다음 세트(종목, 몇 번째 세트, 무게와 횟수). 디자인 가이드의 권장 길이는 제목 30자, 본문 40자 이내다 | 전체 |
| 버튼 | 시간 줄이기(15초), 시간 늘리기(15초), 건너뛰기 | 전체 |
| ongoing | `setOngoing(true)` | 전체 |
| 바로 보이기 | `setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)`. API 31부터 시스템이 서비스 알림 표시를 잠시 미루기도 한다 | 31 |
| 잠금 화면 | `VISIBILITY_PUBLIC`이면 내용이 잠금 화면에 다 보인다 | 전체 |
| 소리 한 번만 | `setOnlyAlertOnce(true)`. 갱신할 때 다시 울리지 않는다 | 전체 |

근거는 [알림 채널][channels], [알림 만들기][build-notif], [알림 디자인 가이드][design-notif], [Notification.Builder][ref-builder], [포그라운드 서비스 시작][fgs-launch]이다(문서). 알림에 남은 시간, 다음 세트, 버튼 3개를 보여 주는 것은 정해졌다(2026-11-18 협의, REST-07). 채널 구성과 문구는 이 문서의 제안이다.

휴식이 끝나면 위 "휴식이 끝났을 때"와 같은 절차로 종료 알림을 띄운다. REST-08이 이것으로 충족된다.

다음 세트의 종목과 무게는 잠금 화면에도 보인다.

## 배터리 최적화와 제조사 제한

| 항목 | 내용 | 이 앱의 대응 | 근거 |
|---|---|---|---|
| Doze | 전원이 뽑힌 채 화면이 꺼지고 한동안 움직이지 않으면 들어간다. wake lock을 무시하고 일반 알람을 미룬다. 기기를 움직이거나 화면을 켜면 풀린다 | 대응하지 않는다. 휴식은 몇 분이고 세트마다 기기를 만진다. Doze에 들어갈 만큼 가만히 두는 일은 드물다고 본다(추론) | 문서: [Doze][doze] |
| 배터리 최적화 예외 요청 | 예외를 받은 앱은 Doze 중에도 wake lock을 쓴다. Play 정책은 핵심 기능이 망가지는 경우에만 직접 요청을 허용한다. 문서의 허용 사례 표에 타이머는 없다 | 요청하지 않는다. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`를 선언하지 않는다 | 문서: [Doze][doze] |
| 사용자가 배터리 사용을 "제한됨"으로 설정 | 포그라운드 서비스 시작 불가, 돌던 서비스는 포그라운드에서 내려감 | 대응하지 않는다. 사용자가 고른 설정이다 | 문서: [Android 13 변경][a13-all], [백그라운드 최적화][bg-opt] |
| 앱 대기 버킷 | 버킷에 따라 job과 알람 횟수가 줄어든다 | 해당 없음. job과 알람을 쓰지 않는다 | 문서: [전원 관리 제한][power] |
| wake lock 과다 사용 | Android vitals는 부분 wake lock을 24시간에 2시간 이상 잡으면 과다로 본다. 백그라운드나 포그라운드 서비스 중에 잡은 시간만 센다 | 휴식 동안만 잡는다. 한 번 운동의 휴식을 다 더해도 2시간에 못 미친다고 본다(추론). Play에 올린 앱에만 적용되는 지표다 | 문서: [과도한 부분 wake lock][wl-vitals] |
| 제조사의 자체 절전 기능 | 공식 Android 문서에 내용이 없다. 제조사별 사례를 모아 둔 사이트가 있다 | v1은 따로 대응하지 않는다. 신호가 오지 않는다는 제보가 오면 기기별 안내를 검토한다 | 제3자: [Don't kill my app!][dkma] |
| 제조사의 Live Updates 조건 | 제조사가 승격 조건을 더 붙이기도 한다 | 승격되지 않으면 일반 알림으로 보인다 | 문서: [Live Updates][live] |

삼성 One UI 8이 Android 16의 Live Updates를 Now Bar에 연결한다는 보도와, 베타에서는 개발자 옵션을 켜야 모든 앱의 Live Update가 보였다는 보도가 있다(제3자: [SamMobile][sammobile], [Android Authority][aa-oneui8]). 정식 버전의 동작은 확인하지 못했다.

## 버전별 차이

minSdk 26부터 targetSdk 37까지, 이 기능에 닿는 변화만 적었다.

| Android(API) | 달라지는 점 | 이 앱에서 할 일 | 근거 |
|---|---|---|---|
| 8.0~8.1(26~27) | 알림 채널이 필수다. 백그라운드 서비스가 제한되고 `startForegroundService()` 뒤 5초 안에 `startForeground()`를 불러야 한다. `setTimeoutAfter()`가 추가됐다 | 채널을 만든다. 5초 규칙을 지킨다 | 문서: [Android 8.0 백그라운드 실행 제한][a8-bg], [알림 채널][channels] |
| 9(28) | `FOREGROUND_SERVICE` 권한이 필요하다 | 매니페스트에 선언한다 | 문서: [포그라운드 서비스 변경 이력][fgs-changes] |
| 10~11(29~30) | `startForeground()`가 서비스 종류를 받는다(29). `VibrationAttributes`가 추가됐다(30) | 종류 값은 0을 넘긴다 | 문서: [ServiceCompat][ref-svccompat], [VibrationAttributes][ref-va] |
| 12~12L(31~32) | 백그라운드에서 포그라운드 서비스를 시작하지 못한다. 정확한 알람에 `SCHEDULE_EXACT_ALARM`이 필요하다. PendingIntent의 변경 가능 여부를 적어야 한다. 알림 버튼에서 액티비티를 우회 실행하지 못한다. 서비스 알림 표시가 잠시 미뤄지기도 한다 | 앱이 보일 때만 서비스를 시작한다. `FLAG_IMMUTABLE`, `FOREGROUND_SERVICE_IMMEDIATE`를 쓴다 | 문서: [Android 12 변경][a12-target], [백그라운드 시작 제한][fgs-bg], [Notification.Builder][ref-builder] |
| 13(33) | `POST_NOTIFICATIONS` 런타임 권한이 생겼다. 사용자가 앱을 중지하는 작업 관리자가 생겼다. 포그라운드 서비스 알림을 사용자가 지울 수 있게 됐다. `USE_EXACT_ALARM`이 추가됐다. `vibrate(VibrationEffect, VibrationAttributes)`가 추가됐다 | 휴식 바의 '알림 켜기'를 눌렀을 때만 권한을 요청한다. 권한이 없어도 타이머와 신호가 돌게 한다 | 문서: [알림 권한][notif-perm], [Android 13 변경][a13-all], [Vibrator][ref-vib] |
| 14(34) | 포그라운드 서비스 종류 선언과 종류별 권한이 필수다. `SCHEDULE_EXACT_ALARM`이 새 설치에서 기본으로 꺼진다. ongoing 알림도 사용자가 지울 수 있게 됐다. 캐시 상태 앱의 백그라운드 작업이 막힌다 | `specialUse`와 그 권한을 선언한다. 지운 알림을 다시 띄우지 않는다 | 문서: [포그라운드 서비스 종류][fgs-types], [Android 14 변경][a14-all] |
| 15(35) | `dataSync` 등에 6시간 제한이 생겼다. `BOOT_COMPLETED`에서 일부 종류를 시작하지 못한다. 오디오 포커스를 요청하려면 최상위 앱이거나 포그라운드 서비스를 실행 중이어야 한다. 방해 금지의 전역 상태를 바꾸지 못한다. 강제 종료하면 PendingIntent가 취소된다 | 해당 종류를 쓰지 않는다. 오디오 포커스를 쓴다면 서비스 안에서 요청한다 | 문서: [Android 15 변경(대상 앱)][a15-target], [Android 15 변경][a15-all] |
| 16(36, 36.1) | 승격 알림 API와 `ProgressStyle`이 추가됐다(36). 승격 요청 API와 `POST_PROMOTED_NOTIFICATIONS`가 추가됐다(36.1). 포그라운드 서비스에서 시작한 job에도 할당량이 적용된다 | `NotificationCompat`으로 승격을 요청하고 권한을 선언한다. job은 쓰지 않는다 | 문서: [Live Updates][live], [Manifest.permission][ref-perm], [포그라운드 서비스 변경 이력][fgs-changes] |
| 17(37) | 백그라운드 소리에 `shortService`가 아닌 포그라운드 서비스가 필요하다. targetSdk 37이면 그 서비스에 WIU 권한이 있어야 한다. `MetricStyle`이 추가됐다 | 세트 완료 순간(앱이 보일 때)에 서비스를 시작한다 | 문서: [백그라운드 오디오 강화][bg-audio], [Android 17 변경][a17-all], [MetricStyle][ref-metric] |

## 구현 방식

### 한 줄 요약

세트를 완료하면 앱이 `specialUse` 포그라운드 서비스를 시작한다. 서비스는 부분 wake lock을 잡고 끝나는 시각까지 시간을 재며 벨소리 모드에 맞는 소리나 진동을 직접 낸다. 방해 금지가 켜져 있으면 내지 않는다. 남은 시간은 시스템이 세는 카운트다운 알림 하나로 보여 주고 같은 알림에 승격을 요청한다.

### 고른 이유

- 버전 26~37에서 한 가지 코드 경로로 동작한다. 버전에 따라 갈리는 것은 서비스 종류 값, 진동 API 오버로드, 권한 요청 여부 정도다.
- Android 17의 백그라운드 소리 조건을 만족한다. 앱이 보일 때 시작한 포그라운드 서비스이기 때문이다.
- 사용자에게 묻는 권한이 알림 하나뿐이고 그것도 사용자가 '알림 켜기'를 눌렀을 때만 묻는다. 정확한 알람, 배터리 최적화 예외, 센서 권한을 받지 않는다.
- 소리와 진동을 앱이 직접 고르므로 REST-05의 벨소리 모드 세 가지와 방해 금지 모드를 그대로 구현한다.
- 알림을 1초마다 갱신하지 않는다. 카운트다운은 시스템이 그린다.
- 신호를 내는 곳이 서비스 한 곳이다. 앱이 화면에 있을 때와 없을 때 같은 코드가 신호를 낸다.

### 동작 순서

1. 사용자가 앱에서 세트를 완료한다. 앱이 끝나는 시각을 저장하고 `startForegroundService()`를 부른다.
2. 서비스가 5초 안에 `startForeground()`를 부른다. 알림은 ongoing, 카운트다운, 버튼 3개, 승격 요청을 담는다. 알림 권한이 없어도 같은 순서로 시작한다. 알림만 보이지 않는다.
3. 서비스가 부분 wake lock을 잡는다.
4. 서비스가 끝나기 10초 전, 5초 전부터 1초마다, 끝나는 순간에 신호를 낸다. 신호마다 방해 금지 상태를 읽고 꺼져 있으면 벨소리 모드를 읽는다.
5. 버튼이나 앱 안의 휴식 바에서 시간을 바꾸면 끝나는 시각을 다시 저장하고 알림의 `when`을 갱신한다.
6. 끝나거나 건너뛰면 wake lock을 놓고 서비스를 멈춘다. 끝난 경우에만 종료 알림을 띄운다.
7. 앱이 화면에 보이는 동안에도 같은 서비스가 돈다. 앱 안의 휴식 바는 저장된 끝나는 시각을 보고 그린다.

### 권한과 매니페스트 선언

| 선언 | 추가된 버전 | 보호 수준 | 쓰는 곳 |
|---|---|---|---|
| `android.permission.FOREGROUND_SERVICE` | API 28 | normal | 포그라운드 서비스 |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | API 34 | normal, appop, instant | 서비스 종류 `specialUse` |
| `android.permission.POST_NOTIFICATIONS` | API 33 | dangerous(런타임) | 알림 |
| `android.permission.POST_PROMOTED_NOTIFICATIONS` | 36.1 | normal, appops | Live Updates |
| `android.permission.WAKE_LOCK` | API 1 | normal | 부분 wake lock |
| `android.permission.VIBRATE` | API 1 | normal | 진동 |

보호 수준과 추가된 버전은 [Manifest.permission][ref-perm]에서 확인했다(문서). 사용자에게 묻는 것은 `POST_NOTIFICATIONS` 하나다. 휴식 바의 '알림 켜기'를 눌렀을 때만 묻는다.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />

<application>
    <service
        android:name=".rest.RestTimerService"
        android:exported="false"
        android:foregroundServiceType="specialUse">
        <property
            android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
            android:value="Workout rest timer that plays countdown cues while the app is in the background" />
    </service>
    <receiver
        android:name=".rest.RestTimerActionReceiver"
        android:exported="false" />
</application>
```

클래스 이름은 예시다. 서비스 선언 형식은 [포그라운드 서비스 선언][fgs-declare]과 [포그라운드 서비스 종류][fgs-types]를 따랐다(문서).

선언하지 않는 것

| 권한 | 이유 |
|---|---|
| `INTERNET` | DATA-02 |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | 타이머가 서비스 안에서 돈다. '알람 및 리마인더' 권한은 받지 않기로 했다 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 뒤 휴식을 되살리지 않는다 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 배터리 최적화 예외를 요청하지 않는다 |
| `FOREGROUND_SERVICE_HEALTH`, `ACTIVITY_RECOGNITION`, `HIGH_SAMPLING_RATE_SENSORS` | `health` 종류를 쓰지 않는다 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | `mediaPlayback` 종류를 쓰지 않는다 |
| `ACCESS_NOTIFICATION_POLICY` | 방해 금지 상태를 읽기만 하고 바꾸지 않는다 |

### 구현 이슈(#26)가 따를 순서

1. 타이머 상태. 끝나는 시각 저장, 15초씩 늘리기와 줄이기, 건너뛰기, 신호 시각 계산을 안드로이드와 무관한 코드로 만들고 단위 테스트를 붙인다. 휴식이 10초보다 짧을 때와 시간을 줄여 신호 시각이 이미 지났을 때의 규칙을 여기서 정한다.
2. 앱 안의 휴식 바(REST-02)를 이 상태에 연결한다.
3. 매니페스트 선언, 알림 채널, 서비스 뼈대. 5초 규칙, 종류 값(API 34 이상만 `specialUse`), 카운트다운 알림, `FOREGROUND_SERVICE_IMMEDIATE`.
4. wake lock과 신호. 방해 금지 분기(켜져 있으면 신호 없음), 벨소리 모드 분기, 소리와 진동 재생.
5. 알림 버튼 3개와 종료 처리(서비스 중지, 종료 알림, 자동 삭제).
6. 알림 권한. 휴식 바의 '알림 켜기', 네 가지 상태, `notification_prompt_declined` 기록, 창을 닫기만 한 경우의 처리, 앱 설정 화면의 '알림 꺼짐' 표시와 켜는 버튼, 휴식 도중 허용했을 때 알림 다시 보내기.
7. Live Updates. 승격 요청, `canPostPromotedNotifications()` 확인, 사용자가 지운 알림을 다시 띄우지 않기.
8. 프로세스가 죽은 뒤 앱을 다시 열었을 때의 복원.
9. 에뮬레이터 확인. API 26, 32, 33, 34, 36, 37에서 "확인하지 못한 것"의 항목을 본다. Android 17에서는 `adb shell cmd audio set-enable-hardening enable`로 제한을 켜고 `adb shell dumpsys audio`에 `AudioHardening` 항목이 찍히는지 본다(문서: [백그라운드 오디오 강화][bg-audio]). Doze는 `adb shell dumpsys deviceidle force-idle`로 만든다(문서: [Doze][doze]). 확인한 것은 Android 버전과 함께 이 문서에 옮겨 적는다.

## 요구사항에 대한 결론

### 확인이 남은 항목 7번

앱이 화면에 없을 때 휴식 신호를 1초마다 내는 구현 방식은 이렇게 정한다.

- 종류가 `specialUse`인 포그라운드 서비스를 휴식 동안만 돌린다. 세트를 완료하는 순간 앱 화면에서 시작한다.
- 서비스가 부분 wake lock을 잡고 소리와 진동을 직접 낸다.
- 필요한 권한은 `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK`, `VIBRATE`다. 알림용으로 `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS`를 더한다.
- 정확한 알람 권한('알람 및 리마인더')은 받지 않는다.

### 확인이 남은 항목 8번

Live Updates로 승격된 알림에 시간 조절과 건너뛰기 버튼을 붙인다. 공식 문서, 레퍼런스 예제, 공식 샘플이 모두 승격 알림에 액션을 붙이고 금지 조항도 없다. 버튼은 3개까지다. 승격된 카드에서의 실제 모습은 에뮬레이터로 확인한다.

### 요구사항대로 되지 않는 경우

2026-11-18 협의에서 아래 경우를 받아들이고 요구사항 문구를 고치기로 했다.

| 요구사항 | 되지 않는 경우 | 그때의 동작 |
|---|---|---|
| REST-07 | Android 16 이상이어도 기기가 승격하지 않는 경우. 사용자가 승격을 끈 경우, 제조사가 조건을 더 붙인 경우, 36.0 기기 | 같은 알림이 일반 알림으로 보인다. 남은 시간, 다음 세트, 버튼은 그대로다 |
| REST-06 | 휴식 중에 앱이 강제로 꺼진 경우. 작업 관리자 중지, 강제 종료, 배터리 "제한됨" 설정, 제조사 절전 기능 | 그 휴식의 남은 신호가 나오지 않는다. 앱을 다시 열면 저장된 시각으로 복원한다 |
| REST-06 | 기기가 Doze에 들어간 경우 | wake lock이 무시되어 신호가 늦거나 빠질 수 있다(추론). 몇 분짜리 휴식에서는 드물다고 본다 |
| REST-07, REST-08 | 알림이 꺼져 있는 경우. 아직 '알림 켜기'를 누르지 않았거나 거절한 경우 | 알림과 칩이 보이지 않는다. 타이머와 소리·진동은 동작한다(추론) |

### 요구사항 문서에 반영한 것

| ID | 바뀐 내용 |
|---|---|
| REST-02 | 시간을 늘리고 줄이는 폭이 15초다 |
| REST-03 | 기본값은 2분이고 종목마다 값 하나를 둔다 |
| REST-05 | 방해 금지 모드가 켜져 있으면 신호를 주지 않는다 |
| REST-06 | 휴식 중에 앱이 강제로 꺼지면 그 휴식의 남은 신호는 나오지 않는다 |
| REST-07 | 휴식 중 알림의 내용(남은 시간, 다음 세트, 버튼 3개)과 "기기가 알림을 승격하면 상태바 칩이 나온다"는 조건 |
| REST-08 | 승격되지 않는 기기에서는 같은 내용이 일반 알림으로 보이고 휴식이 끝나면 어느 기기에서나 일반 알림으로 알린다 |
| REST-09(새로 만듦) | 알림 권한은 휴식 바의 '알림 켜기'를 눌렀을 때만 묻는다. 거절하면 앱 설정 화면에서만 안내한다 |
| 확인이 남은 항목 7, 8 | 정한 내용으로 바꿨다 |

## 구현(#26)에서 정한 것

이 문서의 추천(포그라운드 서비스 `specialUse`, 부분 wake lock, 앱이 직접 내는 소리와 진동, 카운트다운 알림 하나에 승격 요청)을 그대로 따랐다. 문서가 #26에 미룬 것과 문서와 다르게 한 것을 적는다.

| 항목 | 정한 것 | 이유 |
|---|---|---|
| 끝나는 시각을 두는 곳 | 데이터베이스가 아니라 기기의 `SharedPreferences`(`rest/RestTimerStore`). 단조 시계와 벽시계 값을 함께 적고, 둘이 5초 넘게 어긋나면(재부팅) 벽시계를 믿는다 | "타이머의 운명"은 운동 기록과 같은 저장소를 추론으로 적었다. 휴식 상태는 기록이 아니고 백업에도 넣지 않으며(docs/architecture.md), 스키마를 바꾸지 않아도 된다. 운동 기록이 바로 저장되는 것은 그대로다 |
| 신호 시점 | 10, 5, 4, 3, 2, 1, 0초 전. 남은 시간이 정확히 그 시점이면 낸다. 시간을 바꾸거나 다시 시작하면 그때 남은 시간으로 다시 계산하고 지난 신호는 내지 않는다 | 이슈의 제안. 끝나는 시각 기준으로 매번 다시 계산해 오차가 쌓이지 않는다 |
| "−15초"로 0 이하가 될 때 | 건너뛰기와 같다. 끝 신호와 끝 알림이 없다 | 끝 신호 시점을 지나친 것이다(이슈의 제안) |
| 소리 | `ToneGenerator`의 내장 톤을 알림 스트림(`STREAM_NOTIFICATION`)에 낸다. 10초 전 200ms, 카운트다운 100ms, 끝은 두 번 울리는 톤 600ms | 효과음 파일을 싣지 않는다. 알림 음량을 따른다. 오디오 포커스는 요청하지 않는다 |
| 진동 | 10초 전 200ms, 카운트다운 80ms, 끝은 400ms 두 번. 용도는 알림 | 끝을 앞의 신호와 구분한다 |
| 끝 알림 | 채널 "휴식 끝"(중요도 높음, 채널 소리와 진동 없음). 5분 뒤 저절로 지워진다. 앱이 화면에 있을 때는 띄우지 않는다(바가 "0:00"을 보인다) | 다른 앱을 쓰는 중에도 화면 위에 잠깐 떠야 눈에 띈다. 소리와 진동은 앱의 끝 신호가 이미 냈다 |
| 알림 본문 | 휴식 바와 같은 "다음 벤치 프레스 60kg × 10". 몇 번째 세트인지는 적지 않는다 | 바와 알림의 글을 하나로 둔다. 이 문서의 "Android 8.0~15의 일반 알림" 표와 다르다 |
| 설정의 '알림 켜기' | Android 12L 이하이거나 권한은 있는데 알림이 꺼져 있으면 기기의 앱 알림 설정을 연다. 아니면 허용 창을 요청하고, 결과가 0.5초 안에 '닫음'으로 돌아오면 창이 뜨지 않은 것으로 보고 앱 알림 설정을 연다 | 시스템이 창을 띄웠는지 앱이 알 길이 없다 |
| 프로세스가 죽은 뒤 | 앱을 다시 열면(액티비티 `onStart`) 저장한 시각으로 바를 되살리고 서비스를 다시 띄운다. 이미 지났으면 되살리지 않는다 | "되살리는 방법"의 채택안 |

에뮬레이터에서 확인한 것(2026-11-20)

API 36.0(Android 16 첫 릴리스) 에뮬레이터

- 앱을 홈으로 내리고 화면을 끈 채(`input keyevent 26`) 신호가 10초 전, 5~1초 전, 끝에 1초 간격으로 났다(앱의 로그 시각으로 확인). 진동 모드에서는 진동이 용도 알림으로 실제 울렸다(`dumpsys vibrator_manager`). 소리는 에뮬레이터를 소리 없이 띄워 귀로 듣지 못했다.
- 알림 권한이 없을 때 서비스가 시작되고 신호가 났다. 알림은 보이지 않았다.
- 휴식 바의 '알림 켜기' → 시스템 허용 창 → "허용 안 함" 뒤 버튼이 사라지고 설정에 '알림 꺼짐' 행이 보였다. 설정의 '알림 켜기' → 허용 창 → "허용" 뒤 행이 사라졌다.
- 휴식 중 알림에 "휴식 중", 다음 세트, 카운트다운, 버튼 셋이 보였고 알림의 "−15초"가 남은 시간을 줄였다. 끝나면 휴식 중 알림이 지워지고 "휴식 끝" 알림이 떴다.
- 이 에뮬레이터(36.0)에서는 승격되지 않아 상태바 칩이 나오지 않았다. 승격 요청 API가 36.1에 추가됐기 때문으로 본다. 36.1 이상 이미지가 없어 칩은 확인하지 못했다.
- 최근 앱 목록에서 앱을 지워도(모두 지우기) 프로세스와 서비스가 남아 신호와 끝 알림이 났다.
- 휴식 중에 프로세스를 죽이면(`kill -9`) 알림이 사라졌다. 다시 열자 바가 저장한 끝나는 시각대로 남은 시간을 보였고 서비스와 알림이 다시 떴으며 끝 신호가 났다.

API 26 에뮬레이터

- '알림 켜기' 버튼이 없다. 휴식 중 알림(서비스 종류 0으로 시작)과 끝 알림이 일반 알림으로 왔다. 화면을 끈 채 신호가 제때 났다.

### 실기기에서 진행 알림이 남은 문제(#69, 2026-11-21)

삼성 기기(Android 17)에서 두 가지를 겪었다.

1. 화면을 켠 채 앱만 내린 경우 신호와 끝 알림은 제때 왔지만, 서비스가 멈춘 뒤에도 휴식 중 알림(id=1, `ONGOING|NO_CLEAR|FOREGROUND_SERVICE|PROMOTED_ONGOING`)이 남았다. 사용자가 지울 수 없었다.
2. 화면을 끄고 잠근 경우 신호 로그가 한 줄도 없었고 끝 알림도 없었다. 끝나는 시각 뒤 서비스는 없었고 휴식 중 알림은 남았다.

찾은 것

- 서비스는 `stopSelf()`를 부른 뒤에도 `onDestroy()`가 올 때까지 상태를 계속 보며 휴식 중 알림을 `notify()`로 고칠 수 있었다. 그 사이에 상태가 바뀌면(새 휴식, 알림 다시 보내기) 멈춘 서비스가 알림을 다시 올렸다. Robolectric 테스트로 재현했다(`RestTimerServiceTest`).
- 남은 알림을 지우는 곳이 서비스의 `stopForeground()` 하나뿐이었다. 시스템이 지우지 못하면(프로세스가 죽거나, 시스템이 서비스를 포그라운드에서 내린 경우 등) 끝까지 남았다.
- 실기기에서 알림이 남은 정확한 경로는 확인하지 못했다. 에뮬레이터(API 36)에서는 고치기 전 코드로도 알림이 남지 않았다. `FOREGROUND_SERVICE` 표시가 붙은 채 남았으므로 시스템 쪽의 순서 문제(포그라운드 알림의 게시와 취소가 각각 비동기로 처리됨)일 수 있다(추론).

고친 것

- 서비스가 멈추기로 하면 상태를 보던 작업을 먼저 끊고, wake lock을 놓고, 포그라운드를 내린 뒤 앱이 알림을 직접 지운다. 멈춘 뒤에는 알림을 고치지 않는다.
- 서비스를 시작하자마자 같은 알림을 `notify()`로 한 번 더 보내던 것을 없앴다. `startForeground()`가 올린 알림과 같다.
- `stopSelfResult(startId)`로 멈춘다. 멈추는 사이에 새 휴식의 시작 요청이 와 있으면 서비스가 이어서 돈다.
- 휴식이 없고 서비스도 없는데 휴식 중 알림이 남아 있으면 지운다. 서비스가 끝나고 1초 뒤, 휴식이 없어질 때(서비스가 이미 없으면), 프로세스가 새로 떴을 때 확인한다. `FOREGROUND_SERVICE` 표시가 남은 알림은 같은 ID의 일반 알림으로 바꾼 뒤 지운다. 앱이 그 표시가 붙은 알림을 지우지 못할 수 있어서다(추론). 남아 있던 알림의 표시는 로그로 남긴다.
- 기기에서 원인을 볼 수 있게 로그(`adb logcat -s RestTimer`)를 늘렸다. 서비스 시작과 끝, 멈춤 요청, wake lock을 잡은 때, 지난 프로세스가 끝난 이유(API 30 이상, `ApplicationExitInfo`의 시각·이유 코드·설명).

화면이 꺼진 동안 신호가 오지 않은 문제(확인 중)

에뮬레이터(API 36, 2026-11-21)에서 확인한 것

- 화면을 끄고 배터리를 뽑은 상태로 만들고 Doze를 강제로 깊은 단계까지 넣어도(`dumpsys deviceidle force-idle`) 30초 휴식의 신호가 1초 간격으로 제때 났고 끝 알림이 왔다. 포그라운드 서비스가 잡은 wake lock은 Doze에서도 유지된다(AOSP 동작으로 본다. 에뮬레이터는 CPU가 실제로 잠들지 않으므로 기기와 다를 수 있다).
- 배터리 사용을 "제한됨"과 같게 만들면(`cmd appops set <패키지> RUN_ANY_IN_BACKGROUND ignore`) 앱이 화면에서 내려가자마자 시스템이 서비스를 포그라운드에서 내렸다(`dumpsys activity services`의 `isForeground=false`). 알림의 `FOREGROUND_SERVICE` 표시도 빠졌다. 이 상태에서 첫 신호음에 `AudioHardening background playback would be muted ... level: partial` 로그가 찍혔다. 신호 로그는 에뮬레이터에서 제때 났다.
- 포그라운드에서 내려간 서비스는 Doze에서 wake lock을 잃고, 프로세스가 캐시 상태로 내려가면 얼거나 종료될 수 있다(추론). 실기기에서 신호 로그가 하나도 없고 서비스가 사라진 것과 맞는다. 제조사 절전 기능(삼성의 "절전 앱"·"사용하지 않는 앱 절전" 등)이 같은 결과를 내는지는 확인하지 못했다.

실기기에서 볼 것

- 휴식 중 화면을 끈 상태에서 `dumpsys activity services <패키지>`의 `isForeground`, `dumpsys power`의 `NextSet:RestTimer` wake lock, `am get-standby-bucket <패키지>`, `cmd appops get <패키지> RUN_ANY_IN_BACKGROUND`.
- 다음에 앱을 열 때 `RestTimer` 로그의 "지난 프로세스 종료" 줄(휴식 중 프로세스가 죽었는지와 그 이유).

정확한 알람으로 끝 시각을 따로 거는 방안(정하지 않음)

- `setExactAndAllowWhileIdle()`과 `setAlarmClock()`은 API 31부터 `SCHEDULE_EXACT_ALARM`(사용자가 '알람 및 리마인더'에서 켬)이나 API 33부터 `USE_EXACT_ALARM`(설치할 때 받음, Play 정책 대상)이 있어야 한다(문서: [알람 예약][alarms]). 2026-11-18 협의에서 '알람 및 리마인더' 권한은 받지 않기로 했다. `USE_EXACT_ALARM`을 쓸지는 소유자가 정한다. 이 PR은 새 권한을 더하지 않는다.
- 권한 없이 쓰는 `OnAlarmListener`의 `setExact()`는 Doze에서 미뤄지고 프로세스가 죽으면 함께 사라진다. 프로세스가 얼거나 죽는 경우를 막지 못한다.
- 정확한 알람이 프로세스를 깨워도 Android 17에서는 포그라운드 서비스 없이 백그라운드 소리가 막힌다(위 "방식 비교" B). 알람만으로는 끝 알림은 띄워도 신호음은 보장하지 못한다(추론).

## 확인하지 못한 것

#26에서 기기나 에뮬레이터로 확인할 것

소리와 신호

- Android 17에서 앱이 보일 때 시작한 `specialUse` 서비스가 화면이 꺼진 뒤에도 소리를 내는지. 문서의 조건 문장은 `shortService`만 빼지만, 같은 문서의 권장 사항은 미디어 앱에 `mediaPlayback` 서비스를 쓰라고 적는다. 소리가 나지 않으면 `mediaPlayback`을 함께 선언하는 방안을 다시 본다.
- `SoundPool`과 `ToneGenerator`가 백그라운드 오디오 제한의 대상인지. 문서의 영향받는 API 목록에는 `AudioTrack.write()`, AAudio, OpenSL ES와 "재생을 관리하는 클라이언트 라이브러리"만 적혀 있다.
- 진동 모드와 무음 모드에서 `USAGE_NOTIFICATION` 소리를 시스템이 스스로 끄는지. 이어폰을 끼고 음악을 듣는 중에 신호음이 어디로 어떻게 나오는지.
- 화면이 꺼진 상태에서 1초 신호의 실제 오차. Doze와 절전 모드에서 포그라운드 서비스의 wake lock이 실제로 무시되는지.
- 진동의 길이와 세기, 신호음의 높이와 길이. 10초 전 신호와 끝나는 순간의 신호를 어떻게 구별할지.

방해 금지 모드

- `getCurrentInterruptionFilter()`가 권한 없이 읽히는지. API 26부터 37까지 본다.
- 방해 금지의 종류(우선순위만, 알람만, 모두 차단)마다 돌아오는 값이 이 문서의 표와 같은지. 제조사의 방해 금지 변형(취침 모드 등)에서 무엇이 돌아오는지.
- 휴식 중에 방해 금지를 켜고 껐을 때 다음 신호부터 바로 따르는지.

알림 권한

- 알림 권한이 없는 상태에서 서비스가 시작되고 소리와 진동이 실제로 나오는지.
- 허용 창에서 "허용 안함"을 눌렀을 때와 닫기만 했을 때 `shouldShowRequestPermissionRationale()`이 각각 무엇을 돌려주는지. 두 번 거절한 뒤 요청하면 창 없이 결과가 바로 돌아오는지.
- 허용 창을 닫는 방법(뒤로 가기, 창 바깥 누르기)이 Android 13부터 17까지 어떻게 다른지.
- 휴식 도중에 허용했을 때 서비스가 알림을 다시 보내면 바로 나타나는지. 다시 보내지 않아도 나타나는지.
- 허용한 뒤 기기 설정에서 알림을 끈 앱이 다시 요청하면 허용 창이 뜨는지.
- Android 12L 이하에서 시스템 설정으로 알림을 껐을 때 `areNotificationsEnabled()`가 false를 돌려주고 `ACTION_APP_NOTIFICATION_SETTINGS`가 앱의 알림 설정 화면을 여는지.

알림과 Live Updates

- 승격된 알림 카드에 버튼 3개가 다 보이는지, 잠금 화면에서 눌리는지, 칩을 눌렀을 때 무엇이 열리는지.
- 카운트다운이 0에 닿은 순간과 서비스를 멈춘 직후의 칩과 알림 모습.
- Android 16의 어느 빌드부터 칩이 나오는지. 36.0 기기에서 `NotificationCompat`의 승격 요청이 어떻게 처리되는지.
- 삼성 등 제조사 기기에서 승격이 되는지, 어떤 모양으로 보이는지.
- 앱이 화면에 보이는 동안 칩과 알림이 어떻게 보이는지. 앱 안의 휴식 바와 겹쳐 보이는지.
- 중요도 `IMPORTANCE_LOW` 채널의 종료 알림이 사용자 눈에 충분히 띄는지. 화면 위로 잠깐 뜨는 알림이 필요한지.

서비스

- 최근 앱 목록에서 앱을 밀어 없앤 뒤 서비스가 이어지는지. 제조사별로 본다.
- API 26~33에서 매니페스트의 `foregroundServiceType="specialUse"`가 문제없이 무시되는지.

문서로 확인하지 못한 것

- 프로세스가 죽은 뒤 시스템이 `START_STICKY` 서비스를 다시 만들 때 `startForeground()`가 허용되는지, WIU 권한이 붙는지.
- `shouldShowRequestPermissionRationale()`의 반환값이 거절 횟수에 따라 언제 바뀌는지. 가이드에는 true면 설명 화면을 보이라는 말만 있다.
- 방해 금지 중에 앱이 직접 내는 알림 용도의 소리와 진동을 시스템이 어디까지 막는지. 레퍼런스는 필터마다 "일부 오디오 스트림을 끈다"거나 "모든 오디오 스트림과 진동을 끈다"고만 적는다. 이 앱은 방해 금지 중에 신호를 내지 않으므로 동작에는 영향이 없다.
- Live Updates 가이드는 설정 화면을 여는 액션을 `Settings.ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS`로 적는데 `Settings` 레퍼런스에는 `ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`만 있다. 컴파일해서 확인한다.
- 승격된 알림을 사용자가 "일시적으로 닫거나 내릴 수 있다"는 문장은 있으나 그 뒤 앱이 받는 신호가 `setDeleteIntent()` 말고 더 있는지는 적혀 있지 않다.
- Live Update를 얼마나 자주 띄워도 되는지. 한 번 운동에 휴식이 스무 번쯤 생긴다. 문서에 횟수 제한은 없다.
- Android 17 변경 페이지는 "중요한 변경 일부"만 싣는다고 적는다. 릴리스 노트 전체는 읽지 않았다.
- Play 스토어의 `specialUse` 심사 기준. v1은 Play에 올리지 않아 읽지 않았다.
- 제조사 절전 기능의 구체적인 동작. 제3자 사이트가 있다는 것만 적었고 내용을 검증하지 않았다.

출처가 약한 것

- 삼성 One UI의 Live Updates 지원은 보도 두 건의 검색 요약으로만 읽었다. 기사 본문과 삼성 공식 개발자 문서는 확인하지 못했다.

## 출처

Live Updates와 알림

- [Live Updates][live]
- [진행 중심 알림][pcn]
- [알림 권한][notif-perm], [런타임 권한 요청][perm-req]
- [알림 채널][channels], [알림 만들기][build-notif], [알림 디자인 가이드][design-notif]
- [Live Updates 샘플][sample](공식 샘플 저장소)

포그라운드 서비스와 백그라운드 실행

- [포그라운드 서비스 종류][fgs-types], [선언][fgs-declare], [시작][fgs-launch], [중지][fgs-stop]
- [백그라운드 시작 제한][fgs-bg], [시간 제한][fgs-timeout], [변경 이력][fgs-changes]
- [사용자가 앱을 중지할 때][fgs-user-stop]
- [프로세스 수명 주기][process]
- [알람 예약][alarms], [정확한 알람 기본 거부][a14-exact]

전원 관리

- [Doze와 앱 대기][doze], [전원 관리 제한][power], [백그라운드 최적화][bg-opt]
- [기기를 깨워 두는 방법][awake], [wake lock 권장 사항][wl-bp], [과도한 부분 wake lock][wl-vitals]

버전별 변경

- [백그라운드 오디오 강화(Android 17)][bg-audio], [Android 17 변경][a17-all], [Android 17 변경(대상 앱)][a17-target]
- [Android 15 변경][a15-all], [Android 15 변경(대상 앱)][a15-target]
- [Android 14 변경][a14-all], [Android 13 변경][a13-all], [Android 12 변경(대상 앱)][a12-target]
- [Android 8.0 백그라운드 실행 제한][a8-bg]

API 레퍼런스

- [Notification][ref-notif], [Notification.Builder][ref-builder], [NotificationCompat.Builder][ref-compat]
- [ProgressStyle][ref-progress], [MetricStyle][ref-metric]
- [NotificationManager][ref-nm], [Settings][ref-settings], [Manifest.permission][ref-perm]
- [AudioManager][ref-am], [AudioAttributes][ref-aa], [SoundPool][ref-soundpool], [ToneGenerator][ref-tonegen]
- [Vibrator][ref-vib], [VibrationAttributes][ref-va]
- [Service][ref-service], [ServiceCompat][ref-svccompat], [PowerManager][ref-pm], [VERSION_CODES_FULL][ref-vcf]
- [androidx.core 릴리스 노트][core-rel]

제3자 글

- [SamMobile: One UI 8 Now Bar integrates Android 16 Live Updates][sammobile]
- [Android Authority: One UI 8 Live Updates support][aa-oneui8]
- [Don't kill my app!][dkma]

[live]: https://developer.android.com/develop/ui/views/notifications/live-update
[pcn]: https://developer.android.com/about/versions/16/features/progress-centric-notifications
[notif-perm]: https://developer.android.com/develop/ui/views/notifications/notification-permission
[perm-req]: https://developer.android.com/training/permissions/requesting
[channels]: https://developer.android.com/develop/ui/views/notifications/channels
[build-notif]: https://developer.android.com/develop/ui/views/notifications/build-notification
[design-notif]: https://developer.android.com/design/ui/mobile/guides/home-screen/notifications
[sample]: https://github.com/android/platform-samples/tree/main/samples/user-interface/live-updates
[fgs-types]: https://developer.android.com/develop/background-work/services/fgs/service-types
[fgs-declare]: https://developer.android.com/develop/background-work/services/fgs/declare
[fgs-launch]: https://developer.android.com/develop/background-work/services/fgs/launch
[fgs-stop]: https://developer.android.com/develop/background-work/services/fgs/stop-fgs
[fgs-bg]: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
[fgs-timeout]: https://developer.android.com/develop/background-work/services/fgs/timeout
[fgs-changes]: https://developer.android.com/develop/background-work/services/fgs/changes
[fgs-user-stop]: https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping
[process]: https://developer.android.com/guide/components/activities/process-lifecycle
[alarms]: https://developer.android.com/develop/background-work/services/alarms
[a14-exact]: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
[doze]: https://developer.android.com/training/monitoring-device-state/doze-standby
[power]: https://developer.android.com/topic/performance/power/power-details
[bg-opt]: https://developer.android.com/topic/performance/background-optimization
[awake]: https://developer.android.com/develop/background-work/background-tasks/awake
[wl-bp]: https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/best-practices
[wl-vitals]: https://developer.android.com/topic/performance/vitals/excessive-wakelock
[bg-audio]: https://developer.android.com/about/versions/17/changes/bg-audio
[a17-all]: https://developer.android.com/about/versions/17/behavior-changes-all
[a17-target]: https://developer.android.com/about/versions/17/behavior-changes-17
[a15-all]: https://developer.android.com/about/versions/15/behavior-changes-all
[a15-target]: https://developer.android.com/about/versions/15/behavior-changes-15
[a14-all]: https://developer.android.com/about/versions/14/behavior-changes-all
[a13-all]: https://developer.android.com/about/versions/13/behavior-changes-all
[a12-target]: https://developer.android.com/about/versions/12/behavior-changes-12
[a8-bg]: https://developer.android.com/about/versions/oreo/background
[ref-notif]: https://developer.android.com/reference/android/app/Notification
[ref-builder]: https://developer.android.com/reference/android/app/Notification.Builder
[ref-compat]: https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder
[ref-progress]: https://developer.android.com/reference/android/app/Notification.ProgressStyle
[ref-metric]: https://developer.android.com/reference/android/app/Notification.MetricStyle
[ref-nm]: https://developer.android.com/reference/android/app/NotificationManager
[ref-settings]: https://developer.android.com/reference/android/provider/Settings
[ref-perm]: https://developer.android.com/reference/android/Manifest.permission
[ref-am]: https://developer.android.com/reference/android/media/AudioManager
[ref-aa]: https://developer.android.com/reference/android/media/AudioAttributes
[ref-soundpool]: https://developer.android.com/reference/android/media/SoundPool
[ref-tonegen]: https://developer.android.com/reference/android/media/ToneGenerator
[ref-vib]: https://developer.android.com/reference/android/os/Vibrator
[ref-va]: https://developer.android.com/reference/android/os/VibrationAttributes
[ref-service]: https://developer.android.com/reference/android/app/Service
[ref-svccompat]: https://developer.android.com/reference/androidx/core/app/ServiceCompat
[ref-pm]: https://developer.android.com/reference/android/os/PowerManager
[ref-vcf]: https://developer.android.com/reference/android/os/Build.VERSION_CODES_FULL
[core-rel]: https://developer.android.com/jetpack/androidx/releases/core
[sammobile]: https://www.sammobile.com/news/one-ui-8-now-bar-integrates-android-16-live-updates/
[aa-oneui8]: https://www.androidauthority.com/one-ui-8-live-updates-support-3573794
[dkma]: https://dontkillmyapp.com/
