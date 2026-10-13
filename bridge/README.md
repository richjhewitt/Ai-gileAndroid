# Phone bridge

Lets the Messenger app reach the board on this PC. The board only answers to localhost; the bridge checks the phone key, lets through only the Messenger routes, and passes calls on as localhost.

- Start: `node bridge/bridge.js` (listens on 127.0.0.1:4322; the first run makes the phone key in `bridge/key.local.txt`, kept out of git)
- HTTPS for the phone: `tailscale serve --bg 4322` gives `https://<pc>.<tailnet>.ts.net`, reachable only on Rich's tailnet
- Start it with `BRIDGE_URL=https://<pc>.<tailnet>.ts.net` so the printed pairing link has the right address
- Settings: `BRIDGE_PORT` (4322), `BRIDGE_HOST` (127.0.0.1), `AIGILE_PORT` (4321)
- New key: delete `bridge/key.local.txt` and restart; the phone must pair again
- Tests: `node --test bridge/bridge.test.js`
