package com.smizflark.aigile.messenger.data

import com.smizflark.aigile.messenger.data.TranslateIntent.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateIntentTest {
    @Test fun translateWins() = assertEquals(Route.TRANSLATE, TranslateIntent.route(33, true, true))
    @Test fun oldPhoneUsesProcessText() = assertEquals(Route.PROCESS_TEXT, TranslateIntent.route(28, true, true))
    @Test fun fallbackWhenNoTranslate() = assertEquals(Route.PROCESS_TEXT, TranslateIntent.route(33, false, true))
    @Test fun nothingResolves() = assertEquals(Route.NONE, TranslateIntent.route(33, false, false))
}
