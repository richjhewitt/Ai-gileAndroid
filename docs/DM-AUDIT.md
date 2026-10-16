# Chilled chat DMs: feature and security audit (AII-322)

Read from ChilledDVStuff/chilledchat (server.js `DM ROUTES` / `DM KEY ROUTES`, public/app.js E2E helpers and `sendDm`). Checked against our design in docs/DM.md.

## Features to match
- 1:1 conversations, list with partner, last message, unread count (`GET /api/dms`)
- Send, edit, delete own message; replies with a quoted snippet
- Read markers per person (`dm_reads`)
- Reactions (`dm_reactions`)
- Files, GIFs, voice notes; resumable chunked uploads
- Typing indicator (`dm_typing_start/stop`)
- Clear chat for me only (`dm_clears`, non-destructive)
- Disappearing messages: 24h, shared per conversation, deletes for both
- Push notification when the partner is offline ("💬 New message" preview for sealed text)
- 1:1 voice and video calls (WebRTC signalling over the socket)
- Key backup: private key wrapped with the password (PBKDF2 100k, AES-GCM), export/import key file, deliberate "reset conversation key"

## Their security model
- Each account has an RSA-OAEP 2048 key pair in IndexedDB
- Each conversation has one AES-256-GCM key, wrapped with each member's RSA public key and stored on the server
- Server also encrypts content at rest with its own key

## Gaps (threat list)
- **No key verification:** the server hands out public keys (`/api/publickeys`) and nobody can check them, so the server could swap a key and read new messages. Ours: safety number (have it)
- **Silent plain-text fallback:** `sendDm` sends plain text if the conversation key is missing. Ours: never send unsealed; block and show why
- **Files are not end-to-end sealed:** uploads go up as-is; name, type and size are plain. Ours: seal file bytes and name with the chat key (AII-326)
- **Reply snippet copied by the server:** the server decrypts at-rest and stores the first 120 chars of the quoted message; for E2E text that is ciphertext, fine, but for any plain fallback it leaks. Ours: quote inside the sealed body
- **Reactions route does not check membership:** `POST /api/dms/reactions/:messageId` works for any signed-in user who knows a message id (ids are random UUIDs, so low risk). Ours: relay checks membership on every DM call
- **Reactions are plain:** emoji per message visible to the server. Ours: accept (low value), log it
- **Key backup salt is the username**, 100k PBKDF2: weak against a server that brute-forces short passwords. Ours: no server key backup in the first cut; keys stay on the phone
- **No forward secrecy:** one static key per conversation. Ours too for now (ratchet later)
- **Metadata:** server sees who talks to whom and when. Same for us; accepted

## What changes in our plan
- AII-324: the relay checks membership on every call, the app refuses to send without a sealed key
- AII-326: sealed files and quotes; reactions plain (logged decision)
- Later cards: key change warning, Keystore storage, ratchet
