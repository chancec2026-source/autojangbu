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
  const head = [["상호(법인명)", v.company], ["사업자등록번호", v.bizNo], ["차종", v.carModel], ["자동차등록번호", v.plate]]
    .map(([k, val]) => `<div><span>${k}</span><b>${esc(val || "—")}</b></div>`).join("");
  const fmt = (v, d = 1) => Number(v || 0).toLocaleString("ko-KR", { minimumFractionDigits: d, maximumFractionDigits: d });
  const rows = l.rows.map((r) => `
      <tr>
        <td>${esc(r.date)}</td>
        <td>${esc(r.user || "")}</td>
        <td class="n">${fmt(r.startKm, 0)}</td><td class="n">${fmt(r.endKm, 0)}</td>
        <td class="n strong">${fmt(r.km)}</td>
        <td class="n">${fmt(r.commuteKm)}</td><td class="n">${fmt(r.businessKm)}</td>
        <td>${esc(r.note || "")}</td>
      </tr>`).join("");
  const body = `
    <p class="sub">${esc(who || "사용자")} · ${period} · 업무용 승용차 운행기록부</p>
    <div class="head">${head}</div>
    <div class="stats">
      <div><span>운행</span><b>${s.tripCount ?? l.rows.length}건</b></div>
      <div><span>총 주행</span><b>${fmt(s.totalKm, 0)}km</b></div>
      <div><span>업무 사용</span><b>${fmt(s.businessUseKm, 0)}km</b></div>
      <div class="hi"><span>업무사용비율</span><b>${fmt((s.businessUseRatio || 0) * 100, 0)}%</b></div>
    </div>
    ${s.unconfirmedCount ? `<p class="warn">구분을 확인하지 않은 운행 ${s.unconfirmedCount}건은 추천 구분으로 계산했어요.</p>` : ""}
    ${s.missingPurposeCount ? `<p class="warn">방문 목적이 비어 있는 업무 운행 ${s.missingPurposeCount}건이 있어요. 비고에 목적을 적어 두면 세무 증빙에 유리합니다.</p>` : ""}
    <div class="actions">
      <a class="btn" href="/l/${url.pathname.split("/")[2]}/csv">엑셀용 파일 내려받기(국세청 양식)</a>
      <button class="btn line" id="copy">세무사님께 보낼 링크 복사</button>
    </div>
    <div class="table">
      <table>
        <thead><tr><th>사용일자</th><th>사용자</th><th>주행 전 계기판</th><th>주행 후 계기판</th><th>주행거리</th><th>출·퇴근용</th><th>일반 업무용</th><th>비고</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    <p class="foot">거리는 차 계기판 값을 그대로 기록했습니다(단위 km). 이 링크는 ${new Date(Date.parse(l.createdAt) + TTL_SECONDS * 1000).toLocaleDateString("ko-KR")}까지 열립니다.</p>
    <script>
      document.getElementById("copy").onclick = async () => {
        try { await navigator.clipboard.writeText(location.href); document.getElementById("copy").textContent = "복사했어요"; }
        catch { prompt("아래 주소를 복사하세요", location.href); }
      };
    </script>`;
  return page(mo ? `${Number(mo)}월 운행기록부` : `${Number(y)}년 운행기록부`, body);
}

function page(title, body, status = 200) {
  const html = `<!doctype html>
<html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex">
<title>${esc(title)} · 오토장부</title>
<style>
  :root{--paper:#F1F3EF;--surface:#FBFCFA;--ink:#16201A;--muted:#56635B;--line:#D3D9D3;--sign:#0B6A45;--soft:#E2EEE7;--lane:#C98E00}
  *{box-sizing:border-box} body{margin:0;background:var(--paper);color:var(--ink);font:16px/1.6 -apple-system,"Apple SD Gothic Neo","Malgun Gothic","Noto Sans KR",sans-serif;word-break:keep-all;overflow-wrap:anywhere}
  main{max-width:960px;margin:0 auto;padding:24px 16px 48px;display:grid;gap:16px}
  h1{margin:0;color:var(--sign);font-size:28px} .sub{margin:0;color:var(--muted)}
  .stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px}
  .stats div{background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:12px 14px;display:grid}
  .stats span{color:var(--muted);font-size:14px} .stats b{font-size:22px;font-variant-numeric:tabular-nums}
  .stats .hi{background:var(--sign);border-color:var(--sign);color:#fff} .stats .hi span{color:var(--soft)}
  .warn{margin:0;color:var(--lane)}
  .head{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:4px 16px;font-size:14px}
  .head div{display:flex;gap:8px} .head span{color:var(--muted);min-width:96px} .head b{font-weight:600}
  .actions{display:flex;flex-wrap:wrap;gap:10px}
  .btn{display:inline-flex;align-items:center;min-height:48px;padding:0 18px;border-radius:10px;background:var(--sign);color:#fff;text-decoration:none;font:inherit;font-weight:700;border:1px solid var(--sign);cursor:pointer}
  .btn.line{background:var(--surface);color:var(--ink);border-color:var(--line)}
  .table{overflow-x:auto;background:var(--surface);border:1px solid var(--line);border-radius:12px}
  table{border-collapse:collapse;width:100%;min-width:720px;font-size:14px}
  th,td{padding:8px 10px;border-bottom:1px solid var(--line);text-align:left;white-space:nowrap}
  th{background:var(--soft);font-weight:700} td.n{text-align:right;font-variant-numeric:tabular-nums} td.strong{font-weight:700}
  .foot{color:var(--muted);font-size:13px;margin:0}
</style></head>
<body><main><h1>${esc(title)}</h1>${body}</main></body></html>`;
  return new Response(html, { status, headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-robots-tag": "noindex" } });
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json" } });
}

function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
