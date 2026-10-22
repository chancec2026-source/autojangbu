@echo off
rem 오토장부 체험: 15km 운행 한 번을 자동으로 재생한다(가상 콘솔).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\trip_play.ps1" -Km 15
pause
