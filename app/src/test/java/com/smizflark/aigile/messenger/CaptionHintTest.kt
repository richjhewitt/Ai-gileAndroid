package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.messageHint
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionHintTest {
    @Test fun picturesPickedAsksForCaption() {
        assertEquals(R.string.add_caption, messageHint(watchman = false, hasPics = true))
        assertEquals(R.string.add_caption, messageHint(watchman = true, hasPics = true))
    }
    @Test fun noPicturesKeepsTheUsualHint() {
        assertEquals(R.string.message, messageHint(false, false))
        assertEquals(R.string.message_watchman, messageHint(true, false))
    }
}
