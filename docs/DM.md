# Native direct messages (AII-321)

Built from our own design while the chilled chat audit (AII-322) is blocked; the audit will be checked against this.

## Security (AII-325), `data/DmCrypto.kt`
- Each device holds a P-256 key pair; the private key never leaves the phone
- Two people derive one shared key by ECDH + HKDF, so the board only relays ciphertext
- Every message is AES-256-GCM sealed; the chat id and sender are authenticated, so a message moved elsewhere fails to open
- A **safety number** (30 digits) is the same on both phones: compare it to spot a swapped key
- Not yet: key storage in Android Keystore, key change warnings, group chats, forward secrecy (ratchet)

## UX design round (AII-323)
Three designs compared; written by the lead alone (no helpers: pace guard), so Rich should react to the pick.

- **A. Discord-style:** left rail of servers and DMs, channel lists. Familiar, but it costs two taps and a lot of screen to reach one person on a phone.
- **B. WhatsApp-style:** one flat list of chats, newest first. Fast, but our team chats and people DMs blur together.
- **C. Our chats list plus a People filter (pick):** DMs sit in the existing chats list as one more chip next to the team chats, with the same pins, mute, search, reminders, drafts and swipes.

Why C:
- Everything already built (reminders, stars, undo send, silent, search) works on DMs for free
- One thumb-reachable list, no second navigation model
- A lock line under the name ("End-to-end sealed") and a tap to the safety number, so security is visible without nagging
- Key change shows a banner in the chat before you can send, never a silent switch

Screens to build (AII-324): People chip, new DM sheet (pick a person), the DM chat (the existing chat screen with a lock line), safety number sheet.

## Board relay spec (needed by AII-324, a change in the board repo)
- `GET /api/dm/people`: people you can message, each with `id`, `name`, `publicKey`
- `PUT /api/dm/key`: publish this device's public key
- `GET /api/dm/chats`: your DM chats with last sealed message and unread count
- `POST /api/dm/chats/<id>/messages`: body `{sealed}`; board stores ciphertext and the sender's key fingerprint only
- `GET /api/dm/chats/<id>/messages?after=<n>`: sealed messages; the app opens them with `DmCrypto`
- All calls ride the existing sealed phone link, so the relay never sees plain text
