# Native direct messages (AII-321)

Built from our own design while the chilled chat audit (AII-322) is blocked; the audit will be checked against this.

## Security (AII-325), `data/DmCrypto.kt`
- Each device holds a P-256 key pair; the private key never leaves the phone
- Two people derive one shared key by ECDH + HKDF, so the board only relays ciphertext
- Every message is AES-256-GCM sealed; the chat id and sender are authenticated, so a message moved elsewhere fails to open
- A **safety number** (30 digits) is the same on both phones: compare it to spot a swapped key
- Not yet: key storage in Android Keystore, key change warnings, group chats, forward secrecy (ratchet)
