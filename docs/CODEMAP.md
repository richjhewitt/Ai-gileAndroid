# Code map

Kotlin under `app/src/main/java/com/smizflark/aigile/messenger/` (paths below are relative to it).

## Root
- App.kt: Application class; reacts to the phone's language changing.
- MainActivity.kt: entry point (manifest has enableOnBackInvokedCallback = predictive back, AII-399: back is only OnBackPressedDispatcher / BackHandler, never onBackPressed or KEYCODE_BACK); handles pairing links and chats opened from an alert.

## data/ (pure logic, mostly unit tested)
- data/ChatInfo.kt: a chat's open cards and pictures, for the chat details sheet.
- data/ChatInfo.kt also lists non-picture files and guesses their MIME type (AII-197).
- data/ChatInfo.kt also counts a chat's messages, yours and the first day, for the About this chat line (AII-203).
- data/Links.kt: every web link in a chat (http links, [name](url), bare addresses), newest first, once each, at most 50, for the chat details Links list (AII-194).
- data/Crash.kt: keeps the last crash and turns it into a bug message for the team.
- data/Demo.kt: made-up chats that answer back, for trying the app with no board.
- data/Gif.kt: GIF links in a message (giphy, tenor, .gif) shown as moving pictures in the bubble, like the board (AII-188).
- data/LinkPreview.kt: link previews (AII-341): og:title / og:image (or <title>) of a message's first https link, fetched by the phone (4 s, 64 KB, in-memory cache); ui/LinkPreviewCard.kt draws the card under the bubble; Settings > Privacy switch, off by default (Store linkPreviews); shot LinkPreviewShotTest.
- data/Typing.kt: sendTyping, the one guard for telling a friend I am typing (AII-390); Settings > Privacy switch Show when I'm typing, on by default (Store showTyping); ui/ChatFriend.kt calls it.
- Hide message text on the lock screen (AII-401, Store hideOnLock, backed up): work/ChatNotifier.kt lockSafe() gives chat and summary alerts a public version (chat name + "New message", no actions); Settings > Privacy switch.
- data/Secure.kt: Block screenshots (AII-402, Store blockShots, backed up): FLAG_SECURE set or cleared by Secure.apply in MainActivity / BubbleActivity onStart, ChatWidgetConfig onCreate and at once when the Privacy switch is toggled; test SecureTest.
- data/PicLoad.kt: Load pictures Always / Wi-Fi only (AII-391, Store loadPictures, backed up): shouldLoad(setting, metered, cached, mine, tapped); ui/Pictures.kt shows a "Tap to load" box (tag pic-blocked) instead of fetching; picker LoadPicturesPick in ui/SettingsSections.kt; shot chat_tap_to_load_light.
- data/Emoji.kt: bigEmojiCount = how many emoji a message is when it is only 1 to 3 emoji (else 0); ui/ChatBubble.kt shows those big with no bubble (AII-364).
- data/Drafts.kt: half-typed replies, one per chat.
- data/Format.kt: B / I / </> buttons on selected words in the message box (AII-374): toggle wraps or unwraps **bold**, *italic*, `code` per line; ui/FormatRow.kt is the row, MessageField hands it a formatter while text is selected; Markdown.kt now shows *italic*.
- data/GoToDate.kt: Go to date in a chat (AII-193): which day to land on, and the picker's range (dialog: ui/GoToDateDialog.kt, with a First message button that picks the range start, AII-231).
- data/Filter.kt: the chips above the chats list (Drafts chip while a chat has a non-blank draft, AII-369; `Drafts.chatKeys`).
- data/HomeShortcut.kt: Add to home screen (AII-368, AII-376): pins a launcher shortcut that opens a chat (same EXTRA_CHAT route as alerts); menu items in ui/ChatTopBar.kt and the ui/ChatRow.kt hold menu.
- data/Forward.kt: forwarding a message to another chat.
- data/Mapper.kt: turns the board's answers into what the screens show.
- data/Models.kt: data classes for the board's chats and messages.
- data/News.kt: what is new since you last looked, and quiet hours.
- data/Remind.kt: chatKeys = chats with a reminder (AII-226). Remind me about a message (AII-201): in 1 hour, this evening 18:00 (hidden after), tomorrow 9:00, or Pick a time… (day then time, Pause.onDay, AII-225); after the time a Note dialog (Save / Skip) sets Reminder.note, shown as "📝 note" first in the alert and the Reminders list (Remind.withNote, AII-244); reminders kept in Store, fired by work/RemindWorker.kt as an alert that opens the chat at the message.
- Reminders on the minute (AII-396): data/RemindAlarm.kt (exact or WorkManager, ask once, which to reschedule) + work/RemindAlarmReceiver.kt (alarm, boot, app update); RemindWorker.schedule / cancel handle both paths; "Let reminders go off on time?" dialog = askExactOnce in ui/BubbleMenu.kt (Store.exactAsked).
- data/Remind.kt also: forMessage / label give the "🔔 18:00" line under a bubble that has a reminder (AII-251; read once per chat in ui/ChatScreen.kt; the Remind me menu and note dialog are in ui/BubbleMenu.kt).
- data/Silent.kt: Silent alerts for one chat (AII-207): its alerts show with no sound or buzz (setSilent in work/ChatNotifier.kt); Mute still wins; switch in chat details and the chat menu.
- data/ChatNames.kt: a chat's own name on this phone (AII-365): Rename / Reset name in ui/ChatDetails.kt; Store chatNames (backed up); ChatNames.apply puts it on ChatRef.name in Repo.chats and Store.cachedChats (the cache keeps the board's name, ChatRef.boardName for Reset); never sent to the board; shot chat_details_renamed_light.
- data/ChatColour.kt: a chat's colour (AII-375): Default + 6, light and dark tints, contrast check; Store chatColours; Colour row in ui/ChatDetails.kt tints your bubbles (ui/ChatBubble.kt) and the top bar (ui/ChatTopBar.kt); shots chatColourLight / chatColourDark.
- data/ChatSound.kt: a chat's own alert sound (AII-373): Store chatSounds, channel id per chat + sound (ChatNotifier.channelFor makes it and deletes the old one); Alert sound row in ui/ChatDetails.kt opens the ringtone picker; Mute and Silent alerts still win.
- data/EventGuess.kt: Add to calendar in the hold menu (AII-394): title = first line (80 chars), a simple day/time (EN + NL) becomes a 1-hour start time; item hidden when no app takes ACTION_INSERT (ui/BubbleMenu.kt, <queries> in the manifest).
- data/TranslateIntent.kt: Translate in the hold menu (AII-395): route = ACTION_TRANSLATE (API 29+) else PROCESS_TEXT with Google Translate, else item hidden.
- data/Now.kt: the app's clock (fixed in screenshot tests so shots never drift).
- data/SettingsExport.kt: settings export and restore sealed with a passphrase (PBKDF2 + AES-GCM); no board key (AII-349).
- data/SettingsFind.kt: Settings search (AII-403): accent- and case-insensitive word-start match; ui/SettingsScreen.kt wraps each row in SettingRow(labels) (ui/SettingsSections.kt), headings hide with no match; extra words kw_* in strings.
- data/Mute.kt: mute a chat for 1 hour, 8 hours or until unmuted (AII-157); timed mutes end by themselves.
- data/Outbox.kt: outbox state changes (retry / delete failed messages, change time) and the list of every waiting Send later message (AII-166).
- data/Pace.kt: pacing of sends / checks.
- data/Closed.kt: Close chat, hidden until a newer message (board's dmclosed list).
- data/Cleared.kt: Clear chat for me (AII-363): per chat a cleared-before time (Store cleared), messages up to it are hidden on this phone only (Cleared.visible, applied in ui/ChatRows.kt rows); Clear chat in the chat details and top bar menu, confirm dialog, Undo snackbar in ui/ChatScreen.kt.
- data/Pause.kt: pause alerts for a while.
- data/Pins.kt: pinned chats first; sortChats = Newest first / A to Z / Unread first (AII-241, AII-404, Settings, Store sortChats) applied before pins and muted-last.
- data/KeyShortcuts.kt: hardware keyboard shortcuts (AII-408): Ctrl+F search, Ctrl+N new message, Esc back, Alt+Down / Alt+Up next / previous chat; MainActivity.dispatchKeyEvent runs them, onProvideKeyboardShortcuts lists them (strings key_*); test KeyShortcutsTest.
- data/NextUnread.kt: which chat the Next unread button at the bottom of a chat goes to (AII-204); button in ui/ChatParts.kt.
- data/QuickReply.kt: replying from the notification.
- data/Quote.kt: quoting a message in a reply.
- data/Replies.kt: your own quick replies, one per line in Settings (AII-180).
- data/Recent.kt: recent searches, newest first, at most 5 (AII-155).
- data/Repo.kt: the one place screens and background checks get chats from.
- data/ScrollPlace.kt: remembers where you left each chat.
- data/Seal.kt: seals every board call end to end (AES-256-GCM, key from pairing; vectors in docs/SEALED.md).
- data/SafetyWatch.kt: a friend's safety words that change (AII-333): Store safetySeen, Repo learnSafety / safetyChanged / acceptSafety, deliver holds friend lines back; SafetyChangedBar in ui/ChatFriend.kt replaces the message box. Friend send / edit only go sealed (Source friendCall, AII-336).
- data/DmCrypto.kt: end-to-end sealing for direct messages (P-256 ECDH, AES-GCM, safety number); see docs/DM.md (AII-325).
- Reactions in friend chats (AII-326): FriendMsg.reacts to Bubble.reacts chips (Mapper.chips); Source.react toggles; quick emoji row in HoldMenu, chips in ui/ChatBubble.kt.
- Friend DMs (AII-324) use the board's friend DMs: Source edit / delete / typing / setChecked; reply goes by id (Pending.reply, Held.reply); Edit and Delete dialogs in ui/BubbleMenu.kt; safety words and We checked it in ui/ChatDetails.kt; "typing…" polled every 3 s in ui/ChatScreen.kt.
- Crop and rotate a picked picture (AII-385): tap its preview in a friend chat; data/PicEdit.kt (crop maths, PNG-or-JPEG choice), ui/PicEditor.kt (full-screen editor, EXIF-upright load, writes an edited copy beside the kept picture); shot pic_editor_dark.
- Voice notes in friend chats (AII-331): data/Voice.kt (length label, too-short rule, audio check, file names); hold the mic in ui/ChatComposer.kt to record, slide up ~80dp to lock and record hands-free with Delete/Send (AII-384, rules in data/VoiceLock.kt) (ui/VoiceRecord.kt, MediaRecorder, RECORD_AUDIO), ui/VoicePlayer.kt plays them in the bubble (one at a time; speed chip 1x/1.5x/2x, AII-378, kept in Store.voiceSpeed, rules in Voice.nextSpeed); ui/ChatFriend.kt holds the other friend-DM helpers.
- data/Search.kt: search results (chats and messages); Pictures / Links / Files (AII-267, `files`, non-picture files as in ChatInfo.isFile; AII-270 snippet = "📎 name" via `fileNames`) / Starred / Last 7 days (AII-258, `last7`) / From a person (AII-386, `from`; `authors()` = You first, then names A to Z; `YOU` = own messages) (AII-246, `starred` = the stars) filters list matching messages newest first (AII-192); snippets show markdown as read and carry the found-word ranges to bold (AII-195).
- data/Share.kt: text shared in from other apps. Pictures shared in (AII-338): MainActivity sharedImages / sharedPics, New message shows them (PickedPictures) and sends through the outbox.
- data/ShareTargets.kt: your top 4 chats as direct targets in the share sheet (AII-398): ui/Shortcuts.kt pushes them as long-lived "chat:<key>" shortcuts with the SHARE_TARGET category (res/xml/shortcuts.xml share-target on MainActivity); a share with EXTRA_SHORTCUT_ID opens New message with that chat picked (MainActivity sharedTo -> NewMessageScreen preselect).
- data/Source.kt: chat source, the real board or the demo.
- data/Selection.kt: select several messages (AII-371): toggle and the picked messages' text, oldest first in Transcript style; hold menu Select (ui/BubbleMenu.kt), tinted picked bubbles (ui/ChatBubble.kt), SelectTopBar with Copy / Share / Star (ui/ChatTopBar.kt), state in ui/ChatScreen.kt; shot selectModeLight.
- data/Stars.kt: starred messages (AII-138), listed in the chat details; lead chats kept in step with the board (AII-141, Repo syncStars; Starred refreshes them on open, AII-162).
- data/PinnedMsgs.kt: pinned messages (AII-339), this phone only (Store pinnedMsgs); Pin / Unpin in the bubble hold menu, the pinned bar under the chat's top bar (PinnedBar in ui/ChatParts.kt; tap jumps and cycles "1 of N", X unpins). Not Pins.kt, which is pinned chats.
- data/SavedPictures.kt: size of the saved pictures and which files Clear may delete, never ones waiting in the outbox (AII-215). Also per chat (AII-392): chatKeys = the chat's board / web pictures as Coil disk-cache keys (the picture's address), usage = count and bytes of those saved, removableKeys; Storage line and Clear pictures button in ui/ChatDetails.kt.
- data/Store.kt: pairing in the "messenger" prefs (not backed up), settings in the "settings" prefs (backed up, AII-81); extends StoreFiles.
- data/StoreFiles.kt: Store's saved files: drafts, closed / cleared / starred / pinned / reminders / seen / told lists, the chat cache and the outbox (AII-366 split).
- data/Vault.kt: the phone key sealed with the Android Keystore.
- data/Transcript.kt: a whole chat as plain text, for Share this chat (AII-139) and Save as file (AII-229, safe file name here; saveTextFile in ui/ChatDetails.kt writes to Downloads).
- data/UndoSend.kt: a sent message held 5 or 10 seconds (Settings, AII-233) with an Undo bar (AII-202); used by ui/ChatScreen.kt (helpers in ui/ChatUndoSend.kt).
- data/Swipe.kt: what a swipe on a chat row does; choices include Remind me in 1 hour (AII-257).
- data/WhatsNew.kt: What's new bar (AII-268): top of the chats list on the first open after the version code changes (never on a new install); tap opens About, X closes; last seen version in Store.
- data/Tips.kt: demo welcome tips (AII-276): three tips at the top of the chats list until Got it; Store tipsSeen. The Connect screen leads with a big Try the demo button (AII-275).
- data/Update.kt: 'New version' bar: latest GitHub release (tag build-N = versionCode), checked once a day.

## ui/ (Compose screens)
- ui/CardDetails.kt: card detail sheet and the board's column names.
- ui/CardDetails.kt also: Accept / Send back (reason dialog) on a card in For you to test (AII-273; Source.verdict -> board POST .../cards/<id>/verdict); the demo card TUN-7 waits in For you to test (AII-274).
- ui/ChatDetails.kt: chat details sheet (tap the chat name): open cards, this chat's reminders (AII-223, tap jumps to the message, X cancels via cancelReminder in ui/RemindersScreen.kt), pictures, links (AII-194), Mute and Silent alerts side by side (AII-245), and Pin; section headings show their counts (AII-252).
- ui/ChatScreen.kt: the chat screen itself (list, find, scrolling).
- ui/ChatScreenParts.kt: small pieces of ChatScreen: KeepDraft, WatchWorking, rememberFloatingDay, ErrorLine, ClearChatHost (AII-366 split).
- ui/ChatGestures.kt: the pinch-to-zoom text size (AII-259) and the next unread chat lookup (AII-204) for ui/ChatScreen.kt.
- ui/ChatScroll.kt: remembered scroll place and New messages opening (AII-72, AII-102), Go to date scroll (AII-193), day and new-line rows, share/save the chat as text (AII-139, AII-229).
- ui/ChatPictures.kt: the photo picker and pasting a copied picture into the message box (AII-27, AII-56). A GIF or sticker from the keyboard (AII-370): ui/MessageField.kt (state-based field + contentReceiver) -> receivePictures; data/PasteContent.kt decides friend vs team.
- ui/ChatUndoSend.kt: Undo send hold helpers (AII-202): the Undo bar and sending a held message on leaving the chat.
- ui/ChatTopBar.kt: the chat's top bar (name, 'working on', find, Mute; menu: Go to date, My last message AII-242).
- ui/ChatRows.kt: builds the chat's rows (days, card headers, messages, waiting sends) and counts unread below.
- ui/ChatBubble.kt: one message bubble (copy, reply, forward, options, retry; long ones fold with Read more, AII-184).
- ui/BubbleMenu.kt: the bubble hold menu (Remind me, Reply, Forward, Star, Copy, Copy as quote, Select, links, Share, Read aloud) and the Remind me time menu and note dialog (AII-201, AII-244).
- data/MessageInfo.kt + MessageInfoDialog in ui/BubbleMenu.kt: hold a message, Info shows the exact sent and edited times (AII-372); the board keeps no per-message read time.
- ui/ChatComposer.kt: the message input with pictures, reply, dictation and quick replies (AII-179); hold a quick reply to edit them in Settings (AII-255).
- ui/ChatComposer.kt messageHint: with pictures picked the box says "Add a caption"; its text goes in the same message as the pictures (AII-379).
- ui/ChatParts.kt: chat Row model, find bar, jump-to-newest, card header, quote bar, forward dialog.
- ui/ChatsScreen.kt: every chat in one list.
- ui/TwoPane.kt: wide screens (840dp, or 600dp in landscape) show the chats list (360dp) and the open chat side by side, "Pick a chat" when none (AII-350); MainActivity keeps the selected chat across turning and folding.
- ui/ChatRow.kt: one row of the chats list (swipe, hold menu, badges, a bell when the chat has a reminder AII-226; Remind me in 1 hour on its newest cached message, AII-240) and its preview, draft and unsent labels.
- ui/ConnectScreen.kt: connect the phone to a board, or try the demo; after Sign out (route connect?out=true) Scan QR code comes first (AII-289).
- ui/Dictate.kt: voice to text with the phone's recogniser.
- ui/Find.kt: find in a chat (pure).
- ui/Lock.kt: app lock with fingerprint, face or PIN; Lock after straight away / 1 / 5 minutes (AII-234, Store lockAfter, not backed up like lock).
- ui/Markdown.kt: renders message markdown ([name](link), bare web addresses and • bullets like the board, AII-187; ⧉ after a **Label:** value copies the value, AII-191).
- ui/MuteFor.kt: the Mute for how long dialog (AII-157), used by the chats list, chat menu and chat details.
- ui/NewMessageScreen.kt: a new message to any team.
- ui/Parts.kt: small shared UI helpers (colour parsing and similar).
- ui/ReadAloud.kt: Read aloud from the hold menu with the phone's own voice (AII-185); leaving a chat stops it.
- ui/Files.kt: tap a file to download it to the cache and open it with another app (AII-197); used in bubbles and the chat details Files list.
- ui/Pictures.kt: loads a message's pictures (MessagePictures, PickedPictures, PictureNote).
- ui/PictureViewer.kt: PictureHost shows one full screen over the whole app (bars hidden, swipe, zoom).
- ui/PictureActions.kt: Save and Share for a picture (AII-95, AII-96, AII-104).
- ui/CopiedPicture.kt: watches the clipboard for a copied picture (AII-56).
- ui/SearchScreen.kt: search across all chats, with Pictures, Links, Files, Starred, Last 7 days and From (person menu) chips (AII-192, AII-267, AII-246, AII-258, AII-386).
- ui/SettingsScreen.kt: settings.
- ui/RemindersScreen.kt: every reminder under Today, Tomorrow, Later headings (AII-269, Remind.groups), soonest first in each (AII-209, a Reminders row in Settings when there are any); tap one, its chat opens at the message (AII-247); hold one or tap X to Cancel (also cancels its WorkManager job remind-<id>).
- ui/SettingsScreen.kt: settings, in sections Connection, Look, Chats, Alerts, Privacy, Storage, About (AII-232); the small pieces (SectionHeader, Toggle, Stepper, SwipePick, ChangeBoard) are in ui/SettingsSections.kt.
- ui/AboutScreen.kt: About, from the bottom of Settings (AII-220): app name, version, build number and a hand-written list of open-source licences (LICENCES, keep in step with app/build.gradle.kts); tap one to open its licence page. What's new: WHATS_NEW (string to build), whatsNewShown keeps the 3 newest builds (AII-271).
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
- widget/ChatWidget.kt + ChatWidgetConfig.kt + ChatWidgetLogic.kt: One chat widget (AII-380): pick a chat when adding it (per appWidgetId, ChatWidgetPicks), shows its newest 3 messages from the cache, tap opens that chat; refreshed by LatestWidget.refresh; lines / trimming / id mapping in ChatWidgetLogic (ChatWidgetLogicTest).

## work/ (background)
- work/ChatNotifier.kt: builds and shows message alerts (quick replies as chips on Reply, AII-181); the grouped summary has Mark all as read (AII-387, ReadReceiver.ACTION_ALL over alertingChats, chat key kept in each alert's extras).
- work/CheckWorker.kt: looks for new messages about every 15 minutes.
- work/PauseTile.kt: Quick Settings tile Pause alerts (AII-367): tap pauses 1 hour, tap again turns alerts back on; shows Until HH:MM on Android 10+; logic in PauseTileLogic.
- work/NewMessageTile.kt: Quick Settings tile New message (AII-400): opens the app straight on New message, the same intent as the icon menu.
- work/ReadReceiver.kt: the Mark as read button on an alert.
- work/ReplyReceiver.kt: the Reply button on an alert.
- work/SendWorker.kt: sends queued messages from the outbox, and Send later ones at their time (AII-146).

## Other
- tools/: release.ps1 (release build script) and emulator/ (emu.ps1 shared helpers and boot; smoke.ps1 demo smoke; smoke-board.ps1 real test board smoke, AII-318).
- tools/testboard/testboard.mjs: throwaway test board with demo chats (start, check, phone over Tailscale with a pairing QR, emulator = pairing link plus a Host-rewriting proxy for 10.0.2.2, stop; AIGILE_TESTBOARD_STATE runs a second one).
- app/src/debug: debug build only, plain http to 10.0.2.2 for the board smoke (AII-318).
- docs/shots: reference screenshots (Roborazzi) for each screen and theme.
- test/.../screens: ShotBase.kt (demo data, fixed clock, shot helpers); ChatsListShotTest (chats list), ChatShotTest (one chat), OtherShotTest (Connect, Starred, New message, Settings, black, Dutch); LongMessageShotTest (Read more, AII-184); ReadAloudShotTest (AII-185); LinksShotTest (named links, AII-187).

## res/xml backup (AII-81)
- res/xml/backup_rules.xml and res/xml/data_extraction_rules.xml: backup takes only settings.xml (look, text size, pins, mutes, quiet hours); keys, board address, drafts and outbox stay out.
- data/ChatHeads.kt: chat heads (AII-381): Settings > Alerts switch (Store chatHeads, off, API 30+) gives each chat alert BubbleMetadata and a long-lived shortcut "chat:<key>" (not HomeShortcut's "home:"); the bubble opens BubbleActivity (one chat; MainActivity can't take documentLaunchMode=always); channels setAllowBubbles.
