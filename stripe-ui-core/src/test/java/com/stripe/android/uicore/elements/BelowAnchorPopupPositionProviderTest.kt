package com.stripe.android.uicore.elements

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BelowAnchorPopupPositionProviderTest {

    @Test
    fun `positions popup below anchor`() {
        val position = BelowAnchorPopupPositionProvider.calculatePosition(
            anchorBounds = IntRect(left = 24, top = 100, right = 376, bottom = 156),
            windowSize = IntSize(width = 400, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 352, height = 240),
        )

        assertThat(position.x).isEqualTo(24)
        assertThat(position.y).isEqualTo(156)
    }
}
