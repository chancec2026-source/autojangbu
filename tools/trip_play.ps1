# 오토장부 체험: Pleos 가상 콘솔로 운행 1건을 재생한다(출발 D → 계기판 +km → 주차 P → 운전석 문 열기).
# 사용: powershell -ExecutionPolicy Bypass -File trip_play.ps1 [-Km 15]
param([int]$Km = 15)
$ErrorActionPreference = "Stop"
$B = "http://127.0.0.1:8088"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adb) { & $adb -e forward tcp:8088 tcp:8088 | Out-Null }

# 가상 콘솔은 3초마다 세션이 살아 있어야 값을 받는다 → 매번 먼저 알린다.
function Ping { Invoke-RestMethod -Method Post "$B/browser/session?client=trial" | Out-Null }
function Post($path) { Ping; Invoke-RestMethod -Method Post "$B$path" | Out-Null }
function Put($path) { Ping; Invoke-RestMethod -Method Put "$B$path" | Out-Null }

try { Ping } catch {
  Write-Host "[안내] 가상 콘솔에 연결하지 못했습니다. 에뮬레이터를 켜고 trial_start.bat 을 먼저 실행해 주세요."
  exit 1
}
$state = Invoke-RestMethod "$B/vehicle/state"
$start = [int][math]::Floor($state.odometer.traveled_distance_km)
Write-Host "탑승: 계기판 $start km"
Post "/vehicle/seat?kind=occupied&area=driver&value=true"
Post "/vehicle/body?door_fl=false"
Start-Sleep -Seconds 1

Write-Host "출발: 기어 D"
Post "/vehicle/gear?gear=Drive"
foreach ($v in 20, 40, 60) { Put "/vehicle/controls?speed=$v&accelerator=30&brake=0&steering=0"; Start-Sleep -Milliseconds 700 }
for ($i = 1; $i -le $Km; $i++) {
  Post "/vehicle/odometer?traveled_distance_km=$($start + $i)"
  Start-Sleep -Milliseconds 500
}
foreach ($v in 30, 0) { Put "/vehicle/controls?speed=$v&accelerator=0&brake=40&steering=0"; Start-Sleep -Milliseconds 700 }

Write-Host "주차: 기어 P, 운전석 문 열기"
Post "/vehicle/gear?gear=Park"
Start-Sleep -Seconds 2
Post "/vehicle/body?door_fl=true"
Post "/vehicle/seat?kind=occupied&area=driver&value=false"
Start-Sleep -Seconds 3
Post "/vehicle/body?door_fl=false"
Write-Host "끝: 계기판 $($start + $Km) km . 오토장부에 '운행 $Km km 기록했어요'가 떴는지 보세요."
