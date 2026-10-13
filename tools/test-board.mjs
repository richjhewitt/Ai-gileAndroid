// AII-9: a throwaway AI-gile board for the app's JVM tests and screenshots.
// Starts the real board (AI-gile's own QA server helper) on a free port with its own temp data, seeds a project with
// a 💬 chat and a question with answer buttons, prints one JSON line with what it made, then runs until stdin closes.
//
//   node tools/test-board.mjs           # start, print {"base":...}, stop when stdin closes (or Ctrl+C)
//   node tools/test-board.mjs --check   # start, check the chat API the app uses, stop; exit 1 on a failure
//
// AIGILE_HOME points at the AI-gile folder (default: ../AI-gile next to this repo).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const home = process.env.AIGILE_HOME || path.resolve(here, '..', '..', 'AI-gile');
const qa = await import(pathToFileURL(path.join(home, 'tools', 'qa', 'lib.mjs')).href);

const OWNER = 'Owner';   // the QA server always speaks to "Owner"
const board = await qa.startServer({}, await qa.freePort());
const done = () => { board.stop(); try { fs.rmSync(board.dir, { recursive: true, force: true }); } catch {} };
process.on('SIGINT', () => { done(); process.exit(0); });
process.on('SIGTERM', () => { done(); process.exit(0); });

/* ---------- seed ---------- */
const p = await qa.mkProject(board, 'Demo Shop', 'DEMO');
const slug = p.slug;
await board.ok('POST', `/api/projects/${slug}/team`, { name: 'Lead', role: 'Lead', model: 'Opus' }).catch(() => {});
// A 💬 chat: the owner writes, the lead answers.
const dm = await qa.mkCard(board, slug, '💬 Is the shop ready?', { quick: true, description: 'Is the shop ready for Friday?', createdBy: OWNER, status: 'team' });
await qa.say(board, slug, dm.id, 'Lead', '- Nearly: checkout is done, the receipts page is next');
// A question with answer buttons (shows in the chat until answered).
const ask = await qa.mkCard(board, slug, 'Pick a colour for the shop', { status: 'todo' });
await qa.say(board, slug, ask.id, 'Lead', '- **Which colour for the shop?**', { needsYou: true, mark: 'ask', options: 'Teal,Orange' });

const info = { base: board.base, owner: OWNER, slug, dm: dm.id, ask: ask.id, data: board.data };

/* ---------- --check: the calls the app makes ---------- */
if (process.argv.includes('--check')) {
  let bad = 0;
  const check = (name, ok, extra) => { console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${ok ? '' : '  -> ' + JSON.stringify(extra)}`); if (!ok) bad++; };
  try {
    const meta = await board.ok('GET', '/api/meta');
    check('meta names the owner', meta.owner === OWNER, meta.owner);
    const chats = await board.ok('GET', '/api/chats');
    check('chats list has the demo project', chats.some(c => c.slug === slug), chats.map(c => c.slug));
    const chat = await board.ok('GET', `/api/chats/${slug}`);
    check('chat has the lead answer', chat.messages.some(m => m.author === 'Lead' && /checkout/.test(m.text)), chat.messages);
    check('chat has the question with buttons', chat.messages.some(m => (m.options || []).includes('Teal')), chat.messages);
    await board.ok('POST', `/api/projects/${slug}/cards/${dm.id}/comments`, { author: OWNER, text: 'Great, thanks' });
    const after = await board.ok('GET', `/api/chats/${slug}`);
    check('a reply shows in the chat as mine', after.messages.some(m => m.mine && m.text === 'Great, thanks'), after.messages);
    const made = await board.ok('POST', `/api/projects/${slug}/cards`, { title: '💬 New message', quick: true, description: 'Hello team', createdBy: OWNER, status: 'team' });
    check('a new message makes a 💬 card', /^DEMO-\d+$/.test(made.id || ''), made);
  } catch (e) { check('API calls', false, e.message); }
  done();
  process.exit(bad ? 1 : 0);
}

console.log(JSON.stringify(info));
process.stdin.resume();
process.stdin.on('end', () => { done(); process.exit(0); });
