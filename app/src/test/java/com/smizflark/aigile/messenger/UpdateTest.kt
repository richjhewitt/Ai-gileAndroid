package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Release
import com.smizflark.aigile.messenger.data.Update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-132: the 'New version' bar. */
class UpdateTest {
    @Test fun buildIsTheNumberAtTheEndOfTheTag() {
        assertEquals(322, Update.build("build-322"))
        assertEquals(0, Update.build("v0.1.0-beta"))
        assertEquals(0, Update.build(""))
    }

    @Test fun parsePicksTheApk() {
        val body = """{"tag_name":"build-40","html_url":"https://github.com/r/a/releases/tag/build-40","assets":[
            {"name":"notes.txt","browser_download_url":"https://x/notes.txt"},
            {"name":"aigile-messenger.apk","browser_download_url":"https://x/aigile-messenger.apk"}]}"""
        assertEquals(Release(40, "https://x/aigile-messenger.apk"), Update.parse(body))
    }

    @Test fun parseFallsBackToTheReleasePage() {
        assertEquals(Release(41, "https://g/build-41"), Update.parse("""{"tag_name":"build-41","html_url":"https://g/build-41","assets":[]}"""))
    }

    @Test fun parseIgnoresWhatIsNotARelease() {
        assertNull(Update.parse("""{"message":"Not Found"}"""))
        assertNull(Update.parse("not json"))
    }

    @Test fun onceADay() {
        assertTrue(Update.due(Update.DAY, 0))
        assertFalse(Update.due(Update.DAY, 1))
        assertTrue(Update.due(5, 10))   // clock went back
    }

    @Test fun newerOnlyWhenTheBuildIsHigher() {
        assertTrue(Update.newer(Release(11, "u"), 10))
        assertFalse(Update.newer(Release(10, "u"), 10))
        assertFalse(Update.newer(null, 10))
    }
}
