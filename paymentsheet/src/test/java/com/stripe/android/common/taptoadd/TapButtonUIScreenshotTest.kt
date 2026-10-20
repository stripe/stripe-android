package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class TapButtonUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        FontSize.entries,
        boxModifier = Modifier.padding(10.dp),
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            TapButtonUI(
                label = stringResource(R.string.stripe_tap_to_add_card_button_label),
                enabled = true,
                onClick = {},
            )
        }
    }
}
