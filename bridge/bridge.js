// Phone bridge (AII-13): lets the Messenger app on Rich's phone reach the AI-gile board on this PC.
// Zero dependencies. The board only answers to localhost, so the phone talks to this bridge and the bridge
// passes the Messenger's own routes on to the board. Everything else is 404.
// Run:  node bridge/bridge.js          -> listens on 127.0.0.1:4330 (Tailscale serve puts HTTPS in front)
//       node bridge/bridge.js pair     -> prints the pairing link for the app (aigile://pair?url=...&key=...)
//       node bridge/bridge.js new-key  -> makes a new phone key (the old phone has to pair again)
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

/* ---------- settings ---------- */
const PORT = +process.env.BRIDGE_PORT || 4330;
const BIND = process.env.BRIDGE_HOST || '127.0.0.1';
const BOARD_PORT = +process.env.AIGILE_PORT || 4321;
const KEY_FILE = process.env.BRIDGE_KEY_FILE || path.join(__dirname, 'phone-key.local.json');
const MAX_BODY = 256 * 1024;

/* ---------- phone key ---------- */
// One key, made on first run and kept next to this file (*.local.* stays out of git).
function readKey(file = KEY_FILE) {
  try { const k = JSON.parse(fs.readFileSync(file, 'utf8')).key; if (typeof k === 'string' && k.length >= 32) return k; } catch {}
  return null;
}
function newKey(file = KEY_FILE) {
  const key = crypto.randomBytes(32).toString('base64url');
  fs.writeFileSync(file, JSON.stringify({ key, made: new Date().toISOString() }, null, 2) + '\n', { mode: 0o600 });
  return key;
}
const keyOf = (file = KEY_FILE) => readKey(file) || newKey(file);

function keyMatches(header, key) {
  const m = /^Bearer\s+(\S+)$/i.exec(String(header || ''));
  if (!m) return false;
  const a = crypto.createHash('sha256').update(m[1]).digest(), b = crypto.createHash('sha256').update(key).digest();
  return crypto.timingSafeEqual(a, b);
}

/* ---------- what the phone may call ---------- */
// The Messenger's routes (from the AII lead, see AIG-907), plus the pictures the chats show.
// Path parts are one plain segment each: no slashes, no "..", once decoded.
const SEG = '([^/]+)';
const ALLOWED = [
  ['GET', '/api/meta'],
  ['GET', '/api/chats'],
  ['GET', `/api/chats/${SEG}`],
  ['POST', '/api/chats/_watchman'],
  ['GET', '/api/dms/contacts'],
  ['GET', `/api/dms/contacts/${SEG}/messages`],
  ['POST', `/api/dms/contacts/${SEG}/messages`],
  ['POST', `/api/projects/${SEG}/cards`],
  ['POST', `/api/projects/${SEG}/cards/${SEG}/comments`],
  ['GET', `/api/projects/${SEG}/logo`],
  ['GET', '/api/app-logo'],
  ['GET', '/api/me-picture'],
  ['GET', `/api/avatars/badge/${SEG}/${SEG}/${SEG}`],
  ['GET', `/api/avatars/${SEG}`],
].map(([m, p]) => [m, new RegExp('^' + p + '$')]);

function safeSeg(s) {
  let d; try { d = decodeURIComponent(s); } catch { return false; }
  return d !== '' && d !== '.' && d !== '..' && !/[\/\\\0]/.test(d);
}
function allowed(method, pathname) {
  for (const [m, re] of ALLOWED) {
    const hit = re.exec(pathname);
    if (hit && m === method) return hit.slice(1).every(safeSeg);
  }
  return false;
}

/* ---------- wrong-key brake ---------- */
// 10 wrong keys in a minute from one address: that address waits out the minute.
const misses = new Map();   // ip -> [first miss ms, count]
function braked(ip, now = Date.now()) {
  const m = misses.get(ip);
  return !!m && now - m[0] < 60e3 && m[1] >= 10;
}
function miss(ip, now = Date.now()) {
  const m = misses.get(ip);
  if (!m || now - m[0] >= 60e3) misses.set(ip, [now, 1]); else m[1]++;
  if (misses.size > 1000) for (const [k, v] of misses) if (now - v[0] >= 60e3) misses.delete(k);
}

/* ---------- the bridge ---------- */
function reply(res, code, error) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify({ error }));
}

function createBridge({ key, boardPort = BOARD_PORT } = {}) {
  key = key || keyOf();
  return http.createServer((req, res) => {
    // Tailscale serve proxies from this PC, so the phone's own address comes in X-Forwarded-For (trusted from this PC only).
    const near = req.socket.remoteAddress, local = /^(127\.|::1$|::ffff:127\.)/.test(near || '');
    const ip = (local && String(req.headers['x-forwarded-for'] || '').split(',')[0].trim()) || near;
    if (braked(ip)) return reply(res, 429, 'too many wrong keys, wait a minute');
    if (!keyMatches(req.headers.authorization, key)) { miss(ip); return reply(res, 401, 'wrong or missing phone key'); }

    const url = new URL(req.url, 'http://x');
    if (!allowed(req.method, url.pathname)) return reply(res, 404, 'not available from the phone');

    // Only what the board needs: it checks Host (localhost) and JSON; no Origin, cookies or the phone key go on.
    const headers = { host: `localhost:${boardPort}`, accept: req.headers.accept || '*/*' };
    if (req.method !== 'GET') headers['content-type'] = 'application/json';

    let size = 0;
    const up = http.request({ host: '127.0.0.1', port: boardPort, method: req.method, path: url.pathname + url.search, headers }, r => {
      const out = { 'Cache-Control': 'no-store' };
      for (const h of ['content-type', 'content-length', 'last-modified', 'etag']) if (r.headers[h]) out[h] = r.headers[h];
      res.writeHead(r.statusCode, out);
      r.pipe(res);
    });
    up.on('error', () => { if (!res.headersSent) reply(res, 502, 'the board is not running on the PC'); else res.destroy(); });
    req.on('data', d => { size += d.length; if (size > MAX_BODY) { up.destroy(); if (!res.headersSent) reply(res, 413, 'message too big'); req.destroy(); } });
    req.pipe(up);
  });
}

module.exports = { createBridge, allowed, keyMatches, readKey, newKey, keyOf, braked, miss, misses };

/* ---------- command line ---------- */
if (require.main === module) {
  const cmd = process.argv[2] || 'run';
  if (cmd === 'new-key') { newKey(); console.log('New phone key made. Pair the phone again: node bridge/bridge.js pair <address>'); }
  else if (cmd === 'pair') {
    const addr = process.argv[3];
    if (!addr) { console.log('Usage: node bridge/bridge.js pair https://<this-pc>.<tailnet>.ts.net'); process.exit(1); }
    console.log(`aigile://pair?url=${encodeURIComponent(addr.replace(/\/+$/, ''))}&key=${encodeURIComponent(keyOf())}`);
  } else if (cmd === 'run') {
    const server = createBridge();
    server.listen(PORT, BIND, () => console.log(`Phone bridge on http://${BIND}:${PORT} -> board on localhost:${BOARD_PORT}`));
  } else { console.log('Commands: run (default), pair <address>, new-key'); process.exit(1); }
}
