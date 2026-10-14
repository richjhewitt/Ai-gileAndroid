package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.TextSize
import com.smizflark.aigile.messenger.ui.nextTextSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PinchTextSizeTest {
    @Test fun pinchOutGoesBigger() = assertEquals(TextSize.Large, nextTextSize(TextSize.Normal, 1.5f))
    @Test fun pinchInGoesSmaller() = assertEquals(TextSize.Small, nextTextSize(TextSize.Normal, 0.6f))
    @Test fun smallPinchChangesNothing() = assertEquals(TextSize.Normal, nextTextSize(TextSize.Normal, 1.05f))
    @Test fun oneStepAtMost() = assertEquals(TextSize.Large, nextTextSize(TextSize.Normal, 5f))
    @Test fun stopsAtEnds() {
        assertEquals(TextSize.Huge, nextTextSize(TextSize.Huge, 2f))
        assertEquals(TextSize.Small, nextTextSize(TextSize.Small, 0.3f))
    }
}
