package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Dictate
import org.junit.Assert.assertEquals
import org.junit.Test

class DictateTest {
    @Test fun emptyBoxStartsWithCapital() = assertEquals("Ship it tonight", Dictate.join("", "ship it tonight"))

    @Test fun addsAfterTypedTextWithOneSpace() = assertEquals("Hi team ship it", Dictate.join("Hi team", " ship it "))

    @Test fun noDoubleSpace() = assertEquals("Hi team ship it", Dictate.join("Hi team ", "ship it"))

    @Test fun nothingSaidKeepsText() = assertEquals("Hi", Dictate.join("Hi", "  "))
}
