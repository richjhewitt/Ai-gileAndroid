package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Find
import org.junit.Assert.assertEquals
import org.junit.Test

class FindTest {
    private val texts = listOf("Hello World", "nothing", "say HELLO again", "", "world")

    @Test fun ignoresCase() = assertEquals(listOf(0, 2), Find.matches(texts, "hello"))
    @Test fun keepsOrder() = assertEquals(listOf(0, 4), Find.matches(texts, "WORLD"))
    @Test fun oneLetterIsTooShort() = assertEquals(emptyList<Int>(), Find.matches(texts, "o"))
    @Test fun blankMatchesNothing() = assertEquals(emptyList<Int>(), Find.matches(texts, "   "))
    @Test fun spacesAtTheEndsAreIgnored() = assertEquals(listOf(1), Find.matches(texts, " nothing "))
    @Test fun noMatch() = assertEquals(emptyList<Int>(), Find.matches(texts, "zebra"))
    @Test fun noTexts() = assertEquals(emptyList<Int>(), Find.matches(emptyList(), "hello"))
}
