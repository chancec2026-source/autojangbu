# 오토장부 (AutoJangbu)

시동만 걸고 다니면, 차 계기판 거리를 그대로 읽어 **국세청 업무용 승용차 운행기록부**를 자동으로 채워 주는 Pleos Connect 차량 앱입니다.

## 무엇을 하나요
- **자동 운행 기록**: 기어가 D로 바뀌면 출발 계기판 거리, P에서 운전석 문이 열리면 도착 계기판 거리를 저장합니다. 화면이 꺼져 있어도 상주 서비스가 기록합니다.
- **말 한마디 분류**: 주차하면 업무·출퇴근·개인 버튼이 달린 알림이 뜹니다. Gleo AI에게 "오토장부, 업무로 기록해. 강남 현장 다녀왔어"라고 말하면 구분과 메모가 저장됩니다.
- **이번 달 결산**: 업무사용비율, 날짜별 운행 막대, 가장 긴 운행, 자주 간 곳.
- **국세청 양식 내보내기**: 머리말(과세기간·상호·사업자등록번호·차종·자동차등록번호) 포함 엑셀용 CSV, 이번 달 / 사업연도 전체, QR 코드로 휴대폰에 받아 세무사에게 링크로 보내기. 방문 목적이 빠진 업무 운행은 표시.
- **운전자 여러 명**: 운전자별 시트 위치를 기억해 출발할 때 누가 운전하는지 알아보고 사용자 칸을 채움.
- 위치 좌표는 수집하지 않습니다.

## 직접 만져 보기
PC에서 Pleos 에뮬레이터로 체험하는 방법은 **[체험_안내.md](체험_안내.md)** 에 있습니다(`trial_start.bat` → `trial_drive.bat` 더블클릭).

## 문서·영상
`docs/` — 기획안, 후보 발굴 요약, 기획서(접수양식) 초안, 공식 접수양식 원본, `docs/영상/` 시연 영상(720p)·숏폼·자막 대본

화면 디자인은 시안 A **'나이트 클러스터'**(어두운 계기판형)입니다. 시안 3종 비교와 적용 화면 캡처는 `docs/디자인시안/`(보드: `index.html`). 앱에 넣은 글꼴 SUIT · Barlow Semi Condensed 는 OFL이며 라이선스 사본은 `car-app/app/src/main/assets/licenses/` 에 있습니다.

## 폴더
| 폴더 | 내용 |
|---|---|
| `car-app/` | 차량 앱 (Android · Kotlin · Jetpack Compose) |
| `share-web/` | 장부 공유 서버 (Cloudflare Worker + KV, 60일 뒤 자동 삭제) |
| `tools/` | 에뮬레이터 시연 스크립트 (가상 콘솔로 운행 재생, 시연 영상 녹화) |
| `apk/` | 심사용 release APK, 체험판 `autojangbu-trial.apk`(예시 데이터 버튼 포함) |

## 사용한 Pleos API
- **Vehicle SDK 2.3.2**: `Odometer.getCurrentDistanceDriven` / `registerDistanceDriven`, `Gear.registerGearSelection`, `Door.registerDoorPosition`, `Seat`(착석·시트 위치), `CarInfo.getCRNInfo`(자동차등록번호)
- **Gleo AI App Schema** (`car-app/app/src/main/assets/schema.json`): `CLASSIFY_LAST_TRIP`, `ADD_TRIP_NOTE`, `GET_MONTHLY_LOGBOOK_SUMMARY` — `component=service`, 결과는 `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE` 콜백

## 빌드와 실행
준비물: Android Studio, JDK 17, Pleos Connect Emulator v2.1.0 (API 34)

```bash
cd car-app
# local.properties: sdk.dir=..., autojangbu.shareUrl=..., autojangbu.appKey=...
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

에뮬레이터에서 운행 재생(가상 콘솔):

```bash
adb forward tcp:8088 tcp:8088
bash tools/demo_trip.sh 18          # 지금 계기판 값에서 18km 운행 1건
```

Gleo 음성 호출 재현(에뮬레이터에 Gleo 음성 앱이 없을 때):

```bash
adb shell "am broadcast --user 10 -a CLASSIFY_LAST_TRIP -p kr.chance.autojangbu \
  --es ACTION_PARAM '{\"actionParameters\":{\"category\":\"business\",\"note\":\"거래처 미팅\"}}'"
```

## 테스트
`car-app/app/src/test` — 운행 판단 규칙(잠깐 정차, 기어만 바꾼 경우, 3분 정차 등), 국세청 양식 CSV, 음성 파라미터 해석.
