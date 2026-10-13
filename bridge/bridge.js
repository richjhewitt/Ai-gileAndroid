// Phone bridge (AII-12): lets the Messenger app on Rich's phone reach the AI-gile board on this PC.
// The board only answers to localhost, so the phone talks to this bridge instead:
// - every call needs "Authorization: Bearer <phone key>"; too many wrong keys lock the bridge out for a while
// - only the routes the Messenger uses get through (see ROUTES); everything else is 404
// - calls go on to the board as if from localhost, with no cookies, Origin or key attached
// HTTPS from the phone comes from Tailscale serve in front of this port (see README.md).
// No dependencies: run with `node bridge/bridge.js`.
'use strict';
const http = require('http');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

/* ---------- routes ---------- */
const SEG = '[A-Za-z0-9_-]+';
const ROUTES = [
  ['GET', '/api/meta'],
  ['GET', '/api/chats'],
  ['GET', `/api/chats/${SEG}`],
  ['POST', '/api/chats/_watchman'],
  ['GET', '/api/dms/contacts'],
  ['GET', `/api/dms/contacts/${SEG}/messages`],
  ['POST', `/api/dms/contacts/${SEG}/messages`],
  ['POST', `/api/projects/${SEG}/cards`],
  ['POST', `/api/projects/${SEG}/cards/${SEG}/comments`],
  ['GET', `/api/avatars/(badge(/${SEG})+|[A-Za-z0-9_.-]+)`],
  ['GET', `/api/projects/${SEG}/logo`],
].map(([method, p]) => ({ method, re: new RegExp(`^${p}$`) }));

function allowed(method, pathname) {
  if (pathname.includes('..') || pathname.includes('//')) return false;
  return ROUTES.some(r => r.method === method && r.re.test(pathname));
}

/* ---------- key ---------- */
function sameKey(given, key) {
  const a = Buffer.from(String(given)), b = Buffer.from(key);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function loadKey(file) {
  try { const k = fs.readFileSync(file, 'utf8').trim(); if (k.length >= 32) return k; } catch {}
  const k = crypto.randomBytes(32).toString('base64url');
  fs.writeFileSync(file, k + '\n', { mode: 0o600 });
  return k;
}

/* ---------- bridge ---------- */
const MAX_BODY = 64 * 1024;

function createBridge({ key, boardPort = 4321, boardHost = '127.0.0.1', maxFails = 10, lockMs = 15 * 60 * 1000, now = Date.now }) {
  if (!key || key.length < 32) throw new Error('phone key must be at least 32 characters');
  let fails = [], lockedUntil = 0;

  function send(res, code, obj) {
    res.writeHead(code, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
    res.end(JSON.stringify(obj));
  }

  return http.createServer((req, res) => {
    const t = now();
    if (t < lockedUntil) return send(res, 429, { error: 'too many wrong keys; try again later' });
    const auth = String(req.headers.authorization || '');
    const given = auth.startsWith('Bearer ') ? auth.slice(7).trim() : '';
    if (!sameKey(given, key)) {
      fails = fails.filter(f => t - f < 60 * 1000).concat(t);
      if (fails.length >= maxFails) { lockedUntil = t + lockMs; fails = []; }
      return send(res, 401, { error: 'wrong phone key' });
    }
    fails = [];

    let url;
    try { url = new URL(req.url, 'http://bridge'); } catch { return send(res, 400, { error: 'bad address' }); }
    if (!allowed(req.method, url.pathname)) return send(res, 404, { error: 'not available from the phone' });

    const chunks = []; let size = 0;
    req.on('data', c => { size += c.length; if (size > MAX_BODY) { send(res, 413, { error: 'message too long' }); req.destroy(); } else chunks.push(c); });
    req.on('end', () => {
      if (res.headersSent) return;
      const body = Buffer.concat(chunks);
      const headers = { Host: `localhost:${boardPort}`, Accept: req.headers.accept || 'application/json' };
      if (req.method !== 'GET') { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = body.length; }
      const up = http.request({ host: boardHost, port: boardPort, method: req.method, path: url.pathname + url.search, headers }, r => {
        const out = { 'Cache-Control': 'no-store' };
        if (r.headers['content-type']) out['Content-Type'] = r.headers['content-type'];
        res.writeHead(r.statusCode, out);
        r.pipe(res);
      });
      up.on('error', () => { if (!res.headersSent) send(res, 502, { error: 'the board is not running on the PC' }); });
      up.setTimeout(20000, () => up.destroy());
      up.end(req.method === 'GET' ? undefined : body);
    });
  });
}

/* ---------- command line ---------- */
if (require.main === module) {
  const port = Number(process.env.BRIDGE_PORT || 4322);
  const host = process.env.BRIDGE_HOST || '127.0.0.1';
  const boardPort = Number(process.env.AIGILE_PORT || 4321);
  const key = loadKey(path.join(__dirname, 'key.local.txt'));
  createBridge({ key, boardPort }).listen(port, host, () => {
    const url = process.env.BRIDGE_URL || `http://${host}:${port}`;
    console.log(`Phone bridge on http://${host}:${port} -> board on port ${boardPort}`);
    console.log(`Pairing link: aigile://pair?url=${encodeURIComponent(url)}&key=${encodeURIComponent(key)}`);
  });
}

module.exports = { createBridge, allowed, loadKey };
