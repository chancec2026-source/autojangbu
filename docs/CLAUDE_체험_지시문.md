# Claude Code 지시문 — 오토장부 체험 환경 세팅·실행

> 이 문서는 **Claude Code가 읽고 실행하는 지시문**이다. 사용자(대표님)는 VSCode의 Claude에 "docs/CLAUDE_체험_지시문.md 대로 해줘"라고만 말한다.
> 사람이 직접 따라 하는 쉬운 안내는 저장소 루트의 `체험_안내.md`.

## 목표
Windows PC에 Pleos Connect 에뮬레이터를 띄우고, 오토장부 체험판(`apk/autojangbu-trial.apk`)을 설치해 아래를 확인한 뒤 화면 캡처 3장으로 보고한다.
1. 예시 한 달 치 운행이 보이는 첫 화면
2. 운행 1건 재생 후 뜨는 "이번 운행, 어떻게 기록할까요?" 카드
3. '이번 달 결산' 화면

빌드는 필요 없다(체험판 APK가 저장소에 있다). 사용자가 직접 눌러 보며 만질 수 있게 마지막에 에뮬레이터와 가상 콘솔(브라우저)을 켜 둔 채로 끝낸다.

## 진행 원칙
- 사용자에게 여쭙는 것은 두 가지뿐: ① Pleos SDK 이용약관(42dot) 동의 — 에뮬레이터 이미지 설치 전에 한 번, ② 저장소 접근 권한이 없을 때 GitHub 로그인. 나머지는 명령어로 직접 한다.
- 다른 에뮬레이터가 켜져 있으면 건드리지 말고, adb 명령은 Pleos 에뮬레이터 시리얼(`-s emulator-XXXX`)로만 한다.
- 막히면 원인 → 조치 → 결과를 짧게 보고한다.

## 0. 저장소 받기
```
gh repo clone chancec2026-source/autojangbu E:\app\autojangbu
```
- 비공개 저장소다. 권한 오류면 사용자에게 `gh auth login`(찬스 계정 또는 초대받은 계정)을 부탁한다.
- **경로에 한글을 넣지 않는다**(나중에 빌드할 때 Gradle이 한글 경로를 거부한다).

## 1. 준비물 확인
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (emulator, platform-tools 있어야 함). 없으면 Android Studio 설치를 사용자에게 부탁한다(설치는 GUI).
- 가속: `"%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -accel-check` → WHPX 또는 HAXM usable.
- 디스크 여유 30GB 이상(이미지 압축 4.1GB + 풀면 19.6GB).

## 2. Pleos Connect 에뮬레이터 이미지 설치 (약관 동의 받은 뒤)
GUI 방법은 `체험_안내.md` 2단계. 명령어로 하려면:
1. 내려받기(4,087,751,964 bytes, sha1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa`):
   `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_emulator.zip`
   — 한글 없는 폴더에 받고 sha1을 확인한다(목록 XML: `.../connect-avd/2.x/crp-sys-img.xml`).
2. 압축 풀기 → 안의 `crp_avd_out_x86_64` 폴더를 `%LOCALAPPDATA%\Android\Sdk\system-images\android-34\pleos_car_emulator\x86_64` 로 옮긴다.
3. 같은 폴더에 `tools/pleos-avd/package.xml` 을 복사한다(SDK가 이미지를 알아보게).
4. AVD 만들기:
   - `%USERPROFILE%\.android\avd\Pleos_Connect_v2.1.avd\config.ini` ← `tools/pleos-avd/config.ini`
   - `%USERPROFILE%\.android\avd\Pleos_Connect_v2.1.ini` ← `tools/pleos-avd/Pleos_Connect_v2.1.ini` (`path=` 를 실제 사용자 폴더 절대경로로 바꾼다)
   - 확인: `emulator -list-avds` 에 `Pleos_Connect_v2.1`
5. 실행: `emulator -avd Pleos_Connect_v2.1 -no-snapshot-load -no-boot-anim` (백그라운드) → `adb -s emulator-5554 shell getprop sys.boot_completed` 가 `1` 이 될 때까지 기다린다(처음 몇 분).
6. 시간대를 한국으로(기본 UTC라 '출퇴근/업무' 추천이 틀어진다):
   `adb -s <시리얼> shell settings put global auto_time_zone 0` → `adb -s <시리얼> shell cmd alarm set-timezone Asia/Seoul`

## 3. 체험판 설치와 실행
- `trial_start.bat` 이 하는 일을 그대로 하거나 실행한다: 체험판 설치 → `adb forward tcp:8088 tcp:8088` → 앱 실행 → 브라우저 `http://127.0.0.1:8088/`(가상 콘솔).
  - bat 끝의 `pause` 때문에 멈추므로 `cmd /c "echo.| <경로>\trial_start.bat"` 로 실행한다.
  - 에뮬레이터가 여러 대면 bat의 `-e` 가 실패한다 → adb 명령을 시리얼 지정으로 직접 실행한다.
- 알림 권한: `adb -s <시리얼> shell pm grant --user 10 kr.chance.autojangbu android.permission.POST_NOTIFICATIONS`
- 예시 데이터: 앱 **장부 정보 → 체험용 예시 데이터 넣기** 버튼을 누르거나(사용자도 이것을 쓰면 된다), 명령으로
  `adb -s <시리얼> shell am broadcast --user 10 -a kr.chance.autojangbu.DEMO_SEED -p kr.chance.autojangbu --ef odo <지금 계기판 km>`

## 4. 운행 1건 재생과 확인
- `trial_drive.bat`(= `powershell -ExecutionPolicy Bypass -File tools\trip_play.ps1 -Km 15`)
- 확인: `adb -s <시리얼> logcat -d -s AutoJangbu` 에 `운행 저장 A→B km (15.0km)`
- 캡처: `adb -s <시리얼> exec-out screencap -p > shot.png` (Git Bash면 `/sdcard` 경로 앞에 `MSYS_NO_PATHCONV=1`)
- '이번 달 결산' 탭은 화면 오른쪽 위. 버튼 위치는 `uiautomator dump` 로 글자를 찾아 누른다(`tools/demo_video.sh` 의 `tap_text` 참고).

## 알려진 함정 (실제로 겪은 것)
| 증상 | 원인·조치 |
|---|---|
| am start/broadcast가 안 먹음 | Pleos 에뮬레이터의 사용자는 **user 10** → `--user 10` |
| 새로 설치한 앱이 방송을 무시 | 한 번도 실행 안 한 앱은 '정지 상태' → 앱을 먼저 실행 |
| 가상 콘솔 값 변경이 `emulator_inactive` | `POST /browser/session?client=아무거나` 를 3초 안에 계속 보내야 함(`trip_play.ps1` 참고) |
| 운행이 0km라 기록 안 됨 | 계기판은 뒤로 안 가고, 속도를 올려도 거리는 안 늚 → 계기판 값을 직접 올림 |
| 연료(%) 미지원, 시트 위치 늘 0, Gleo 음성 앱 없음 | 에뮬레이터 한계(정상). 시트 0/0/0이면 앱이 기본 사용자로 기록 |
| `inject-vhal-event` SecurityException | user 빌드라 막힘 → 가상 콘솔 HTTP로만 값을 바꾼다 |
| 화면 녹화 파일이 안 받아짐(Git Bash) | `/sdcard` 경로 변환 → `MSYS_NO_PATHCONV=1` |

## (선택) 직접 빌드하려면
- JDK 17, `car-app/local.properties`: `sdk.dir=...`, `autojangbu.shareUrl=https://chance-autojangbu.chancec2026.workers.dev`, `autojangbu.appKey=...`
- 앱 키는 저장소에 없다 → 필요하면 사장님께 요청. 없이 빌드해도 되지만 'QR 공유'만 실패한다.
- `cd car-app && gradlew testDebugUnitTest assembleDebug` (테스트 18개)

## 완료 보고 양식
- 화면 캡처 3장(첫 화면 · 분류 카드 · 결산)
- 된 것 / 안 된 것(에뮬레이터 한계는 '정상'으로 표시)
- 사용자가 지금 눌러 볼 수 있는 상태인지(에뮬레이터·가상 콘솔 켜 둠)
