package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.PaparazziTest
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(PaparazziTest::class)
internal class TapToAddButtonUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(10.dp),
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            TapToButtonUI(enabled = true) {}
        }
    }
}
