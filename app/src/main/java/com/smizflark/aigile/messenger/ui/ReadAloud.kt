package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Hold a message, Read aloud: the phone's own voice reads it (AII-185). One voice for the app; leaving a chat stops it. */
internal object ReadAloud {
    /** The message being read, or null. */
    var speaking by mutableStateOf<String?>(null)
        private set
    private var tts: TextToSpeech? = null
    private var ready = false
    private var waiting: Pair<String, String>? = null

    fun speak(ctx: Context, key: String, text: String) {
        speaking = key
        val t = tts
        if (t != null && ready) { say(t, key, text); return }
        waiting = key to text
        if (t == null) tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            val w = waiting; waiting = null
            val now = tts
            if (ready && now != null && w != null) say(now, w.first, w.second) else if (!ready) speaking = null
        }.also { it.setOnUtteranceProgressListener(Done) }
    }

    fun stop() {
        waiting = null
        tts?.stop()
        speaking = null
    }

    private fun say(t: TextToSpeech, key: String, text: String) {
        // Long messages go in pieces under the engine's limit, one after another.
        val max = TextToSpeech.getMaxSpeechInputLength() - 1
        text.chunked(max).forEachIndexed { i, part -> t.speak(part, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "$key#$i") }
    }

    private object Done : UtteranceProgressListener() {
        override fun onStart(id: String?) {}
        override fun onDone(id: String?) { finished(id) }
        @Deprecated("Deprecated in Java") override fun onError(id: String?) { finished(id) }
        override fun onError(id: String?, errorCode: Int) { finished(id) }
        private fun finished(id: String?) {
            val t = tts ?: return
            if (id?.substringBefore('#') == speaking && !t.isSpeaking) speaking = null
        }
    }
}
