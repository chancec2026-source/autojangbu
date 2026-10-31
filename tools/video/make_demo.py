# 시연 영상 편집: 원본 녹화(raw.mp4)를 장면별로 자르고 자막·앞뒤 소개 화면을 붙인다.
# 사용: bash tools/demo_video.sh <작업폴더>/raw.mp4 → python tools/video/make_demo.py <작업폴더>
# 장면 시간(SEG)은 녹화 때 함께 저장되는 raw.mp4.scenes.txt 를 보고 맞춘다.
import pathlib, subprocess, shutil, sys

D = pathlib.Path(sys.argv[1])
FONT = pathlib.Path(__file__).resolve().parents[2] / "car-app/app/src/main/res/font"
for f in ("suit_extrabold.ttf", "suit_bold.ttf", "suit_medium.ttf"):
    shutil.copy(FONT / f, D / f)

RAW = "raw.mp4"
INTRO, OUTRO = 4.5, 6.0
# (원본 시작, 원본 끝, 배속, 큰 자막, 작은 자막)
SEG = [
    (0.3, 5.3, 1, "업무용 차 운행기록부를, 차가 알아서 씁니다",
     "이번 달 업무사용비율 · 운행 목록 · 확인할 것이 한 화면에"),
    (5.3, 26.3, 3, "D로 출발하면 계기판 거리 기록 시작  (3배속)",
     "주행 중에는 화면을 쓰지 않습니다 · 운전자에게 아무것도 묻지 않아요"),
    (26.3, 35.2, 1, "P로 주차하고 운전석 문을 열면, 운행 1건 완성",
     "출발·도착 계기판 km 그대로 · 시간대로 구분을 미리 추천, 버튼 한 번이면 끝"),
    (35.2, 41.7, 1, "“오토장부, 업무로 기록해. 성수동 거래처 미팅”",
     "말 한마디로 구분과 방문 목적 저장 · Gleo AI 음성 호출과 같은 신호로 재현"),
    (41.7, 51.7, 1, "이번 달 운행 결산",
     "날짜별 운행 · 업무사용비율 · 가장 긴 운행 · 자주 간 곳"),
    (54.5, 64.5, 1, "국세청 업무용승용차 운행기록부 양식 그대로 엑셀 저장",
     "방문 목적이 빠진 업무 운행은 따로 표시해 채우게 합니다"),
    (66.5, 77.0, 1, "장부 정보: 상호 · 사업자등록번호 · 차종 · 자동차등록번호",
     "운전자 여러 명은 시트 위치로 알아보고, 운행기록부 사용자 칸을 자동으로 채웁니다"),
    (80.0, 96.0, 1, "QR 한 번이면 휴대폰으로, 세무사님께는 링크로",
     "이번 달 또는 올해 전체 · 공유 링크는 60일 뒤 자동 삭제"),
]

def txt(name, s):
    (D / name).write_text(s, encoding="utf-8")
    return name

def dt(font, file, size, color, y, extra=""):
    return (f"drawtext=fontfile={font}:textfile={file}:fontsize={size}:fontcolor={color}"
            f":x=(w-text_w)/2:y={y}{extra}")

parts, labels = [], []
# 앞 소개: 첫 화면을 흐리고 어둡게 깔고 제목
parts.append(
    f"[0:v]trim=0.3:0.4,setpts=PTS-STARTPTS,loop=loop=-1:size=1:start=0,trim=0:{INTRO},setpts=PTS-STARTPTS,"
    f"fps=30,scale=1920:1080,boxblur=20:2,colorchannelmixer=rr=0.35:gg=0.35:bb=0.35,"
    + dt("suit_extrabold.ttf", txt("i1.txt", "오토장부"), 120, "white", 330) + ","
    + dt("suit_bold.ttf", txt("i2.txt", "시동만 걸고 다니면, 차 계기판 거리로"), 54, "white", 500) + ","
    + dt("suit_bold.ttf", txt("i3.txt", "국세청 업무용승용차 운행기록부가 자동으로 채워집니다"), 54, "white", 580) + ","
    + dt("suit_medium.ttf", txt("i4.txt", "Pleos Connect 에뮬레이터 실제 동작 화면 녹화  ·  화면 속 운행·업체 정보는 예시입니다"), 30, "0x9FB3C8", 760)
    + ",format=yuv420p[intro]")
labels.append("[intro]")

for i, (a, b, sp, big, small) in enumerate(SEG):
    pts = f"(PTS-STARTPTS)/{sp}" if sp != 1 else "PTS-STARTPTS"
    parts.append(
        f"[0:v]trim={a}:{b},setpts={pts},fps=30,scale=1920:1080,"
        f"drawbox=x=0:y=962:w=iw:h=118:color=0x070B10@0.94:t=fill,"
        f"drawbox=x=0:y=962:w=iw:h=3:color=0x3FB6FF@0.9:t=fill,"
        + dt("suit_bold.ttf", txt(f"b{i}.txt", big), 40, "white", 978) + ","
        + dt("suit_medium.ttf", txt(f"s{i}.txt", small), 27, "0xA9BCCF", 1032)
        + f",format=yuv420p[v{i}]")
    labels.append(f"[v{i}]")

# 뒤 마무리
parts.append(
    f"[0:v]trim=93:93.1,setpts=PTS-STARTPTS,loop=loop=-1:size=1:start=0,trim=0:{OUTRO},setpts=PTS-STARTPTS,"
    f"fps=30,scale=1920:1080,boxblur=20:2,colorchannelmixer=rr=0.3:gg=0.3:bb=0.3,"
    + dt("suit_bold.ttf", txt("o1.txt", "단말기도, 휴대폰 앱도 없이"), 58, "white", 300) + ","
    + dt("suit_extrabold.ttf", txt("o2.txt", "차에 이미 있는 숫자로, 오토장부"), 76, "white", 390) + ","
    + dt("suit_medium.ttf", txt("o3.txt", "Pleos Vehicle SDK  ·  Odometer · Gear · Door · Seat · CarInfo"), 34, "0xA9BCCF", 580) + ","
    + dt("suit_medium.ttf", txt("o4.txt", "Gleo AI App Schema  ·  음성 함수 3개 (구분 기록 · 메모 추가 · 이번 달 요약)"), 34, "0xA9BCCF", 636) + ","
    + dt("suit_medium.ttf", txt("o5.txt", "화면 속 운행·업체 정보는 예시입니다"), 28, "0x7F93A8", 800)
    + ",format=yuv420p[outro]")
labels.append("[outro]")

fc = ";\n".join(parts) + ";\n" + "".join(labels) + f"concat=n={len(labels)}:v=1:a=0[out]"
(D / "demo_filter.txt").write_text(fc, encoding="utf-8")

subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", RAW, "-/filter_complex", "demo_filter.txt",
                "-map", "[out]", "-c:v", "libx264", "-preset", "slow", "-crf", "22", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", "demo_1080p.mp4"], cwd=D, check=True)
print("ok")
