/* 시안별 앱 아이콘과 공용 선 아이콘. 시안 화면과 비교 보드가 함께 쓴다. */
(function () {
  const app = {
    // A 나이트 클러스터: 계기판 바늘
    a: `<svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg"><defs><linearGradient id="iaBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1C2C3F"/><stop offset="1" stop-color="#091018"/></linearGradient></defs><rect width="64" height="64" rx="16" fill="url(#iaBg)"/><rect x=".5" y=".5" width="63" height="63" rx="15.5" fill="none" stroke="#2B3C50"/><path d="M16.41 44 A18 18 0 1 1 47.59 44" fill="none" stroke="#2A3B4F" stroke-width="5" stroke-linecap="round"/><path d="M16.41 44 A18 18 0 0 1 45.38 22.96" fill="none" stroke="#61CDFF" stroke-width="5" stroke-linecap="round"/><path d="M32 35 L40.92 26.97" stroke="#fff" stroke-width="3.6" stroke-linecap="round"/><circle cx="32" cy="35" r="3.6" fill="#fff"/></svg>`,
    // B 결재 장부: 장부 줄 + 도장
    b: `<svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg"><rect width="64" height="64" rx="16" fill="#FFFFFF"/><rect x=".75" y=".75" width="62.5" height="62.5" rx="15.25" fill="none" stroke="#D3D9E2" stroke-width="1.5"/><path d="M14 19h36M14 28h36M14 37h20" stroke="#2346B0" stroke-width="3.2" stroke-linecap="round"/><g transform="rotate(-12 44 44)"><circle cx="44" cy="44" r="11.5" fill="none" stroke="#D1392B" stroke-width="3"/><path d="M38.5 41.5h11M44 41.5v8M39.5 49.5h9" stroke="#D1392B" stroke-width="2.6" stroke-linecap="round"/></g></svg>`,
    // C 빅 타입: 굵은 '오' — 가로획이 운행 띠처럼 끊어진다
    c: `<svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg"><rect width="64" height="64" rx="16" fill="#FF5A1F"/><circle cx="32" cy="24" r="9.5" fill="none" stroke="#fff" stroke-width="6"/><path d="M32 34v8" stroke="#fff" stroke-width="6" stroke-linecap="round"/><path d="M15 45h16M36 45h7M48 45h1" stroke="#fff" stroke-width="6" stroke-linecap="round"/></svg>`,
  };

  const P = {
    left: '<path d="M15 5l-7 7 7 7"/>',
    right: '<path d="M9 5l7 7-7 7"/>',
    mic: '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"/>',
    qr: '<rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><path d="M14 14h2v2h-2zM18 18h2v2h-2zM14 18h2M18 14h2"/>',
    down: '<path d="M12 4v11M7 10.5l5 5 5-5M5 20h14"/>',
    pencil: '<path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3zM14 7l3 3"/>',
    warn: '<path d="M12 3.8 2.6 19.6h18.8z"/><path d="M12 10v4.4M12 17.2v.3"/>',
    check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    phone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
    link: '<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>',
    file: '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5M9 13h6M9 17h6"/>',
    x: '<path d="M6 6l12 12M18 6 6 18"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4.5 20c1.4-3.6 4.2-5.5 7.5-5.5s6.1 1.9 7.5 5.5"/>',
    send: '<path d="M4 12 20 4l-5 16-3.5-6.5z"/><path d="M11.5 13.5 20 4"/>',
  };
  const icon = (name, size = 28, sw = 2) =>
    `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[name]}</svg>`;

  window.AJ_ICONS = { app, icon };
})();
