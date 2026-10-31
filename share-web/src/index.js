// 오토장부 공유 서버: 차에서 올린 월별 운행기록부를 휴대폰·세무사가 링크로 보고 내려받는다.
// - POST /api/ledgers      (헤더 X-App-Key) 장부 올리기 → { id, url }
// - GET  /l/:id            장부 보기(휴대폰 화면)
// - GET  /l/:id/csv        국세청 양식 CSV(엑셀용) 내려받기
// 링크는 추측할 수 없는 무작위 id이고, 저장은 60일 뒤 자동 삭제된다. 위치 정보는 받지 않는다.

const TTL_SECONDS = 60 * 24 * 60 * 60;
const MAX_BYTES = 300_000;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === "POST" && path === "/api/ledgers") return upload(request, env, url);

    const m = path.match(/^\/l\/([A-Za-z0-9_-]{20,40})(\/csv)?$/);
    if (request.method === "GET" && m) {
      const ledger = await env.LEDGERS.get(m[1], "json");
      if (!ledger) return page("링크를 찾을 수 없어요", "<p>만료되었거나 잘못된 주소예요. 차에서 다시 보내 주세요.</p>", 404);
      return m[2] ? csvResponse(ledger) : ledgerPage(ledger, url);
    }

    if (path === "/") return page("오토장부", "<p>차 계기판 거리로 업무용 승용차 운행기록부를 자동으로 만드는 차량 앱입니다.</p>");
    return page("페이지를 찾을 수 없어요", "", 404);
  },
};

async function upload(request, env, url) {
  if (!env.APP_KEY || request.headers.get("X-App-Key") !== env.APP_KEY) return json({ error: "unauthorized" }, 401);
  const text = await request.text();
  if (text.length > MAX_BYTES) return json({ error: "too_large" }, 413);
  let body;
  try { body = JSON.parse(text); } catch { return json({ error: "bad_json" }, 400); }
  // month: "2026-10"(한 달) 또는 "2026"(사업연도 전체)
  if (!/^\d{4}(-\d{2})?$/.test(body.month || "") || typeof body.csv !== "string" || !Array.isArray(body.rows)) {
    return json({ error: "bad_request" }, 400);
  }
  const id = randomId();
  const ledger = {
    month: body.month,
    driver: { name: String(body.driver?.name || ""), department: String(body.driver?.department || "") },
    vehicle: {
      company: String(body.vehicle?.company || ""), bizNo: String(body.vehicle?.bizNo || ""),
      carModel: String(body.vehicle?.carModel || ""), plate: String(body.vehicle?.plate || ""),
    },
    summary: body.summary || {},
    rows: body.rows.slice(0, 2000),
    csv: body.csv,
    createdAt: new Date().toISOString(),
  };
  await env.LEDGERS.put(id, JSON.stringify(ledger), { expirationTtl: TTL_SECONDS });
  return json({ id, url: `${url.origin}/l/${id}` });
}

function randomId() {
  const bytes = crypto.getRandomValues(new Uint8Array(18));
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function csvResponse(ledger) {
  const name = `운행기록부_${ledger.month}.csv`;
  return new Response(ledger.csv, {
    headers: {
      "content-type": "text/csv; charset=utf-8",
      "content-disposition": `attachment; filename="ledger_${ledger.month}.csv"; filename*=UTF-8''${encodeURIComponent(name)}`,
      "cache-control": "no-store",
      "x-robots-tag": "noindex",
    },
  });
}

function ledgerPage(l, url) {
  const [y, mo] = l.month.split("-");
  const period = mo ? `${Number(y)}년 ${Number(mo)}월` : `${Number(y)}년 전체`;
  const s = l.summary;
  const v = l.vehicle || {};
  const who = [l.driver.department, l.driver.name].filter(Boolean).join(" ");
  const fmt = (v, d = 1) => Number(v || 0).toLocaleString("ko-KR", { minimumFractionDigits: d, maximumFractionDigits: d });
  const sum = (k) => l.rows.reduce((a, r) => a + Number(r[k] || 0), 0);
  const total = Number(s.totalKm ?? sum("km"));
  const ratio = Math.min(1, Math.max(0, Number(s.businessUseRatio || 0)));
  const bizShare = total > 0 ? Math.min(ratio, sum("businessKm") / total) : 0;
  const head = [["상호(법인명)", v.company], ["사업자등록번호", v.bizNo], ["차종", v.carModel], ["자동차등록번호", v.plate]]
    .map(([k, val]) => `<div><span>${k}</span><b>${esc(val || "—")}</b></div>`).join("");
  // 국세청 양식처럼 해당 없는 칸은 비워 둔다.
  const rows = l.rows.map((r) => `
      <tr>
        <td>${esc(r.date)}</td>
        <td>${esc(r.user || "")}</td>
        <td class="n">${fmt(r.startKm)}</td><td class="n">${fmt(r.endKm)}</td>
        <td class="n strong">${fmt(r.km)}</td>
        <td class="n">${r.commuteKm ? fmt(r.commuteKm) : ""}</td><td class="n">${r.businessKm ? fmt(r.businessKm) : ""}</td>
        <td>${esc(r.note || "")}</td>
      </tr>`).join("");
  const warn = (text) => `<p class="warn">${ICON_WARN}<span>${text}</span></p>`;
  const body = `
    <p class="sub">${esc([who || "사용자", v.plate, v.company].filter(Boolean).join(" · "))} · ${period}</p>
    <section class="gauge">
      <div class="g-wrap">${gaugeSvg(ratio, bizShare)}
        <div class="g-c"><span>업무사용비율</span><b class="num">${fmt(ratio * 100, 0)}<small>%</small></b></div>
      </div>
      <p class="g-km num">업무 <b>${fmt(s.businessUseKm)}</b> / 전체 ${fmt(total)} km</p>
    </section>
    <div class="stats">
      <div><span>운행</span><b class="num">${s.tripCount ?? l.rows.length}<small>건</small></b></div>
      <div><span>총 주행</span><b class="num">${fmt(total, 0)}<small>km</small></b></div>
      <div><span>업무 사용</span><b class="num">${fmt(s.businessUseKm, 0)}<small>km</small></b></div>
    </div>
    ${s.unconfirmedCount ? warn(`구분을 확인하지 않은 운행 ${s.unconfirmedCount}건은 추천 구분으로 계산했어요.`) : ""}
    ${s.missingPurposeCount ? warn(`방문 목적이 비어 있는 업무 운행 ${s.missingPurposeCount}건이 있어요. 비고에 목적을 적어 두면 세무 증빙에 유리합니다.`) : ""}
    <div class="actions">
      <a class="btn primary" href="/l/${url.pathname.split("/")[2]}/csv">${ICON_DOWN}엑셀 파일 내려받기 (국세청 양식)</a>
      <button class="btn ghost" id="copy">${ICON_LINK}<span>세무사님께 보낼 링크 복사</span></button>
    </div>
    <div class="head">${head}</div>
    <div class="table">
      <table>
        <thead><tr><th>사용일자</th><th>사용자</th><th>주행 전 계기판</th><th>주행 후 계기판</th><th>주행거리</th><th>출·퇴근용</th><th>일반 업무용</th><th>비고</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    <p class="foot">거리는 차 계기판 값을 그대로 기록했습니다(단위 km). 이 링크는 ${new Date(Date.parse(l.createdAt) + TTL_SECONDS * 1000).toLocaleDateString("ko-KR")}까지 열립니다.</p>
    <script>
      document.getElementById("copy").onclick = async () => {
        const label = document.querySelector("#copy span");
        try { await navigator.clipboard.writeText(location.href); label.textContent = "복사했어요"; }
        catch { prompt("아래 주소를 복사하세요", location.href); }
      };
    </script>`;
  return page(mo ? `${Number(mo)}월 운행기록부` : `${Number(y)}년 운행기록부`, body, 200, "공유된 장부");
}

// 차 앱과 같은 계기판(시안 A): 240° 눈금 위 일반 업무(하늘색)·출퇴근(파랑), 바늘이 업무사용비율.
function gaugeSvg(ratio, bizShare) {
  const W = 340, H = 196, cx = 170, cy = 112, R = 84, w = 11;
  const pt = (v, r) => {
    const a = ((150 + 240 * v) * Math.PI) / 180;
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  };
  const arc = (v0, v1) => {
    const [x0, y0] = pt(v0, R), [x1, y1] = pt(v1, R);
    return `M${x0.toFixed(1)} ${y0.toFixed(1)}A${R} ${R} 0 ${(v1 - v0) * 240 > 180 ? 1 : 0} 1 ${x1.toFixed(1)} ${y1.toFixed(1)}`;
  };
  let ticks = "";
  for (let i = 0; i <= 50; i++) {
    const v = i / 50, major = i % 5 === 0, lit = v <= ratio + 1e-9;
    const [x0, y0] = pt(v, R + 10), [x1, y1] = pt(v, R + (major ? 19 : 14));
    const c = lit ? (major ? "#B9CADB" : "#5E7489") : (major ? "#3B4A5C" : "#253242");
    ticks += `<line x1="${x0.toFixed(1)}" y1="${y0.toFixed(1)}" x2="${x1.toFixed(1)}" y2="${y1.toFixed(1)}" stroke="${c}" stroke-width="${major ? 2 : 1.4}" stroke-linecap="round"/>`;
  }
  const biz = bizShare > 0.001 ? arc(0.0001, bizShare) : "";
  const com = ratio - bizShare > 0.001 ? arc(bizShare, ratio) : "";
  const [n0x, n0y] = pt(ratio, R - 10), [n1x, n1y] = pt(ratio, R + 10);
  const needle = `x1="${n0x.toFixed(1)}" y1="${n0y.toFixed(1)}" x2="${n1x.toFixed(1)}" y2="${n1y.toFixed(1)}"`;
  return `<svg viewBox="0 0 ${W} ${H}" aria-hidden="true">
    <defs><filter id="glow" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="4"/></filter></defs>
    ${ticks}
    <path d="${arc(0, 1)}" fill="none" stroke="#18232F" stroke-width="${w}" stroke-linecap="round"/>
    ${ratio < 0.999 ? `<path d="${arc(ratio, 1)}" fill="none" stroke="#33445A" stroke-width="${w}"/>` : ""}
    <g filter="url(#glow)" opacity=".55">
      ${biz ? `<path d="${biz}" fill="none" stroke="#61CDFF" stroke-width="${w}"/>` : ""}
      ${com ? `<path d="${com}" fill="none" stroke="#3A80E4" stroke-width="${w}"/>` : ""}
      <line ${needle} stroke="#fff" stroke-width="6" stroke-linecap="round"/>
    </g>
    ${biz ? `<path d="${biz}" fill="none" stroke="#61CDFF" stroke-width="${w}" stroke-linecap="round"/>` : ""}
    ${com ? `<path d="${com}" fill="none" stroke="#3A80E4" stroke-width="${w}"/>` : ""}
    <line ${needle} stroke="#fff" stroke-width="3.5" stroke-linecap="round"/>
  </svg>`;
}

const ICON_APP = `<svg viewBox="0 0 64 64" aria-hidden="true"><defs><linearGradient id="ia" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1C2C3F"/><stop offset="1" stop-color="#091018"/></linearGradient></defs><rect width="64" height="64" rx="16" fill="url(#ia)"/><rect x=".5" y=".5" width="63" height="63" rx="15.5" fill="none" stroke="#2B3C50"/><path d="M16.41 44A18 18 0 1 1 47.59 44" fill="none" stroke="#2A3B4F" stroke-width="5" stroke-linecap="round"/><path d="M16.41 44A18 18 0 0 1 45.38 22.96" fill="none" stroke="#61CDFF" stroke-width="5" stroke-linecap="round"/><path d="M32 35L40.92 26.97" stroke="#fff" stroke-width="3.6" stroke-linecap="round"/><circle cx="32" cy="35" r="3.6" fill="#fff"/></svg>`;
const ico = (d) => `<svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${d}</svg>`;
const ICON_DOWN = ico('<path d="M12 4v11M7 10.5l5 5 5-5M5 20h14"/>');
const ICON_LINK = ico('<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>');
const ICON_WARN = ico('<path d="M12 3.8 2.6 19.6h18.8z"/><path d="M12 10v4.4M12 17.2v.3"/>');

function page(title, body, status = 200, tag = "") {
  const html = `<!doctype html>
<html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex">
<meta name="theme-color" content="#0A1119">
<title>${esc(title)} · 오토장부</title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/sun-typeface/SUIT@2/fonts/variable/woff2/SUIT-Variable.css">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Barlow+Semi+Condensed:wght@500;600&display=swap">
<style>
  :root{--bg:#0A1119;--panel:#101A25;--raised:#16222F;--line:#1C2835;--line2:#283748;--text:#EAF0F6;--sub:#93A1B2;--faint:#5C6A7B;--biz:#61CDFF;--amber:#F2B54A;
    --kr:"SUIT Variable","Apple SD Gothic Neo","Malgun Gothic","Noto Sans KR",sans-serif;--num:"Barlow Semi Condensed","SUIT Variable",sans-serif}
  *{box-sizing:border-box}
  html{background:var(--bg)}
  body{margin:0;min-height:100vh;color:var(--text);font:16px/1.6 var(--kr);word-break:keep-all;overflow-wrap:anywhere;-webkit-font-smoothing:antialiased;
    background:radial-gradient(640px 380px at 8% -12%,#16263A 0%,rgba(22,38,58,0) 70%),var(--bg)}
  main{max-width:960px;margin:0 auto;padding:22px 16px 48px;display:grid;gap:14px}
  .brand{display:flex;align-items:center;gap:10px;font-size:16px;font-weight:800}
  .brand svg{width:30px;height:30px;flex:none}
  .brand .tag{margin-left:auto;font-size:13px;font-weight:600;color:var(--sub);padding:4px 10px;border:1px solid var(--line2);border-radius:999px}
  h1{margin:10px 0 0;font-size:28px;font-weight:800;letter-spacing:-.02em;line-height:1.3;text-wrap:balance}
  p{text-wrap:pretty}
  .sub{margin:0;color:var(--sub);font-size:14px}
  .num,.n{font-family:var(--num);font-variant-numeric:tabular-nums}
  .ic{width:20px;height:20px;flex:none}
  .gauge{border-radius:22px;background:linear-gradient(180deg,#111C28,#0C151F);border:1px solid var(--line);padding:10px 12px 16px;text-align:center}
  .g-wrap{position:relative;max-width:340px;margin:0 auto}
  .g-wrap svg{display:block;width:100%}
  .g-c{position:absolute;left:0;right:0;top:27%;display:grid;justify-items:center}
  .g-c span{font-size:13px;font-weight:700;color:var(--sub)}
  .g-c b{font-size:72px;font-weight:600;line-height:.95}
  .g-c small{font-size:28px;font-weight:500;color:var(--sub);margin-left:2px}
  .g-km{margin:-6px 0 0;font-size:15px;color:var(--sub)} .g-km b{color:var(--text);font-weight:600}
  .stats{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}
  .stats div{background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:10px 12px;display:grid}
  .stats span{color:var(--sub);font-size:12px;font-weight:700}
  .stats b{font-size:24px;font-weight:600;line-height:1.2}
  .stats small{font:700 12px var(--kr);color:var(--sub);margin-left:2px}
  .warn{margin:0;color:var(--amber);font-size:14px;font-weight:600;display:flex;gap:8px;align-items:flex-start}
  .warn .ic{width:18px;height:18px;margin-top:2px}
  .actions{display:grid;gap:8px}
  .btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:52px;padding:0 18px;border-radius:14px;font:inherit;font-size:16px;font-weight:700;text-decoration:none;cursor:pointer}
  .btn.primary{background:var(--text);color:var(--bg);border:1px solid var(--text)}
  .btn.ghost{background:transparent;color:var(--sub);border:1px solid var(--line2)}
  .btn:focus-visible{outline:3px solid var(--biz);outline-offset:2px}
  .head{display:grid;grid-template-columns:repeat(2,1fr);border:1px solid var(--line);border-radius:14px;overflow:hidden;background:var(--panel)}
  .head div{padding:9px 12px;border-top:1px solid var(--line);display:grid}
  .head div:nth-child(-n+2){border-top:0} .head div:nth-child(even){border-left:1px solid var(--line)}
  .head span{color:var(--sub);font-size:12px;font-weight:600} .head b{font-weight:600;font-size:15px}
  .table{overflow-x:auto;background:var(--panel);border:1px solid var(--line);border-radius:16px}
  table{border-collapse:collapse;width:100%;min-width:760px;font-size:14px}
  th,td{padding:10px 12px;border-bottom:1px solid var(--line);text-align:left;white-space:nowrap}
  th{position:sticky;top:0;background:#0D1620;color:var(--sub);font-size:13px;font-weight:700}
  td.n{text-align:right;font-size:15px} td.strong{font-weight:600;color:var(--text)}
  tbody tr:last-child td{border-bottom:0}
  .foot{color:var(--faint);font-size:13px;margin:0}
  @media (min-width:720px){
    h1{font-size:34px}
    .actions{grid-template-columns:auto auto;justify-content:start}
    .head{grid-template-columns:repeat(4,1fr)} .head div{border-top:0!important;border-left:1px solid var(--line)} .head div:first-child{border-left:0}
  }
  @media print{
    html,body{background:#fff!important;color:#000}
    .brand .tag,.actions{display:none}
    .gauge,.stats div,.head,.table,th{background:#fff!important;border-color:#bbb}
    .sub,.g-c span,.g-km,.stats span,.head span,th,.foot{color:#444}
    td.strong,.g-km b{color:#000}
  }
</style></head>
<body><main><div class="brand">${ICON_APP}오토장부${tag ? `<span class="tag">${esc(tag)}</span>` : ""}</div><h1>${esc(title)}</h1>${body}</main></body></html>`;
  return new Response(html, { status, headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-robots-tag": "noindex" } });
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json" } });
}

function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
