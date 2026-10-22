@echo off
rem 오토장부 체험 시작: Pleos 에뮬레이터에 체험판을 깔고, 가상 콘솔(운전 조작 화면)을 브라우저로 연다.
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" (
  echo [안내] Android Studio가 설치되어 있지 않습니다. 체험_안내.md 의 1단계를 먼저 해 주세요.
  pause
  exit /b 1
)
echo [1/4] Pleos 에뮬레이터를 기다리는 중입니다.
echo       Android Studio의 Device Manager에서 Pleos Connect 에뮬레이터를 먼저 켜 주세요.
"%ADB%" -e wait-for-device
:boot
set BOOT=
for /f %%b in ('"%ADB%" -e shell getprop sys.boot_completed 2^>nul') do set BOOT=%%b
if not "%BOOT%"=="1" (
  timeout /t 3 >nul
  goto boot
)
echo [2/4] 오토장부 체험판을 설치합니다.
"%ADB%" -e install -r "%~dp0apk\autojangbu-trial.apk"
echo [3/4] 가상 콘솔을 연결합니다.
"%ADB%" -e forward tcp:8088 tcp:8088
"%ADB%" -e shell am start -n kr.chance.autojangbu/.MainActivity >nul
echo [4/4] 브라우저에 가상 콘솔을 엽니다.
start "" http://127.0.0.1:8088/
echo.
echo 준비 끝! 체험_안내.md 의 3단계부터 따라 해 보세요.
echo  - 에뮬레이터 화면: 오토장부 앱(마우스로 누르기)
echo  - 브라우저 화면: 가상 콘솔(기어 P/D, 계기판 거리, 문 열기로 운전 흉내)
pause
