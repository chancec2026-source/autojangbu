#!/usr/bin/env bash
# 오토장부 시연용: Pleos 가상 콘솔로 '운행 1건'을 재생한다.
# 사용: adb forward tcp:8088 tcp:8088 후 bash demo_trip.sh [주행km] [출발km]
# 계기판은 실제 차처럼 뒤로 돌아가지 않으므로, 출발km를 안 주면 지금 계기판 값에서 이어 달린다.
B=http://127.0.0.1:8088
DIST=${1:-18}
curl -s -m 2 -X POST "$B/browser/session?client=autojangbu" >/dev/null
NOW=$(curl -s -m 5 "$B/vehicle/state" | grep -oE '"traveled_distance_km":[0-9.]+' | head -1 | cut -d: -f2 | cut -d. -f1)
START=${2:-${NOW:-0}}

# 가상 콘솔은 브라우저 세션이 3초마다 살아 있어야 값을 받는다.
(while true; do curl -s -m 2 -X POST "$B/browser/session?client=autojangbu" >/dev/null; sleep 1; done) &
HB=$!
trap 'kill $HB 2>/dev/null' EXIT
sleep 1

set_odo()  { curl -s -m 5 -X POST "$B/vehicle/odometer?traveled_distance_km=$1" >/dev/null; }
set_gear() { curl -s -m 5 -X POST "$B/vehicle/gear?gear=$1" >/dev/null; }
set_seat() { curl -s -m 5 -X POST "$B/vehicle/seat?kind=occupied&area=driver&value=$1" >/dev/null; }
set_door() { curl -s -m 5 -X POST "$B/vehicle/body?door_fl=$1" >/dev/null; }

echo "탑승: 계기판 ${START}km, P단"
set_odo "$START"; set_gear Park; set_door true; sleep 2
set_seat true; set_door false; sleep 2

echo "출발: D단"
set_gear Drive; sleep 2
for ((i=1; i<=DIST; i++)); do set_odo $((START + i)); sleep 0.5; done

echo "도착: P단, 문 열림"
set_gear Park; sleep 2
set_door true; set_seat false; sleep 2
echo "끝: 계기판 $((START + DIST))km"
