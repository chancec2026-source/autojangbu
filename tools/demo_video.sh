#!/usr/bin/env bash
# 오토장부 시연 영상 원본 녹화(개발용 빌드 + Pleos 에뮬레이터 + 가상 콘솔).
# 사용: bash demo_video.sh [저장할 mp4 경로]
# 숫자와 장소는 모두 예시 데이터다.
set -u
export MSYS_NO_PATHCONV=1   # Git Bash가 /sdcard 경로를 윈도우 경로로 바꾸지 않게
ADB="${ADB:-C:/Users/HP/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
E="-s emulator-5554"
B=http://127.0.0.1:8088
OUT="${1:-demo.mp4}"
PKG=kr.chance.autojangbu

"$ADB" $E forward tcp:8088 tcp:8088 >/dev/null
(while true; do curl -s -m 2 -X POST "$B/browser/session?client=autojangbu" >/dev/null; sleep 1; done) &
HB=$!
trap 'kill $HB 2>/dev/null' EXIT
sleep 1

odo_now() { curl -s -m 5 "$B/vehicle/state" | grep -oE '"traveled_distance_km":[0-9.]+' | head -1 | cut -d: -f2 | cut -d. -f1; }
gear()  { curl -s -m 5 -X POST "$B/vehicle/gear?gear=$1" >/dev/null; }
speed() { curl -s -m 5 -X PUT "$B/vehicle/controls?speed=$1&accelerator=$2&brake=$3&steering=0" >/dev/null; }
odo()   { curl -s -m 5 -X POST "$B/vehicle/odometer?traveled_distance_km=$1" >/dev/null; }
door()  { curl -s -m 5 -X POST "$B/vehicle/body?door_fl=$1" >/dev/null; }
seat()  { curl -s -m 5 -X POST "$B/vehicle/seat?kind=occupied&area=driver&value=$1" >/dev/null; }
tap()   { "$ADB" $E shell input tap "$1" "$2"; }
# 화면 구성에 따라 버튼 위치가 바뀌므로, 글자로 버튼을 찾아 가운데를 누른다.
tap_text() {
  "$ADB" $E shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b
  b=$("$ADB" $E shell cat /sdcard/ui.xml | tr '>' '\n' | grep -F "text=\"$1" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' ')
  set -- $b
  [ $# -eq 4 ] && tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}
voice() { "$ADB" $E shell "am broadcast --user 10 -a $1 -p $PKG --es ACTION_PARAM '$2'" >/dev/null; }

# 준비: 예시 한 달 운행을 넣고, 오늘 아침 출근 운행은 분류해 둔다.
START=$(odo_now)
# 새로 설치한 앱은 한 번 실행되기 전까지 방송을 받지 않으므로 먼저 띄운다.
"$ADB" $E shell am start --user 10 -n $PKG/.MainActivity >/dev/null
sleep 3
"$ADB" $E shell am broadcast --user 10 -a $PKG.DEMO_SEED -p $PKG --ef odo "$START" >/dev/null
voice CLASSIFY_LAST_TRIP '{"actionParameters":{"category":"commute"}}'
gear Park; speed 0 0 0; door false; seat true
"$ADB" $E shell am start --user 10 -n $PKG/.MainActivity >/dev/null
sleep 4
# 이전 시험에서 열어 둔 창(QR·메모·장부 정보)이 있으면 닫는다.
tap_text "닫기"; tap_text "취소"; tap_text "운행 목록"; tap_text "이번 달"; sleep 1

# 녹화 시작
"$ADB" $E shell screenrecord --size 1920x1080 --time-limit 100 /sdcard/autojangbu_demo.mp4 &
REC=$!
sleep 4

# 출발: D단, 속도를 올리며 14km 주행
gear Drive; sleep 1
for v in 20 40 60; do speed $v 30 0; sleep 1; done
for ((i=1; i<=14; i++)); do odo $((START + i)); sleep 0.7; done
for v in 40 20 0; do speed $v 0 40; sleep 1; done

# 도착: P단, 문 열기 → 운행 저장, 알림
gear Park; sleep 2
door true; seat false; sleep 5

# 음성 분류(Gleo 호출과 같은 신호)
voice CLASSIFY_LAST_TRIP '{"actionParameters":{"category":"business","note":"성수동 거래처 미팅"}}'
sleep 5

# 이번 달 결산 → 운행 목록 → 국세청 양식 내보내기
tap_text "이번 달 결산"; sleep 6
tap_text "운행 목록"; sleep 3
tap_text "이번 달 국세청 양식"; sleep 3
# 휴대폰으로 받기: 장부를 올리고 QR을 띄운다(인터넷 필요)
tap_text "휴대폰으로 받기"; sleep 7
tap_text "닫기"; sleep 1

"$ADB" $E shell pkill -INT screenrecord
wait $REC 2>/dev/null
sleep 2
"$ADB" $E pull /sdcard/autojangbu_demo.mp4 "$OUT" >/dev/null && echo "저장: $OUT"
door false
