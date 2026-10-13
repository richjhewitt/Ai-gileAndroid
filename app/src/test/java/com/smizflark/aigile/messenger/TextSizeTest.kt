package com.smizflark.aigile.messenger

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import com.smizflark.aigile.messenger.ui.TextSize
import com.smizflark.aigile.messenger.ui.scaled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text size setting (AII-66): four sizes, stored by id, scaling every text style. */
class TextSizeTest {

    @Test fun unknownFallsBackToNormal() = assertEquals(TextSize.Normal, TextSize.of("nonsense"))

    @Test fun storedIdsRoundTrip() = TextSize.entries.forEach { assertEquals(it, TextSize.of(it.id)) }

    @Test fun sizesGrowInOrder() {
        val s = TextSize.entries.map { it.scale }
        assertEquals(s.sorted(), s)
        assertEquals(4, s.distinct().size)
    }

    @Test fun normalLeavesTextAlone() = assertEquals(Typography(), scaled(Typography(), TextSize.Normal.scale))

    @Test fun hugeGrowsMessagesAndLineHeight() {
        val t = Typography()
        val huge = scaled(t, TextSize.Huge.scale)
        assertEquals((t.bodyLarge.fontSize.value * 1.3f), huge.bodyLarge.fontSize.value, 0.01f)
        assertTrue(huge.bodyLarge.lineHeight.value > t.bodyLarge.lineHeight.value)
        assertTrue(huge.titleMedium.fontSize > t.titleMedium.fontSize)
    }

    @Test fun smallShrinks() = assertTrue(scaled(Typography(), TextSize.Small.scale).bodyMedium.fontSize < 14.sp)
}
