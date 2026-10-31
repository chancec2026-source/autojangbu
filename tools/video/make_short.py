# 숏폼(세로 1080x1920, 30초~1분): 원본 녹화(raw.mp4)에서 장면을 골라 큰 자막과 함께 세로로 배치한다.
# 사용: python tools/video/make_short.py <작업폴더>  (make_demo.py 를 먼저 돌려 글꼴이 작업폴더에 있어야 한다)
import pathlib, subprocess, sys

D = pathlib.Path(sys.argv[1])
RAW = "raw.mp4"
BG = "0x0A0F16"
# (원본 시작, 원본 끝, 배속, 화면: app=앱 부분만 / full=차 화면 전체, 자막 두 줄)
SEG = [
    (0.3, 4.3, 1, "app", "업무용 차 운행기록부,\n아직도 손으로 쓰세요?"),
    (5.3, 26.3, 4, "full", "시동 걸고 출발하면\n계기판 거리를 자동 기록"),
    (27.5, 34.5, 1, "app", "주차하고 문을 열면\n운행 1건 완성"),
    (35.2, 41.2, 1, "app", "“오토장부, 업무로 기록해.\n성수동 거래처 미팅”"),
    (43.0, 50.0, 1, "app", "월말 결산도\n알아서"),
    (82.5, 90.5, 1, "app", "QR 한 번이면 휴대폰으로,\n세무사님께 링크로 끝"),
]
END = 4.5

def txt(name, s):
    (D / name).write_text(s, encoding="utf-8")
    return name

def dt(font, file, size, color, y, extra=""):
    return (f"drawtext=fontfile={font}:textfile={file}:fontsize={size}:fontcolor={color}"
            f":line_spacing=18:text_align=C:x=(w-text_w)/2:y={y}{extra}")

FOOT = (dt("suit_bold.ttf", txt("sf1.txt", "차에 이미 있는 숫자로, 오토장부"), 46, "white", 1590) + ","
        + dt("suit_medium.ttf", txt("sf2.txt", "Pleos Connect 차량용 앱 · 화면 속 운행은 예시입니다"), 28, "0x8FA3B8", 1670))

n = len(SEG) + 1
parts = [f"[0:v]split={n}" + "".join(f"[r{k}]" for k in range(n))]
labels = []
for i, (a, b, sp, view, cap) in enumerate(SEG):
    pts = f"(PTS-STARTPTS)/{sp}" if sp != 1 else "PTS-STARTPTS"
    if view == "app":
        fit = "crop=1280:1080:640:0,scale=1080:-2"
    else:
        fit = "scale=1080:-2"
    parts.append(
        f"[r{i}]trim={a}:{b},setpts={pts},fps=30,{fit},"
        f"pad=1080:1920:0:(1920-ih)/2+40:color={BG},"
        f"drawbox=x=0:y=0:w=iw:h=8:color=0x3FB6FF:t=fill,"
        + ",".join(dt("suit_extrabold.ttf", txt(f"sc{i}_{j}.txt", line), 72, "white", 290 + j * 100)
                   for j, line in enumerate(cap.split("\n"))) + ","
        + FOOT + f",format=yuv420p[v{i}]")
    labels.append(f"[v{i}]")

k = len(SEG)
parts.append(
    f"[r{k}]trim=93:93.1,setpts=PTS-STARTPTS,loop=loop=-1:size=1:start=0,trim=0:{END},setpts=PTS-STARTPTS,"
    f"fps=30,crop=1280:1080:640:0,scale=1080:-2,boxblur=16:2,colorchannelmixer=rr=0.3:gg=0.3:bb=0.3,"
    f"pad=1080:1920:0:(1920-ih)/2+40:color={BG},"
    + dt("suit_bold.ttf", txt("se1.txt", "단말기도, 휴대폰 앱도 없이"), 58, "white", 760) + ","
    + dt("suit_extrabold.ttf", txt("se2.txt", "차에 이미 있는 숫자로"), 88, "white", 860) + ","
    + dt("suit_extrabold.ttf", txt("se2b.txt", "오토장부"), 88, "0x3FB6FF", 980) + ","
    + dt("suit_medium.ttf", txt("se3.txt", "Pleos Connect 차량용 앱"), 34, "0x8FA3B8", 1160)
    + f",format=yuv420p[v{k}]")
labels.append(f"[v{k}]")

fc = ";\n".join(parts) + ";\n" + "".join(labels) + f"concat=n={len(labels)}:v=1:a=0[out]"
(D / "short_filter.txt").write_text(fc, encoding="utf-8")
subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", RAW, "-/filter_complex", "short_filter.txt",
                "-map", "[out]", "-c:v", "libx264", "-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", "short_vertical.mp4"], cwd=D, check=True)
print("ok")
