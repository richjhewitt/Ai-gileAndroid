# Code map

Kotlin under `app/src/main/java/com/smizflark/aigile/messenger/` (paths below are relative to it).

## Root
- App.kt: Application class; reacts to the phone's language changing.
- MainActivity.kt: entry point; handles pairing links and chats opened from an alert.

## data/ (pure logic, mostly unit tested)
- data/ChatInfo.kt: a chat's open cards and pictures, for the chat details sheet.
- data/Crash.kt: keeps the last crash and turns it into a bug message for the team.
- data/Demo.kt: made-up chats that answer back, for trying the app with no board.
- data/Drafts.kt: half-typed replies, one per chat.
- data/Filter.kt: the chips above the chats list.
- data/Forward.kt: forwarding a message to another chat.
- data/Mapper.kt: turns the board's answers into what the screens show.
- data/Models.kt: data classes for the board's chats and messages.
- data/News.kt: what is new since you last looked, and quiet hours.
- data/Now.kt: the app's clock (fixed in screenshot tests so shots never drift).
- data/Outbox.kt: outbox state changes (retry / delete failed messages).
- data/Pace.kt: pacing of sends / checks.
- data/Pause.kt: pause alerts for a while.
- data/Pins.kt: pinned chats first.
- data/QuickReply.kt: replying from the notification.
- data/Quote.kt: quoting a message in a reply.
- data/Repo.kt: the one place screens and background checks get chats from.
- data/ScrollPlace.kt: remembers where you left each chat.
- data/Seal.kt: seals every board call end to end (AES-256-GCM, key from pairing; vectors in docs/SEALED.md).
- data/Search.kt: search results (chats and messages).
- data/Share.kt: text shared in from other apps.
- data/Source.kt: chat source, the real board or the demo.
- data/Store.kt: on-disk storage: pairing in the "messenger" prefs (not backed up), settings in the "settings" prefs (backed up, AII-81), drafts and cache.
- data/Swipe.kt: what a swipe on a chat row does.

## ui/ (Compose screens)
- ui/CardDetails.kt: card detail sheet and the board's column names.
- ui/ChatDetails.kt: chat details sheet (tap the chat name): open cards, pictures, Mute and Pin.
- ui/ChatScreen.kt: the chat screen itself (list, top bar, find, scrolling).
- ui/ChatBubble.kt: one message bubble (copy, reply, forward, options, retry).
- ui/ChatComposer.kt: the message input with pictures, reply and dictation.
- ui/ChatParts.kt: chat Row model, find bar, jump-to-newest, card header, quote bar, forward dialog.
- ui/ChatsScreen.kt: every chat in one list.
- ui/ConnectScreen.kt: connect the phone to a board, or try the demo.
- ui/Dictate.kt: voice to text with the phone's recogniser.
- ui/Find.kt: find in a chat (pure).
- ui/Lock.kt: app lock with fingerprint, face or PIN.
- ui/Markdown.kt: renders message markdown.
- ui/NewMessageScreen.kt: a new message to any team.
- ui/Parts.kt: small shared UI helpers (colour parsing and similar).
- ui/Pictures.kt: loads a message's pictures.
- ui/SearchScreen.kt: search across all chats.
- ui/SettingsScreen.kt: settings.
- ui/Shortcuts.kt: app icon shortcuts (New message, recent chats).
- ui/Theme.kt: colours and Material theme.

## widget/
- widget/LatestWidget.kt: home screen widget with the newest chats.

## work/ (background)
- work/ChatNotifier.kt: builds and shows message alerts.
- work/CheckWorker.kt: looks for new messages about every 15 minutes.
- work/ReplyReceiver.kt: the Reply button on an alert.
- work/SendWorker.kt: sends queued messages from the outbox.

## Other
- tools/: release.ps1 (release build script) and emulator/ (emulator helpers).
- docs/shots: reference screenshots (Roborazzi) for each screen and theme.

## res/xml backup (AII-81)
- res/xml/backup_rules.xml and res/xml/data_extraction_rules.xml: backup takes only settings.xml (look, text size, pins, mutes, quiet hours); keys, board address, drafts and outbox stay out.
