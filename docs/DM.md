# Native direct messages (AII-321)

Chilled chat audit: docs/DM-AUDIT.md (AII-322).

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

## Board API used (AII-324, AII-326)
The relay spec once planned here was dropped: the board already has secure friend DMs (its own keys, relay and six safety words, lifted from chilled chat), so the app uses those and `DmCrypto` is kept unused for now.
- `GET /api/dms/contacts`: friends, with `safety`, `checked`, `typing`, last times
- `PATCH /api/dms/contacts/<id>` `{checked: true}`: we read the safety words to each other
- `GET|POST /api/dms/contacts/<id>/messages`: lines; a POST may carry `reply` (a line id) and sealed `attachments`
- `PATCH|DELETE /api/dms/contacts/<id>/messages/<mid>`: edit or delete one of your own lines
- `POST /api/dms/contacts/<id>/messages/<mid>/react` `{emoji}`: toggle your reaction (reactions are not sealed, see DM-AUDIT.md)
- `POST /api/dms/contacts/<id>/typing`: at most every 4 s
- All calls ride the existing sealed phone link (`data/Seal.kt`)
- Sending or editing a friend line needs that sealed link: an older Bearer-only pairing stops with "Pair the phone again" (AII-336)
- The phone remembers each friend's safety words (`data/SafetyWatch.kt`); new words replace the message box with a warning until **We checked it**, and the outbox holds friend lines back too (AII-333)
- The phone's own key is already sealed with the Android Keystore (`Vault` in `data/Store.kt`, AII-334)
