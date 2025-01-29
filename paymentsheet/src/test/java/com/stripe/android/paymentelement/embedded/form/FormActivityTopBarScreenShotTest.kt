package com.stripe.android.paymentelement.embedded.form

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class FormActivityTopBarScreenShotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testTopBar_liveMode() {
        paparazziRule.snapshot {
            FormActivityTopBar(
                isLiveMode = true,
                onDismissed = {}
            )
        }
    }

    @Test
    fun testTopBar_testMode() {
        paparazziRule.snapshot {
            FormActivityTopBar(
                isLiveMode = false,
                onDismissed = {}
            )
        }
    }
}
