package com.smizflark.aigile.messenger

import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.ui.PIC_EDGE_INSET
import org.junit.Assert.assertTrue
import org.junit.Test

class PicEditorInsetTest {
    /** AII-406: the editor has 12dp outside padding, so with the inset the crop handles sit at least 24dp from the screen edge. */
    @Test fun handlesStayClearOfTheBackSwipe() = assertTrue(12.dp + PIC_EDGE_INSET >= 24.dp)
}
