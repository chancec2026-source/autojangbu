# 숏폼 다시 만들기

이 폴더에서 실행: `ffmpeg -i <시연_원본.mp4> -/filter_complex filter.txt -map "[out]" -c:v libx264 -pix_fmt yuv420p -crf 20 숏폼.mp4`

- c1~c6.txt: 장면별 자막, foot.txt: 아래 고정 문구
- filter.txt의 trim 값은 `tools/demo_video.sh`로 찍은 원본의 장면 시간 기준
