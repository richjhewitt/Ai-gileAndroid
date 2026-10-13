# Phone bridge

Lets the Messenger app reach the AI-gile board on this PC. Only the Messenger's routes get through, and only with the phone key.

- Start: `node bridge/bridge.js` (127.0.0.1:4330; `BRIDGE_PORT`, `AIGILE_PORT` to change)
- HTTPS from the phone: `tailscale serve --bg 4330` (tailnet only, never Funnel)
- Pairing link for the app: `node bridge/bridge.js pair https://<this-pc>.<tailnet>.ts.net`
- Lost phone: `node bridge/bridge.js new-key`, then pair again
- Key lives in `bridge/phone-key.local.json` (not in git)
- Tests: `node --test bridge/bridge.test.js` (fake board, spare port)
- Pictures (avatars, logos) need the key too: the app's image loader must send the same `Authorization` header
