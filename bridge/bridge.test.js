// node --test bridge   (a fake board on a spare port; never touches the real one)
const test = require('node:test');
const assert = require('node:assert');
const http = require('http');
const fs = require('fs');
const os = require('os');
const path = require('path');
const B = require('./bridge');

const KEY = 'k'.repeat(43);
let board, bridge, bridgePort, seen;

const listen = s => new Promise(ok => s.listen(0, '127.0.0.1', () => ok(s.address().port)));

test.before(async () => {
  board = http.createServer((req, res) => {
    let body = '';
    req.on('data', d => body += d);
    req.on('end', () => {
      seen = { method: req.method, url: req.url, headers: req.headers, body };
      res.writeHead(200, { 'Content-Type': 'application/json', 'Set-Cookie': 'x=1' });
      res.end(JSON.stringify({ ok: true }));
    });
  });
  const boardPort = await listen(board);
  bridge = B.createBridge({ key: KEY, boardPort });
  bridgePort = await listen(bridge);
});
test.after(() => { board.close(); bridge.close(); });
test.beforeEach(() => { seen = null; B.misses.clear(); });

function call(method, p, { key = KEY, body, headers = {} } = {}) {
  return new Promise((ok, no) => {
    const h = { ...headers };
    if (key) h.authorization = 'Bearer ' + key;
    if (body) h['content-type'] = 'application/json';
    const r = http.request({ host: '127.0.0.1', port: bridgePort, method, path: p, headers: h }, res => {
      let s = ''; res.on('data', d => s += d); res.on('end', () => ok({ code: res.statusCode, headers: res.headers, body: s }));
    });
    r.on('error', no);
    r.end(body ? JSON.stringify(body) : undefined);
  });
}

test('no key or a wrong key gets 401 and never reaches the board', async () => {
  assert.equal((await call('GET', '/api/chats', { key: null })).code, 401);
  assert.equal((await call('GET', '/api/chats', { key: 'nope' })).code, 401);
  assert.equal(seen, null);
});

test('a GET the Messenger uses goes through, as localhost, without the phone key', async () => {
  const r = await call('GET', '/api/chats/othertrains?since=1', { headers: { origin: 'https://evil.example', cookie: 'a=b' } });
  assert.equal(r.code, 200);
  assert.deepEqual(JSON.parse(r.body), { ok: true });
  assert.equal(seen.url, '/api/chats/othertrains?since=1');
  assert.match(seen.headers.host, /^localhost:\d+$/);
  assert.equal(seen.headers.authorization, undefined);
  assert.equal(seen.headers.origin, undefined);
  assert.equal(seen.headers.cookie, undefined);
  assert.equal(r.headers['set-cookie'], undefined);
});

test('a reply POST goes through as JSON with its body', async () => {
  const r = await call('POST', '/api/projects/othertrains/cards/OTH-9/comments', { body: { author: 'Rich', text: 'hi' } });
  assert.equal(r.code, 200);
  assert.equal(seen.method, 'POST');
  assert.match(seen.headers['content-type'], /^application\/json/);
  assert.deepEqual(JSON.parse(seen.body), { author: 'Rich', text: 'hi' });
});

test('anything else on the board is 404 from the phone', async () => {
  for (const [m, p] of [
    ['GET', '/api/projects'], ['GET', '/api/secrets'], ['POST', '/api/chats/othertrains'], ['DELETE', '/api/dms/contacts/abc'],
    ['PATCH', '/api/projects/x/cards/X-1'], ['GET', '/api/chats/_watchman/../../secrets'], ['GET', '/api/avatars/..%2F..%2Fdata'],
    ['GET', '/api/avatars/%2e%2e'], ['POST', '/api/chats/_watchman/say'], ['GET', '/'],
  ]) assert.equal((await call(m, p)).code, 404, `${m} ${p}`);
  assert.equal(seen, null);
});

test('10 wrong keys in a minute: that address waits, even with the right key', async () => {
  for (let i = 0; i < 10; i++) await call('GET', '/api/meta', { key: 'wrong' });
  assert.equal((await call('GET', '/api/meta')).code, 429);
  B.misses.clear();
  assert.equal((await call('GET', '/api/meta')).code, 200);
});

test('a body over the limit is refused', async () => {
  const r = await call('POST', '/api/chats/_watchman', { body: { text: 'x'.repeat(300 * 1024) } });
  assert.equal(r.code, 413);
});

test('board not running: 502 with a plain message', async () => {
  const off = B.createBridge({ key: KEY, boardPort: 1 });
  const port = await listen(off);
  const r = await new Promise(ok => http.get({ host: '127.0.0.1', port, path: '/api/meta', headers: { authorization: 'Bearer ' + KEY } }, res => {
    let s = ''; res.on('data', d => s += d); res.on('end', () => ok({ code: res.statusCode, body: s }));
  }));
  off.close();
  assert.equal(r.code, 502);
  assert.match(JSON.parse(r.body).error, /board is not running/);
});

test('the phone key is made once, kept, and replaced by new-key', () => {
  const f = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'bridge-')), 'k.local.json');
  const a = B.keyOf(f);
  assert.ok(a.length >= 40);
  assert.equal(B.keyOf(f), a);
  assert.notEqual(B.newKey(f), a);
  assert.ok(B.keyMatches('Bearer ' + B.readKey(f), B.readKey(f)));
  assert.ok(!B.keyMatches('Bearer ' + a, B.readKey(f)));
});
