package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ScrollPlace
import com.smizflark.aigile.messenger.data.ScrollPlaces
import com.smizflark.aigile.messenger.ui.unreadBelow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Chat keeps its scroll place (AII-72). */
class ScrollPlaceTest {
    @Before fun fresh() = ScrollPlaces.clearAll()

    private fun b(key: String) = Bubble(key, "Lead", "hi", "2026-10-13T10:00:00Z", false)

    @Test fun saveAndRestore() {
        ScrollPlaces.save("chat", ScrollPlace("m5", 40, "m9"))
        assertEquals(ScrollPlace("m5", 40, "m9"), ScrollPlaces.get("chat"))
    }

    @Test fun firstOpenHasNoPlace() = assertNull(ScrollPlaces.get("other"))

    @Test fun clearGoesBackToNewest() {
        ScrollPlaces.save("chat", ScrollPlace("m5", 0, null)); ScrollPlaces.clear("chat")
        assertNull(ScrollPlaces.get("chat"))
    }

    @Test fun newMessagesDoNotShiftIt() {
        val place = ScrollPlace("m5", 0, "m9")
        assertEquals(4, ScrollPlaces.indexOf(place, listOf("m9", "m8", "m7", "m6", "m5", "m4")))
        // two arrive: the same row is found two places further along
        assertEquals(6, ScrollPlaces.indexOf(place, listOf("n2", "n1", "m9", "m8", "m7", "m6", "m5", "m4")))
    }

    @Test fun goneRowOpensAtNewest() = assertNull(ScrollPlaces.indexOf(ScrollPlace("zz", 0, null), listOf("a", "b")))

    @Test fun newMessagesStillCountForThePill() {
        val seen = ScrollPlaces.run { save("c", ScrollPlace("m5", 0, "m3")); get("c")!!.seen }
        assertEquals(2, unreadBelow(listOf(b("m1"), b("m3"), b("m4"), b("m5")), seen))
    }
}
