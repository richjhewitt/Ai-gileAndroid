// AII-9: a throwaway AI-gile board for tests. Runs the real board from ../AI-gile (or AIGILE_ROOT) in this
// process, on its own port and temp data folder, seeded with a team, a 💬 chat and an open question.
// Rich's live board (port 4321, AI-gile/data) is never touched, and no AI agent is ever woken (stub command).
//
//   node testkit/test-board.mjs [--port N] [--keep]
//
// Prints one line "TESTBOARD {json}" (base, port, dir, slug, cards) once the board is up and seeded, then runs
// until stdin closes, Ctrl+C or a kill. One process: killing it can never leave a stray board behind.
import { execFile } from 'node:child_process';
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import net from 'node:net';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(process.env.AIGILE_ROOT || path.join(HERE, '..', '..', 'AI-gile'));
const arg = name => { const i = process.argv.indexOf(name); return i > 0 ? process.argv[i + 1] : undefined; };
const keep = process.argv.includes('--keep');

const freePort = () => new Promise((res, rej) => { const s = net.createServer(); s.once('error', rej); s.listen(0, '127.0.0.1', () => { const { port } = s.address(); s.close(() => res(port)); }); });
const fail = msg => { console.error('test-board: ' + msg); process.exit(2); };

if (!fs.existsSync(path.join(ROOT, 'server.js'))) fail(`no AI-gile at ${ROOT} (set AIGILE_ROOT)`);
const port = Number(arg('--port')) || await freePort();
if (port === 4321) fail('4321 is Rich\'s live board: pick another port');

/* ---------- a private board ---------- */
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'aigile-android-test-'));
const data = path.join(dir, 'data'), usage = path.join(dir, 'usage');
fs.mkdirSync(usage);
fs.writeFileSync(path.join(dir, 'aigile.local.json'), '{}');   // own settings: none of Rich's (relay, Discord, servers)
const require = createRequire(path.join(ROOT, 'server.js'));
const platform = require('./lib/platform');
const stub = platform.writeStub(fs, path.join(dir, 'agent-stub' + platform.stubExt()), 'log', path.join(dir, 'wakes.txt'));
Object.assign(process.env, {
  AIGILE_PORT: String(port), AIGILE_DATA: data, AIGILE_LOCAL: path.join(dir, 'aigile.local.json'), AIGILE_USAGE_DIR: usage,
  AIGILE_AGENT_COMMAND: stub, AIGILE_OWNER: 'Owner', AIGILE_UNDO_WAKE_MS: '0', AIGILE_NO_WINDOWS: '1',
  AIGILE_RELAY: 'http://127.0.0.1:9', AIGILE_NO_TERMINAL: '1', AIGILE_NO_BUG_FETCH: '1',
});
for (const k of ['AIGILE_DEV', 'AIGILE_ALLOW_DONE', 'AIGILE_AS', 'AIGILE_URL']) delete process.env[k];

const cleanup = () => { if (!keep) try { fs.rmSync(dir, { recursive: true, force: true }); } catch {} };
process.on('exit', cleanup);
for (const sig of ['SIGINT', 'SIGTERM', 'SIGHUP']) process.on(sig, () => process.exit(0));

// server.js prints its own start-up line; ours is the one starting "TESTBOARD ".
process.chdir(ROOT);
require('./server.js');

const base = `http://127.0.0.1:${port}`;
const api = async (method, url, body) => {
  const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(`${method} ${url} -> ${r.status} ${JSON.stringify(j)}`);
  return j;
};
// The CLI runs async: a sync call would block this process, which is also the board it talks to.
const cli = (...args) => new Promise((res, rej) => execFile(process.execPath, [path.join(ROOT, 'aigile.js'), ...args],
  { env: { ...process.env, AIGILE_URL: base }, windowsHide: true }, (e, out, err) => e ? rej(new Error(`aigile ${args[0]}: ${err || e.message}`)) : res(out)));

for (let i = 0; i < 100; i++) { try { await api('GET', '/api/meta'); break; } catch { await new Promise(r => setTimeout(r, 100)); } }

/* ---------- seed ---------- */
const P = 'demo-team';
try {
  await api('POST', '/api/projects', { name: 'Demo Team', key: 'DEM' });
  await cli('team', P, 'Pixel', '--role', 'Lead', '--model', 'Opus', '--status', '💤 idle');
  // A 💬 message from the owner and the lead's reply (what the phone's chat shows).
  const dm = await api('POST', `/api/projects/${P}/cards`, { title: '💬 Is the login page done?', description: 'Is the login page done?', type: 'task', status: 'team', createdBy: 'Owner', quick: true });
  await api('POST', `/api/projects/${P}/cards/${dm.id}/comments`, { author: 'Pixel', text: '- Yes: it is in For you to test' });
  // An open question with answer buttons.
  const q = await api('POST', `/api/projects/${P}/cards`, { title: 'Dark mode', type: 'feature', status: 'todo' });
  await cli('ask', P, q.id, 'Dark mode on by default?', '--options', 'Yes,No', '--as', 'Pixel');
  // A plain card, so a reply has somewhere else to land.
  const plain = await api('POST', `/api/projects/${P}/cards`, { title: 'Settings page', type: 'feature', status: 'todo' });
  // A quiet second project: listed, nothing unread.
  await api('POST', '/api/projects', { name: 'Quiet Project', key: 'QUI' });
  console.log('TESTBOARD ' + JSON.stringify({ base, port, dir, slug: P, cards: { dm: dm.id, question: q.id, plain: plain.id } }));
} catch (e) { fail('seeding failed: ' + e.message); }

// Run until the test that started us closes our stdin (or kills us).
process.stdin.on('end', () => process.exit(0));
process.stdin.on('error', () => process.exit(0));
process.stdin.resume();
