// AII-9: checks the throwaway test board. Run: node --test testkit/
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT = path.join(path.dirname(fileURLToPath(import.meta.url)), 'test-board.mjs');

// Starts the board and resolves with its TESTBOARD info once it is seeded.
function start() {
  const proc = spawn(process.execPath, [SCRIPT], { stdio: ['pipe', 'pipe', 'inherit'], windowsHide: true });
  return new Promise((res, rej) => {
    let buf = '';
    const t = setTimeout(() => { proc.kill(); rej(new Error('no TESTBOARD line in 30 s')); }, 30000);
    proc.stdout.on('data', d => {
      buf += d;
      const line = buf.split(/\r?\n/).find(l => l.startsWith('TESTBOARD '));
      if (line) { clearTimeout(t); res({ proc, info: JSON.parse(line.slice(10)) }); }
    });
    proc.on('exit', code => { clearTimeout(t); rej(new Error('test board exited with ' + code)); });
  });
}

test('test board: seeded chats, reply, clean stop', async () => {
  const { proc, info } = await start();
  const get = async url => (await fetch(info.base + url)).json();
  try {
    assert.notEqual(info.port, 4321);
    const chats = await get('/api/chats');
    const demo = chats.find(c => c.slug === 'demo-team');
    assert.ok(demo, 'Demo Team chat listed');
    assert.equal(demo.lead, 'Pixel');
    assert.equal(demo.unread, 1, 'the open question counts as unread');
    assert.ok(chats.some(c => c.slug === 'quiet-project'));
    assert.ok(chats.some(c => c.watchman), 'Lead Scrum Master listed');

    const chat = await get('/api/chats/demo-team');
    assert.ok(chat.messages.some(m => m.card === info.cards.dm && m.mine), 'owner message shown as mine');
    assert.ok(chat.messages.some(m => m.author === 'Pixel'), 'lead reply shown');

    // The phone's reply path: a comment from the owner lands in the chat.
    const r = await fetch(`${info.base}/api/projects/demo-team/cards/${info.cards.dm}/comments`,
      { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ author: 'Owner', text: 'Great, thanks' }) });
    assert.ok(r.ok, 'reply accepted');
    assert.ok((await get('/api/chats/demo-team')).messages.some(m => m.text === 'Great, thanks'));
  } finally {
    proc.stdin.end();
  }
  const code = await new Promise(res => proc.on('exit', res));
  assert.equal(code, 0);
  assert.equal(fs.existsSync(info.dir), false, 'temp folder removed');
  await assert.rejects(fetch(info.base + '/api/meta'), 'board gone');
});
