// Run: node --test bridge/
'use strict';
const test = require('node:test');
const assert = require('node:assert');
const http = require('http');
const { createBridge, allowed, bodyLimit } = require('./bridge');

const KEY = 'k'.repeat(40);

// A fake board that records what reached it and refuses anything not from localhost, like the real one.
function fakeBoard() {
  const seen = [];
  const server = http.createServer((req, res) => {
    let body = '';
    req.on('data', c => body += c);
    req.on('end', () => {
      seen.push({ method: req.method, url: req.url, headers: req.headers, body });
      if (!/^localhost:\d+$/.test(req.headers.host)) { res.writeHead(403); return res.end('{"error":"wrong host"}'); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: true, path: req.url }));
    });
  });
  return { server, seen };
}

function listen(server) { return new Promise(r => server.listen(0, '127.0.0.1', () => r(server.address().port))); }

function call(port, method, p, { key = KEY, body, headers = {} } = {}) {
  return new Promise((resolve, reject) => {
    const h = { ...headers };
    if (key) h.Authorization = 'Bearer ' + key;
    const data = body === undefined ? undefined : JSON.stringify(body);
    if (data) { h['Content-Type'] = 'application/json'; h['Content-Length'] = Buffer.byteLength(data); }
    const req = http.request({ host: '127.0.0.1', port, method, path: p, headers: h }, res => {
      let text = ''; res.on('data', c => text += c); res.on('end', () => resolve({ status: res.statusCode, text }));
    });
    req.on('error', reject);
    req.end(data);
  });
}

async function setup(opts = {}) {
  const board = fakeBoard();
  const boardPort = await listen(board.server);
  const bridge = createBridge({ key: KEY, boardPort, ...opts });
  const port = await listen(bridge);
  return { board, port, close: () => { bridge.close(); board.server.close(); } };
}

test('only Messenger routes are allowed', () => {
  assert.ok(allowed('GET', '/api/meta'));
  assert.ok(allowed('GET', '/api/chats/idea-playground'));
  assert.ok(allowed('POST', '/api/projects/ai-gile/cards/AIG-1/comments'));
  assert.ok(allowed('GET', '/api/avatars/fox.png'));
  assert.ok(allowed('GET', '/api/avatars/badge/cat/teal/headphones'));
  assert.ok(allowed('GET', '/api/projects/tunio/logo'));
  assert.ok(!allowed('POST', '/api/projects/tunio/logo'));
  assert.ok(!allowed('DELETE', '/api/dms/contacts/abc'));
  assert.ok(!allowed('POST', '/api/chats/idea-playground'));
  assert.ok(!allowed('GET', '/api/projects'));
  assert.ok(!allowed('GET', '/api/avatars/../settings'));
  assert.ok(!allowed('PATCH', '/api/projects/x/cards/X-1'));
  assert.ok(allowed('GET', '/api/projects/ai-gile/cards/AIG-12'));   // one card (AII-45)
  assert.ok(!allowed('GET', '/api/projects/ai-gile/cards'));
  // pictures (AII-27)
  assert.ok(allowed('POST', '/api/projects/ai-gile/attachments'));
  assert.ok(allowed('POST', '/api/projects/_watchman/attachments'));
  assert.ok(allowed('GET', '/api/projects/ai-gile/attachments/mg1abc-my%20shot.png'));
  assert.ok(allowed('GET', '/api/dms/contacts/c1/files/m10-cat.webp'));
  assert.ok(!allowed('GET', '/api/projects/ai-gile/attachments'));
  assert.ok(!allowed('DELETE', '/api/projects/ai-gile/attachments/x-a.png'));
  assert.ok(!allowed('GET', '/api/projects/ai-gile/attachments/..%2f..%2fsettings'));
});

test('a wrong or missing key gets 401 and never reaches the board', async () => {
  const s = await setup();
  try {
    assert.equal((await call(s.port, 'GET', '/api/meta', { key: null })).status, 401);
    assert.equal((await call(s.port, 'GET', '/api/meta', { key: 'x'.repeat(40) })).status, 401);
    assert.equal(s.board.seen.length, 0);
  } finally { s.close(); }
});

test('a good key passes through as localhost, without key, cookies or Origin', async () => {
  const s = await setup();
  try {
    const r = await call(s.port, 'POST', '/api/chats/_watchman', { body: { text: 'hi' }, headers: { Origin: 'https://evil.example', Cookie: 'a=b' } });
    assert.equal(r.status, 200);
    const got = s.board.seen[0];
    assert.equal(got.method, 'POST');
    assert.equal(got.url, '/api/chats/_watchman');
    assert.match(got.headers.host, /^localhost:\d+$/);
    assert.equal(got.headers.authorization, undefined);
    assert.equal(got.headers.origin, undefined);
    assert.equal(got.headers.cookie, undefined);
    assert.equal(got.headers['content-type'], 'application/json');
    assert.deepEqual(JSON.parse(got.body), { text: 'hi' });
  } finally { s.close(); }
});

test('routes the phone does not need are 404 and never reach the board', async () => {
  const s = await setup();
  try {
    assert.equal((await call(s.port, 'GET', '/api/projects')).status, 404);
    assert.equal((await call(s.port, 'DELETE', '/api/dms/contacts/abc')).status, 404);
    assert.equal(s.board.seen.length, 0);
  } finally { s.close(); }
});

test('too many wrong keys lock everyone out for a while', async () => {
  let t = 1000;
  const s = await setup({ maxFails: 3, lockMs: 60000, now: () => t });
  try {
    for (let i = 0; i < 3; i++) await call(s.port, 'GET', '/api/meta', { key: 'bad' });
    assert.equal((await call(s.port, 'GET', '/api/meta')).status, 429);
    t += 61000;
    assert.equal((await call(s.port, 'GET', '/api/meta')).status, 200);
  } finally { s.close(); }
});

test('a big body is refused', async () => {
  const s = await setup();
  try {
    const r = await call(s.port, 'POST', '/api/chats/_watchman', { body: { text: 'x'.repeat(70 * 1024) } }).catch(() => ({ status: 413 }));
    assert.equal(r.status, 413);
    assert.equal(s.board.seen.length, 0);
  } finally { s.close(); }
});

test('pictures get a bigger body limit, other calls keep the small one (AII-27)', async () => {
  assert.equal(bodyLimit('/api/chats/_watchman'), 64 * 1024);
  assert.equal(bodyLimit('/api/projects/x/cards/X-1/comments'), 64 * 1024);
  assert.equal(bodyLimit('/api/projects/x/attachments'), 15e6);
  assert.equal(bodyLimit('/api/dms/contacts/c1/messages'), 15e6);
  const s = await setup();
  try {
    const r = await call(s.port, 'POST', '/api/projects/ai-gile/attachments', { body: { name: 'a.png', type: 'image/png', data: 'A'.repeat(1024 * 1024) } });
    assert.equal(r.status, 200);
    assert.equal(JSON.parse(s.board.seen[0].body).data.length, 1024 * 1024);
  } finally { s.close(); }
});

test('board down gives a plain 502', async () => {
  const bridge = createBridge({ key: KEY, boardPort: 1 });
  const port = await listen(bridge);
  try {
    const r = await call(port, 'GET', '/api/meta');
    assert.equal(r.status, 502);
    assert.match(r.text, /board is not running/);
  } finally { bridge.close(); }
});
