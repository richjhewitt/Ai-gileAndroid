package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.bigEmojiCount
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiTest {
    @Test fun oneToThree() {
        assertEquals(1, bigEmojiCount("😀"))
        assertEquals(2, bigEmojiCount("😀 😂"))
        assertEquals(3, bigEmojiCount(" 😀😂👍\n"))
    }
    @Test fun fourIsTooMany() = assertEquals(0, bigEmojiCount("😀😀😀😀"))
    @Test fun sequencesCountOnce() {
        assertEquals(1, bigEmojiCount("👨‍👩‍👧"))
        assertEquals(1, bigEmojiCount("👍🏽"))
        assertEquals(1, bigEmojiCount("🇳🇱"))
        assertEquals(2, bigEmojiCount("🇳🇱🇧🇪"))
        assertEquals(1, bigEmojiCount("❤️"))
        assertEquals(1, bigEmojiCount("❤"))
        assertEquals(1, bigEmojiCount("1️⃣"))
    }
    @Test fun notEmojiOnly() {
        assertEquals(0, bigEmojiCount(""))
        assertEquals(0, bigEmojiCount("  "))
        assertEquals(0, bigEmojiCount("hi 😀"))
        assertEquals(0, bigEmojiCount("😀!"))
        assertEquals(0, bigEmojiCount("123"))
        assertEquals(0, bigEmojiCount("7"))
        assertEquals(0, bigEmojiCount("#"))
        assertEquals(0, bigEmojiCount("©"))
    }
}
