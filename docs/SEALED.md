# Sealed phone link v1

Transport is already HTTPS over Tailscale; this seals every call end to end so even the proxy sees nothing, and the device key never travels after pairing.

## Keys
- Pairing gives the app: board url, device id `d` (hex), device key `K` (base64url, 32 random bytes). The QR is `aigile://pair?url=<https url>&id=<d>&key=<K>`.
- Seal key: `S = HKDF-SHA256(ikm = K bytes, salt = UTF-8 "aigile-seal-v1", info = UTF-8 "phone " + d, length 32)`.
- Board stores per device: id, host, name, sha256(K) (for the plain Bearer path) and S (base64url) for sealing, in aigile.local.json (git-ignored).

## Request
- `POST /api/phone/sealed`, header `X-Aigile-Device: <d>`, Content-Type application/json, NO Authorization header.
- Body `{"n": base64url(12-byte random nonce), "c": base64url(ciphertext || 16-byte GCM tag)}`
- Plaintext (UTF-8 JSON): `{"t": <ms since epoch>, "m": "GET"|"POST"|..., "p": "/api/chats", "b": <JSON body or null>}`
- AES-256-GCM, key S, nonce n, AAD = UTF-8 `"aigile-seal-v1|req|" + d`.
- Board rejects (403, plain JSON `{"error":"sealed: ..."}`): unknown device, bad tag, |now − t| > 120 000 ms, nonce seen in the last 10 minutes (keep a per-device set), p not starting with `/api/`, or p on the phone deny list.
- Board runs the inner request as that device (same deny list, same handlers, same Host binding as the device's host).

## Response
- `200 {"n": ..., "c": ...}` with a fresh nonce, plaintext `{"s": <inner HTTP status>, "b": <inner JSON body>}`, AAD `"aigile-seal-v1|res|" + d`.
- Non-JSON inner responses (avatars, attachments): `b` = `{"ct": "<content-type>", "data": base64}`.

## Feature flag
- `GET /api/meta` (Bearer or sealed) includes `"sealed": 1`. The app uses sealing when present, else falls back to Bearer.

## Test vectors

Fixed inputs (never reuse a nonce in real use; both directions share n here only so the vectors stay short):

- K bytes 0x00..0x1f, base64url `AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8`
- d = `a1b2c3d4e5f6`
- n bytes 0x00..0x0b, base64url `AAECAwQFBgcICQoL`
- t = `1700000000000`

base64url is RFC 4648 §5 without `=` padding. JSON is compact (no spaces), keys in exactly this order: request `t, m, p, b`; response `s, b`; envelope `n, c`.

Seal key:

```
S (base64url) = b8A3-7OMQrGQV8qPfAkIm40ylw_x7R7MS7xdpHM0ups
S (hex)       = 6fc037fbb38c42b19057ca8f7c09089b8d32970ff1ed1ecc4bbc5da47334ba9b
```

Request, AAD `aigile-seal-v1|req|a1b2c3d4e5f6`:

```
plaintext: {"t":1700000000000,"m":"GET","p":"/api/chats","b":null}
body:      {"n":"AAECAwQFBgcICQoL","c":"C-rMPhBge0WYfeqf1OqyeWA2CmLnmtGeNQtNEWbw8REEkdGFukqxDT2RcAu8Ik1K7ChJ1XsR44LY5CDa2EY7npuTGoY_kxQ"}
```

Response, AAD `aigile-seal-v1|res|a1b2c3d4e5f6`:

```
plaintext: {"s":200,"b":{"ok":true}}
body:      {"n":"AAECAwQFBgcICQoL","c":"C-rLPhBjfEWEb7iN3qGgJjskHDT4zY7BD6Bp_BG4--LDx5YkHkk7gEs"}
```

Checked by `app/src/test/java/com/smizflark/aigile/messenger/data/SealTest.kt` (javax.crypto) and independently by Node's `crypto.hkdfSync` + `aes-256-gcm`.
