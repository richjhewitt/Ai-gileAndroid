// Throwaway test board for the app's tests (AII-9): a private AI-gile on its own port, with its own data,
// seeded with a team and a DM, so tests never touch Rich's real board.
//   node tools/testboard/testboard.mjs start [--port N] [--board <AI-gile folder>]   prints the address
//   node tools/testboard/testboard.mjs check    calls every API the app uses, one line each
//   node tools/testboard/testboard.mjs stop     stops it and deletes its data
import { spawn } from 'node:child_process';
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
  fs.writeFileSync(STATE, JSON.stringify({ url, pid: proc.pid, dir }, null, 2));
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

/* ---------- stop ---------- */
function stop() {
  const s = readState();
  if (!s) { console.log('not running'); return; }
  try { process.kill(s.pid); } catch {}
  // The server may hold files a moment after it dies (Windows): retry the delete.
  try { fs.rmSync(s.dir, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 }); } catch {}
  fs.rmSync(STATE, { force: true });
  console.log('stopped');
}

const cmds = { start, check, stop };
if (!cmds[cmd]) { console.log('usage: node tools/testboard/testboard.mjs start|check|stop'); process.exit(2); }
try { await cmds[cmd](); } catch (e) { console.error(e.message); process.exit(1); }
