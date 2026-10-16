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

    /* ---------- AII-314: the board first ---------- */

    @Test fun parseBoardReadsTheBoardsLatest() {
        assertEquals(Release(330, "/api/messenger/apk", board = true, sha256 = "ab"),
            Update.parseBoard("""{"build":330,"version":"0.1.0","size":5,"sha256":"ab","url":"/api/messenger/apk"}"""))
        assertNull(Update.parseBoard("""{"error":"no Messenger build on this board yet"}"""))
        assertNull(Update.parseBoard("""{"build":330,"url":"https://elsewhere/x.apk"}"""))
        assertNull(Update.parseBoard("not json"))
    }

    @Test fun theBoardsFingerprintIsKeptAndChecked() {
        val r = Update.parseBoard("""{"build":5,"url":"/api/messenger/apk","sha256":"BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"}""")!!
        assertTrue(Update.matches("abc".toByteArray(), r.sha256))   // SHA-256 of "abc"
        assertFalse(Update.matches("abd".toByteArray(), r.sha256))
        assertTrue(Update.matches("anything".toByteArray(), ""))   // an older board sends none
    }

    @Test fun aNewerBoardBuildSkipsGitHub() {
        val board = Release(12, "/api/messenger/apk", board = true)
        assertEquals(board, Update.pick(board, 10) { error("GitHub asked") })
    }

    @Test fun noBoardOrOlderBoardAsksGitHub() {
        val gh = Release(13, "https://x/a.apk")
        assertEquals(gh, Update.pick(null, 10) { gh })
        assertEquals(gh, Update.pick(Release(10, "/api/messenger/apk", board = true), 10) { gh })
        assertNull(Update.pick(null, 10) { null })
    }

    @Test fun onATieTheBoardWins() {
        val board = Release(9, "/api/messenger/apk", board = true)
        assertEquals(board, Update.pick(board, 10) { Release(9, "https://x/a.apk") })
    }
}
