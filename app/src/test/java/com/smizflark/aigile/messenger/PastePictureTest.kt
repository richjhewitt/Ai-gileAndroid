package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.clipHasNewPicture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Paste a copied picture into a message (AII-56). */
class PastePictureTest {
    @Test fun aCopiedPictureIsOffered() = assertTrue(clipHasNewPicture(listOf("image/png"), 100, -1))

    @Test fun textIsNotOffered() = assertFalse(clipHasNewPicture(listOf("text/plain"), 100, -1))

    @Test fun theSamePictureIsNotOfferedTwice() = assertFalse(clipHasNewPicture(listOf("image/png"), 100, 100))

    @Test fun aNewCopyIsOfferedAgain() = assertTrue(clipHasNewPicture(listOf("text/uri-list", "image/jpeg"), 200, 100))
}
