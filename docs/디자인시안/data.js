/* 오토장부 디자인 시안 — 세 시안이 함께 쓰는 예시 데이터.
   2026년 10월. '지금'은 10월 30일(금) 14:41, 방금 주차해서 분류를 기다리는 운행이 1건 있다.
   거리는 0.1km 단위 정수(km10)로 더해 소수 오차를 없앤다. */
(function () {
  const WD = ["일", "월", "화", "수", "목", "금", "토"];
  const DAYS = 31, TODAY = 30;
  const dow = (d) => (3 + d) % 7; // 2026-10-01 = 목
  const holidays = { 3: "개천절", 5: "대체공휴일", 9: "한글날" };
  const profile = { company: "주식회사 찬스", bizNo: "123-45-67890", carModel: "아이오닉 5", plate: "12가 3456", name: "대표" };
  const SHARE_URL = "https://chance-autojangbu.chancec2026.workers.dev/l/Xk3vQ9pLm2RtY7wZbN4sJ8aH";

  const toMin = (s) => +s.slice(0, 2) * 60 + +s.slice(3);
  const hhmm = (m) => String(Math.floor(m / 60)).padStart(2, "0") + ":" + String(m % 60).padStart(2, "0");

  const work = [1, 2, 6, 7, 8, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 26, 27, 28, 29, 30];
  const biz = [
    [1, "10:20", "11:05", 214, "강남 OO아파트 현장"],
    [1, "14:00", "14:30", 98, "역삼 세무사 사무실"],
    [2, "13:10", "14:20", 386, "판교 자재 업체"],
    [6, "09:40", "10:50", 462, "인천 물류창고 납품", "이수민"],
    [7, "10:30", "11:31", 280, "강남 OO아파트 현장"],
    [7, "15:10", "15:42", 140, "성수동 거래처 미팅"],
    [8, "10:05", "10:40", 164, ""],
    [12, "11:00", "12:10", 418, "수원 협력사 방문"],
    [13, "10:30", "11:31", 280, "강남 OO아파트 현장"],
    [14, "14:20", "14:55", 132, "을지로 인쇄소 시안 확인"],
    [15, "09:50", "10:40", 316, "분당 고객사 A/S", "이수민"],
    [16, "16:00", "16:25", 88, ""],
    [19, "10:30", "12:06", 440, "판교 자재 업체"],
    [20, "10:30", "11:31", 280, "강남 OO아파트 현장"],
    [21, "13:30", "13:56", 120, "역삼 세무사 사무실"],
    [22, "13:23", "14:02", 140, "성수동 거래처 미팅"],
    [23, "10:10", "11:20", 334, "광명 현장 실측", "이수민"],
    [26, "11:15", "11:50", 156, "용산 전자상가 부품 구매"],
    [27, "10:30", "11:31", 280, "강남 OO아파트 현장"],
    [28, "15:00", "15:38", 140, "성수동 거래처 미팅"],
    [29, "09:30", "10:45", 462, "인천 물류창고 납품", "이수민"],
  ];
  const personal = [[3, "11:20", "12:05", 264], [10, "15:30", "16:02", 126], [18, "10:00", "11:10", 348], [25, "13:40", "14:15", 96]];

  const trips = [];
  const jit = [0, 2, -1, 1, 0, -2, 1];
  const inS = ["08:14", "08:20", "08:17", "08:22", "08:12"], outS = ["18:40", "18:52", "19:05", "18:35", "18:47"];
  work.forEach((d, i) => {
    const a = toMin(inS[i % 5]);
    trips.push({ day: d, start: hhmm(a), end: hhmm(a + 26 + (i % 4)), km10: 124 + jit[i % 7], cat: "commute", dir: "in" });
    if (d !== TODAY) {
      const b = toMin(outS[i % 5]);
      trips.push({ day: d, start: hhmm(b), end: hhmm(b + 27 + (i % 3)), km10: 124 + jit[(i + 3) % 7], cat: "commute", dir: "out" });
    }
  });
  biz.forEach(([day, start, end, km10, memo, driver]) => trips.push({ day, start, end, km10, cat: "business", memo, driver }));
  personal.forEach(([day, start, end, km10]) => trips.push({ day, start, end, km10, cat: "personal" }));
  trips.push({ day: TODAY, start: "14:05", end: "14:41", km10: 236, cat: "business", pending: true });
  trips.forEach((t) => { t.driver = t.driver || "대표"; t.memo = t.memo || ""; t.km = t.km10 / 10; });
  trips.sort((x, y) => x.day - y.day || toMin(x.start) - toMin(y.start));

  const total10 = trips.reduce((a, t) => a + t.km10, 0);
  let odo = 124920 - total10; // 지금 계기판 12,492.0km 에서 거꾸로 맞춘다
  const monthStartOdo = odo / 10;
  trips.forEach((t, i) => { t.id = i + 1; t.startOdo = odo / 10; odo += t.km10; t.endOdo = odo / 10; });

  const sum10 = (f) => trips.filter(f).reduce((a, t) => a + t.km10, 0);
  const s = {
    count: trips.length,
    total: total10 / 10,
    business: sum10((t) => t.cat === "business") / 10,
    commute: sum10((t) => t.cat === "commute") / 10,
    personal: sum10((t) => t.cat === "personal") / 10,
    businessUse: sum10((t) => t.cat !== "personal") / 10,
  };
  s.ratio = s.businessUse / s.total;
  s.ratioPct = Math.round(s.ratio * 100);
  s.unconfirmed = trips.filter((t) => t.pending).length;
  s.missingPurpose = trips.filter((t) => t.cat === "business" && !t.memo).length;
  s.commuteDays = new Set(trips.filter((t) => t.cat === "commute").map((t) => t.day)).size;
  s.longest = trips.reduce((a, b) => (b.km10 > a.km10 ? b : a));
  const places = {};
  trips.forEach((t) => { if (t.memo) places[t.memo] = (places[t.memo] || 0) + 1; });
  s.places = Object.entries(places).sort((a, b) => b[1] - a[1]).slice(0, 3);
  const drivers = {};
  trips.forEach((t) => (drivers[t.driver] = (drivers[t.driver] || 0) + 1));
  s.drivers = Object.entries(drivers).sort((a, b) => b[1] - a[1]);
  s.avgPerDay = s.total / TODAY;

  const daily = Array.from({ length: DAYS }, (_, i) => ({ day: i + 1, business: 0, commute: 0, personal: 0, total: 0 }));
  trips.forEach((t) => { const d = daily[t.day - 1]; d[t.cat] += t.km; d.total += t.km; });

  // 목록용: 날짜 내림차순, 하루 안에서는 늦은 운행이 위로
  const groups = [];
  [...trips].reverse().forEach((t) => {
    let g = groups[groups.length - 1];
    if (!g || g.day !== t.day) groups.push((g = { day: t.day, trips: [], km10: 0 }));
    g.trips.push(t);
    g.km10 += t.km10;
  });
  groups.forEach((g) => (g.km = g.km10 / 10));

  const nf1 = new Intl.NumberFormat("ko-KR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  const nf0 = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
  const f1 = (v) => nf1.format(v), f0 = (v) => nf0.format(v);
  const dayLabel = (d) => `10.${d} ${WD[dow(d)]}`;
  const longDay = (d) => `10월 ${d}일 ${WD[dow(d)]}요일`;
  const CAT = { business: "업무", commute: "출퇴근", personal: "개인" };
  const title = (t) =>
    t.pending ? "분류를 기다리는 운행" : t.cat === "commute" ? (t.dir === "in" ? "출근" : "퇴근") : t.cat === "personal" ? "개인 운행" : t.memo;

  // QR 코드(SVG). qrcode-generator 가 있으면 진짜 QR 을 그린다.
  function qrSVG(color = "#000") {
    if (!window.qrcode) return "";
    const q = window.qrcode(0, "M");
    q.addData(SHARE_URL);
    q.make();
    const n = q.getModuleCount();
    let d = "";
    for (let r = 0; r < n; r++) for (let c = 0; c < n; c++) if (q.isDark(r, c)) d += `M${c} ${r}h1v1h-1z`;
    return `<svg viewBox="-2 -2 ${n + 4} ${n + 4}" shape-rendering="crispEdges" width="100%" height="100%"><path d="${d}" fill="${color}"/></svg>`;
  }

  // 원호 경로: v(0~1)를 각도 start~start+sweep(도, 시계 방향)로 바꾼다
  function arcPath(cx, cy, r, v0, v1, start = 150, sweep = 240) {
    const pt = (v) => {
      const a = ((start + sweep * v) * Math.PI) / 180;
      return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
    };
    const [x0, y0] = pt(v0), [x1, y1] = pt(v1);
    const large = (v1 - v0) * sweep > 180 ? 1 : 0;
    return `M${x0.toFixed(2)} ${y0.toFixed(2)} A${r} ${r} 0 ${large} 1 ${x1.toFixed(2)} ${y1.toFixed(2)}`;
  }
  function polar(cx, cy, r, v, start = 150, sweep = 240) {
    const a = ((start + sweep * v) * Math.PI) / 180;
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  }

  window.AJ = {
    WD, DAYS, TODAY, dow, holidays, profile, SHARE_URL, trips, s, daily, groups, monthStartOdo,
    pending: trips.find((t) => t.pending), f1, f0, dayLabel, longDay, CAT, title, qrSVG, arcPath, polar,
    expiry: "12월 29일", view: (location.hash || "#main").slice(1),
  };
})();
