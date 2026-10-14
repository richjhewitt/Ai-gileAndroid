// Throwaway test board for the app's tests (AII-9): a private AI-gile on its own port, with its own data,
// seeded with a team and a DM, so tests never touch Rich's real board.
//   node tools/testboard/testboard.mjs start [--port N] [--board <AI-gile folder>]   prints the address
//   node tools/testboard/testboard.mjs check    calls every API the app uses, one line each
//   node tools/testboard/testboard.mjs phone [--host test.aigile.lol] [--server Smizflark]   serves it to Rich's phone at
//     https://test.aigile.lol through an ssh tunnel to the server and shows a QR to pair the app (--tailscale [--https 8443]: the old way)
//   node tools/testboard/testboard.mjs stop     stops it and deletes its data
import { execFileSync, spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import crypto from 'node:crypto';
import fs from 'node:fs';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const STATE = path.join(os.tmpdir(), 'aigile-testboard.json');
const args = process.argv.slice(2), cmd = args[0];
const flag = name => { const i = args.indexOf('--' + name); return i > 0 ? args[i + 1] : undefined; };
const sleep = ms => new Promise(r => setTimeout(r, ms));
const readState = () => { try { return JSON.parse(fs.readFileSync(STATE, 'utf8')); } catch { return null; } };

/* ---------- api ---------- */
async function call(base, method, url, body) {
  const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json', Authorization: 'Bearer test-key' }, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  if (r.status >= 300) throw new Error(`${method} ${url} -> ${r.status} ${text.slice(0, 200)}`);
  try { return JSON.parse(text); } catch { return text; }
}

const freePort = () => new Promise((res, rej) => { const s = net.createServer(); s.unref(); s.on('error', rej); s.listen(0, '127.0.0.1', () => { const { port } = s.address(); s.close(() => res(port)); }); });

/* ---------- start ---------- */
async function start() {
  const old = readState();
  if (old) { console.log(`already running: ${old.url} (stop it first)`); return; }
  const board = path.resolve(flag('board') || path.join(HERE, '..', '..', '..', 'AI-gile'));
  if (!fs.existsSync(path.join(board, 'server.js'))) throw new Error(`no AI-gile at ${board}: pass --board <folder>`);
  const port = Number(flag('port')) || await freePort();
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'aigile-testboard-'));
  const data = path.join(dir, 'data'), usage = path.join(dir, 'usage'), home = path.join(dir, 'projects');
  for (const d of [data, usage, home]) fs.mkdirSync(d);
  const local = path.join(dir, 'aigile.local.json');
  fs.writeFileSync(local, '{}');
  // Agents the board would wake (a DM to a lead) run a do-nothing stub, never a real agent.
  const platform = createRequire(import.meta.url)(path.join(board, 'lib', 'platform.js'));
  const stub = platform.writeStub(fs, path.join(dir, 'agent-stub' + platform.stubExt()), 'none');
  const token = crypto.randomBytes(16).toString('hex');
  const env = { ...process.env, AIGILE_LOCAL: local, AIGILE_PORT: String(port), AIGILE_DATA: data, AIGILE_HOME: home,
    AIGILE_AGENT_COMMAND: stub, AIGILE_USAGE_DIR: usage, AIGILE_NO_USAGE_AUTO: '1', AIGILE_OWNER: 'Rich', AIGILE_QA_TOKEN: token };
  delete env.AIGILE_DEV; delete env.AIGILE_ALLOW_DONE; delete env.AIGILE_AS; delete env.AIGILE_URL;
  const proc = spawn(process.execPath, [path.join(board, 'server.js')], { env, cwd: board, stdio: 'ignore', detached: true, windowsHide: true });
  proc.unref();
  const url = `http://127.0.0.1:${port}`;
  // Only trust the server that hands back our token: never mistake Rich's board for this one.
  let mine = false;
  for (let i = 0; i < 100 && !mine; i++) {
    try { mine = (await call(url, 'GET', '/api/meta')).qaToken === token; } catch {}
    if (!mine) await sleep(100);
  }
  if (!mine) { try { process.kill(proc.pid); } catch {} fs.rmSync(dir, { recursive: true, force: true }); throw new Error(`no test board on port ${port} (port taken?)`); }
  fs.writeFileSync(STATE, JSON.stringify({ url, pid: proc.pid, dir, board }, null, 2));
  await seed(url);
  console.log(url);
}

/* ---------- seed ---------- */
// One team with a DM from Rich and the lead's answer, so the chats list and a chat have something to show.
async function seed(url) {
  await call(url, 'POST', '/api/projects', { name: 'Test Team', key: 'TST' });
  const card = await call(url, 'POST', '/api/projects/test-team/cards', { title: '💬 Hello team', description: 'Hello from the test board', type: 'task', status: 'team', createdBy: 'Rich', quick: true });
  await call(url, 'POST', `/api/projects/test-team/cards/${card.id}/comments`, { author: 'Lead', text: 'Hi Rich, the test board is up' });
}

/* ---------- check ---------- */
// Every call the app's BoardSource makes (app/.../data/Source.kt), against the test board.
async function check() {
  const s = readState();
  if (!s) throw new Error('no test board: run start first');
  const steps = [
    ['owner name', async () => (await call(s.url, 'GET', '/api/meta')).owner === 'Rich'],
    ['chats list has the team', async () => (await call(s.url, 'GET', '/api/chats')).some(c => JSON.stringify(c).includes('test-team'))],
    ['a team chat opens', async () => !!(await call(s.url, 'GET', '/api/chats/test-team'))],
    ['friends list answers', async () => Array.isArray(await call(s.url, 'GET', '/api/dms/contacts'))],
    ['new message makes a card', async () => !!(await call(s.url, 'POST', '/api/projects/test-team/cards', { title: '💬 From check', description: 'From check', type: 'task', status: 'team', createdBy: 'Rich', quick: true })).id],
  ];
  let bad = 0;
  for (const [name, fn] of steps) {
    let ok = false, why = '';
    try { ok = await fn(); } catch (e) { why = ' ' + e.message; }
    if (!ok) bad++;
    console.log(`${ok ? 'PASS' : 'FAIL'} ${name}${why}`);
  }
  process.exitCode = bad ? 1 : 0;
}

/* ---------- phone ---------- */
// The test board on Rich's phone (AII-67). Default: https://test.aigile.lol, through an ssh tunnel to the server
// (ssh -R: the server's 127.0.0.1:4610 reaches this PC's test board; nginx there sends /api/ and /media/ of
// test.aigile.lol to it, tools/testboard/nginx-testboard.conf). The board lets that Host in only for a paired phone.
// --tailscale: the old way, `tailscale serve` on its own https port (parked, AII-67).
// Then an app pairing (the board's POST /api/phone/app) shown as a QR on a page that opens on the PC.
const tailscale = (...a) => execFileSync('tailscale', a, { encoding: 'utf8', windowsHide: true, timeout: 20000, stdio: 'pipe' });
// The ssh login of a server saved in the board's Setup > My servers (aigile.local.json), so no address lives in this repo.
function serverLogin(board, name) {
  let all = [];
  try { all = JSON.parse(fs.readFileSync(path.join(board, 'aigile.local.json'), 'utf8')).servers || []; } catch {}
  const s = all.find(x => x.kind === 'ssh' && x.name.toLowerCase() === name.toLowerCase());
  if (!s) throw new Error(`no ssh server "${name}" in Setup > My servers: pass --server <name>`);
  return s.value;
}
async function phone() {
  if (args.includes('--tailscale')) return phoneTailscale();
  const s = readState();
  if (!s) throw new Error('no test board: run start first');
  const host = flag('host') || 'test.aigile.lol';
  const remote = Number(flag('remote-port')) || 4610;
  const login = serverLogin(s.board, flag('server') || 'Smizflark');
  if (s.tunnel) try { process.kill(s.tunnel); } catch {}
  const local = new URL(s.url).port;
  const ssh = spawn('ssh', ['-N', '-o', 'BatchMode=yes', '-o', 'ExitOnForwardFailure=yes', '-o', 'ServerAliveInterval=30',
    '-R', `127.0.0.1:${remote}:127.0.0.1:${local}`, login], { detached: true, stdio: 'ignore', windowsHide: true });
  ssh.unref();
  s.tunnel = ssh.pid; s.host = host; fs.writeFileSync(STATE, JSON.stringify(s, null, 2));
  // The tunnel is up once the board answers through it on the server.
  let up = false;
  for (let i = 0; i < 20 && !up; i++) {
    await sleep(500);
    try { up = execFileSync('ssh', ['-o', 'BatchMode=yes', login, `curl -s -o /dev/null -w %{http_code} -H 'Host: ${host}' http://127.0.0.1:${remote}/api/meta`],
      { encoding: 'utf8', windowsHide: true, timeout: 15000, stdio: 'pipe' }).trim() === '403'; } catch {}
  }
  if (!up) throw new Error(`the tunnel to ${flag('server') || 'Smizflark'} did not come up (port ${remote} taken there?)`);
  const { qr } = await call(s.url, 'POST', '/api/phone/app', { host });
  pairPage(s, qr, `https://${host}`);
}
async function phoneTailscale() {
  const s = readState();
  if (!s) throw new Error('no test board: run start first');
  const https = Number(flag('https')) || 8443;
  if (https === 443) throw new Error('443 is for the real board: pick another port');
  const ts = String(JSON.parse(tailscale('status', '--json')).Self?.DNSName || '').replace(/\.$/, '').toLowerCase();
  if (!ts) throw new Error('Tailscale is not signed in on this PC');
  // First time on a tailnet, serve waits on a sign-in page to switch Serve on: say so instead of hanging.
  try { tailscale('serve', '--bg', `--https=${https}`, s.url); } catch (e) {
    const link = String((e.stdout || '') + (e.stderr || '')).match(/https:[/][/]login[.]tailscale[.]com[/][!-~]+/);
    throw new Error(link ? `Serve is off on your Tailscale: open ${link[0]}, switch it on, then run phone again` : 'tailscale serve failed: ' + e.message);
  }
  s.https = https; fs.writeFileSync(STATE, JSON.stringify(s, null, 2));
  const { qr } = await call(s.url, 'POST', '/api/phone/app', { host: `${ts}:${https}` });
  if (!qr.includes(`:${https}&`)) throw new Error('this AI-gile drops the port from the phone address: use one with AII-67 (branch tds/phone-port)');
  pairPage(s, qr, `https://${ts}:${https}`);
}
function pairPage(s, qr, address) {
  const page = path.join(s.dir, 'pair.html');
  fs.writeFileSync(page, `<!doctype html><meta charset="utf-8"><title>Test board: pair the app</title>
<body style="font:18px system-ui;text-align:center;padding:2em"><h2>📱 Test board</h2><p>In the app: <b>Scan QR code</b></p>
${qrSvg(s.board, qr)}<p style="color:#666;font-size:14px">${address} (demo chats, not your real board)</p></body>`);
  console.log(`serving ${address}\npair the app with: ${page}`);
  if (!args.includes('--no-open')) spawn('explorer', [page], { detached: true, stdio: 'ignore', windowsHide: true }).unref();
}
// The QR code from the board's own Setup page (public/js/phone.js), so this tool needs no QR package.
function qrSvg(board, text) {
  const src = fs.readFileSync(path.join(board, 'public', 'js', 'phone.js'), 'utf8');
  const a = src.indexOf('  function qr(text, low)'), b = src.indexOf('  function qrSvg(');
  if (a < 0 || b < a) throw new Error('no QR code maker in this AI-gile');
  const M = new Function(src.slice(a, b) + '\nreturn qr;')()(text);
  const n = M.length + 8; let d = '';
  M.forEach((r, y) => r.forEach((c, x) => { if (c) d += `M${x + 4} ${y + 4}h1v1h-1z`; }));
  return `<svg viewBox="0 0 ${n} ${n}" width="320" height="320"><rect width="${n}" height="${n}" fill="#fff"/><path d="${d}" fill="#000"/></svg>`;
}

/* ---------- stop ---------- */
function stop() {
  const s = readState();
  if (!s) { console.log('not running'); return; }
  if (s.https) try { tailscale('serve', `--https=${s.https}`, 'off'); } catch {}
  if (s.tunnel) try { process.kill(s.tunnel); } catch {}
  try { process.kill(s.pid); } catch {}
  // The server may hold files a moment after it dies (Windows): retry the delete.
  try { fs.rmSync(s.dir, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 }); } catch {}
  fs.rmSync(STATE, { force: true });
  console.log('stopped');
}

const cmds = { start, check, phone, stop };
if (!cmds[cmd]) { console.log('usage: node tools/testboard/testboard.mjs start|check|phone|stop'); process.exit(2); }
try { await cmds[cmd](); } catch (e) { console.error(e.message); process.exit(1); }
