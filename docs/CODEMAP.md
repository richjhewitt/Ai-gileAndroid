# Code map

Kotlin under `app/src/main/java/com/smizflark/aigile/messenger/` (paths below are relative to it).

## Root
- App.kt: Application class; reacts to the phone's language changing.
- MainActivity.kt: entry point; handles pairing links and chats opened from an alert.

## data/ (pure logic, mostly unit tested)
- data/ChatInfo.kt: a chat's open cards and pictures, for the chat details sheet.
- data/ChatInfo.kt also lists non-picture files and guesses their MIME type (AII-197).
- data/ChatInfo.kt also counts a chat's messages, yours and the first day, for the About this chat line (AII-203).
- data/Links.kt: every web link in a chat (http links, [name](url), bare addresses), newest first, once each, at most 50, for the chat details Links list (AII-194).
- data/Crash.kt: keeps the last crash and turns it into a bug message for the team.
- data/Demo.kt: made-up chats that answer back, for trying the app with no board.
- data/Gif.kt: GIF links in a message (giphy, tenor, .gif) shown as moving pictures in the bubble, like the board (AII-188).
- data/Drafts.kt: half-typed replies, one per chat.
- data/GoToDate.kt: Go to date in a chat (AII-193): which day to land on, and the picker's range (dialog: ui/GoToDateDialog.kt, with a First message button that picks the range start, AII-231).
- data/Filter.kt: the chips above the chats list.
- data/Forward.kt: forwarding a message to another chat.
- data/Mapper.kt: turns the board's answers into what the screens show.
- data/Models.kt: data classes for the board's chats and messages.
- data/News.kt: what is new since you last looked, and quiet hours.
- data/Remind.kt: chatKeys = chats with a reminder (AII-226). Remind me about a message (AII-201): in 1 hour, this evening 18:00 (hidden after), tomorrow 9:00, or Pick a time… (day then time, Pause.onDay, AII-225); after the time a Note dialog (Save / Skip) sets Reminder.note, shown as "📝 note" first in the alert and the Reminders list (Remind.withNote, AII-244); reminders kept in Store, fired by work/RemindWorker.kt as an alert that opens the chat at the message.
- data/Remind.kt also: forMessage / label give the "🔔 18:00" line under a bubble that has a reminder (AII-251; read once per chat in ui/ChatScreen.kt; the Remind me menu and note dialog are in ui/BubbleMenu.kt).
- data/Silent.kt: Silent alerts for one chat (AII-207): its alerts show with no sound or buzz (setSilent in work/ChatNotifier.kt); Mute still wins; switch in chat details and the chat menu.
- data/Now.kt: the app's clock (fixed in screenshot tests so shots never drift).
- data/Mute.kt: mute a chat for 1 hour, 8 hours or until unmuted (AII-157); timed mutes end by themselves.
- data/Outbox.kt: outbox state changes (retry / delete failed messages, change time) and the list of every waiting Send later message (AII-166).
- data/Pace.kt: pacing of sends / checks.
- data/Closed.kt: Close chat, hidden until a newer message (board's dmclosed list).
- data/Pause.kt: pause alerts for a while.
- data/Pins.kt: pinned chats first; sortChats = Newest first / A to Z (AII-241, Settings, Store sortChats) applied before pins and muted-last.
- data/NextUnread.kt: which chat the Next unread button at the bottom of a chat goes to (AII-204); button in ui/ChatParts.kt.
- data/QuickReply.kt: replying from the notification.
- data/Quote.kt: quoting a message in a reply.
- data/Replies.kt: your own quick replies, one per line in Settings (AII-180).
- data/Recent.kt: recent searches, newest first, at most 5 (AII-155).
- data/Repo.kt: the one place screens and background checks get chats from.
- data/ScrollPlace.kt: remembers where you left each chat.
- data/Seal.kt: seals every board call end to end (AES-256-GCM, key from pairing; vectors in docs/SEALED.md).
- data/Search.kt: search results (chats and messages); Pictures / Links / Files (AII-267, `files`, non-picture files as in ChatInfo.isFile) / Starred / Last 7 days (AII-258, `last7`) (AII-246, `starred` = the stars) filters list matching messages newest first (AII-192); snippets show markdown as read and carry the found-word ranges to bold (AII-195).
- data/Share.kt: text shared in from other apps.
- data/Source.kt: chat source, the real board or the demo.
- data/Stars.kt: starred messages (AII-138), listed in the chat details; lead chats kept in step with the board (AII-141, Repo syncStars; Starred refreshes them on open, AII-162).
- data/SavedPictures.kt: size of the saved pictures and which files Clear may delete, never ones waiting in the outbox (AII-215).
- data/Store.kt: on-disk storage: pairing in the "messenger" prefs (not backed up), settings in the "settings" prefs (backed up, AII-81), drafts and cache.
- data/Transcript.kt: a whole chat as plain text, for Share this chat (AII-139) and Save as file (AII-229, safe file name here; saveTextFile in ui/ChatDetails.kt writes to Downloads).
- data/UndoSend.kt: a sent message held 5 or 10 seconds (Settings, AII-233) with an Undo bar (AII-202); used by ui/ChatScreen.kt (helpers in ui/ChatUndoSend.kt).
- data/Swipe.kt: what a swipe on a chat row does; choices include Remind me in 1 hour (AII-257).
- data/WhatsNew.kt: What's new bar (AII-268): top of the chats list on the first open after the version code changes (never on a new install); tap opens About, X closes; last seen version in Store.
- data/Update.kt: 'New version' bar: latest GitHub release (tag build-N = versionCode), checked once a day.

## ui/ (Compose screens)
- ui/CardDetails.kt: card detail sheet and the board's column names.
- ui/CardDetails.kt also: Accept / Send back (reason dialog) on a card in For you to test (AII-273; Source.verdict -> board POST .../cards/<id>/verdict); the demo card TUN-7 waits in For you to test (AII-274).
- ui/ChatDetails.kt: chat details sheet (tap the chat name): open cards, this chat's reminders (AII-223, tap jumps to the message, X cancels via cancelReminder in ui/RemindersScreen.kt), pictures, links (AII-194), Mute and Silent alerts side by side (AII-245), and Pin; section headings show their counts (AII-252).
- ui/ChatScreen.kt: the chat screen itself (list, find, scrolling).
- ui/ChatGestures.kt: the pinch-to-zoom text size (AII-259) and the next unread chat lookup (AII-204) for ui/ChatScreen.kt.
- ui/ChatScroll.kt: remembered scroll place and New messages opening (AII-72, AII-102), Go to date scroll (AII-193), day and new-line rows, share/save the chat as text (AII-139, AII-229).
- ui/ChatPictures.kt: the photo picker and pasting a copied picture into the message box (AII-27, AII-56).
- ui/ChatUndoSend.kt: Undo send hold helpers (AII-202): the Undo bar and sending a held message on leaving the chat.
- ui/ChatTopBar.kt: the chat's top bar (name, 'working on', find, Mute; menu: Go to date, My last message AII-242).
- ui/ChatRows.kt: builds the chat's rows (days, card headers, messages, waiting sends) and counts unread below.
- ui/ChatBubble.kt: one message bubble (copy, reply, forward, options, retry; long ones fold with Read more, AII-184).
- ui/BubbleMenu.kt: the bubble hold menu (Remind me, Reply, Forward, Star, Copy, Copy as quote, Select, links, Share, Read aloud) and the Remind me time menu and note dialog (AII-201, AII-244).
- ui/ChatComposer.kt: the message input with pictures, reply, dictation and quick replies (AII-179); hold a quick reply to edit them in Settings (AII-255).
- ui/ChatParts.kt: chat Row model, find bar, jump-to-newest, card header, quote bar, forward dialog.
- ui/ChatsScreen.kt: every chat in one list.
- ui/ChatRow.kt: one row of the chats list (swipe, hold menu, badges, a bell when the chat has a reminder AII-226; Remind me in 1 hour on its newest cached message, AII-240) and its preview, draft and unsent labels.
- ui/ConnectScreen.kt: connect the phone to a board, or try the demo.
- ui/Dictate.kt: voice to text with the phone's recogniser.
- ui/Find.kt: find in a chat (pure).
- ui/Lock.kt: app lock with fingerprint, face or PIN; Lock after straight away / 1 / 5 minutes (AII-234, Store lockAfter, not backed up like lock).
- ui/Markdown.kt: renders message markdown ([name](link), bare web addresses and • bullets like the board, AII-187; ⧉ after a **Label:** value copies the value, AII-191).
- ui/MuteFor.kt: the Mute for how long dialog (AII-157), used by the chats list, chat menu and chat details.
- ui/NewMessageScreen.kt: a new message to any team.
- ui/Parts.kt: small shared UI helpers (colour parsing and similar).
- ui/ReadAloud.kt: Read aloud from the hold menu with the phone's own voice (AII-185); leaving a chat stops it.
- ui/Files.kt: tap a file to download it to the cache and open it with another app (AII-197); used in bubbles and the chat details Files list.
- ui/Pictures.kt: loads a message's pictures; PictureHost shows one full screen over the whole app (bars hidden, swipe, zoom).
- ui/SearchScreen.kt: search across all chats, with Pictures, Links, Files, Starred and Last 7 days chips (AII-192, AII-267, AII-246, AII-258).
- ui/SettingsScreen.kt: settings.
- ui/RemindersScreen.kt: every reminder under Today, Tomorrow, Later headings (AII-269, Remind.groups), soonest first in each (AII-209, a Reminders row in Settings when there are any); tap one, its chat opens at the message (AII-247); hold one or tap X to Cancel (also cancels its WorkManager job remind-<id>).
- ui/SettingsScreen.kt: settings, in sections Connection, Look, Chats, Alerts, Privacy, Storage, About (AII-232); the small pieces (SectionHeader, Toggle, Stepper, SwipePick, ChangeBoard) are in ui/SettingsSections.kt.
- ui/AboutScreen.kt: About, from the bottom of Settings (AII-220): app name, version, build number and a hand-written list of open-source licences (LICENCES, keep in step with app/build.gradle.kts); tap one to open its licence page.
- ui/StarredScreen.kt: every starred message from all chats (AII-140, the ★ on the chats list); Share all in the top bar sends what is listed as text via Stars.asText (AII-243); tap one, its chat opens at it; chat-name chips (2+ chats, AII-228) show one chat's stars (Stars.onlyChat).
- ui/WaitingScreen.kt: every Send later message from all chats, soonest first (AII-166, See every waiting message under the Scheduled chip); tap one, its chat opens at it.
- ui/StarredScreen.kt: every starred message from all chats (AII-140, the ★ on the chats list); tap one, its chat opens at it; chat-name chips (2+ chats, AII-228) show one chat's stars (Stars.onlyChat).
- ui/WaitingScreen.kt: every Send later message from all chats, soonest first (AII-166, See every waiting message under the Scheduled chip); tap one, its chat opens at it; swipe one away to delete it with an Undo snackbar (AII-253, the real file delete waits for the snackbar to time out).
- ui/Shortcuts.kt: app icon shortcuts (New message, recent chats).
- ui/Theme.kt: colours and Material theme.

## widget/
- work/RemindReceiver.kt: the Remind me in 1 hour button on message alerts (AII-210) and Again in 1 hour on the reminder alert itself (AII-224, extra remind_notify = that alert's id, which it dismisses); the alert keeps its newest message's key and text in its extras (work/ChatNotifier.kt).
- widget/LatestWidget.kt: home screen widget with the newest chats.
- widget/LatestWidget.kt: home screen widget with the newest chats. Colours follow the app's look (AII-230): WidgetLook picks light, dark or black backgrounds (res/drawable/widget_bg_*.xml); Settings refreshes it on a look change.

## work/ (background)
- work/ChatNotifier.kt: builds and shows message alerts (quick replies as chips on Reply, AII-181).
- work/CheckWorker.kt: looks for new messages about every 15 minutes.
- work/ReadReceiver.kt: the Mark as read button on an alert.
- work/ReplyReceiver.kt: the Reply button on an alert.
- work/SendWorker.kt: sends queued messages from the outbox, and Send later ones at their time (AII-146).

## Other
- tools/: release.ps1 (release build script) and emulator/ (smoke.ps1 emulator helpers).
- tools/testboard/testboard.mjs: throwaway test board with demo chats (start, check, phone over Tailscale with a pairing QR, stop).
- docs/shots: reference screenshots (Roborazzi) for each screen and theme.
- test/.../screens: ShotBase.kt (demo data, fixed clock, shot helpers); ChatsListShotTest (chats list), ChatShotTest (one chat), OtherShotTest (Connect, Starred, New message, Settings, black, Dutch); LongMessageShotTest (Read more, AII-184); ReadAloudShotTest (AII-185); LinksShotTest (named links, AII-187).

## res/xml backup (AII-81)
- res/xml/backup_rules.xml and res/xml/data_extraction_rules.xml: backup takes only settings.xml (look, text size, pins, mutes, quiet hours); keys, board address, drafts and outbox stay out.
