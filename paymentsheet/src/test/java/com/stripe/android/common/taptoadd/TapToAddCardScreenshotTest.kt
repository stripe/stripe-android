package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.taptoadd.ui.TapToAddCard
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class TapToAddCardScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(10.dp),
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.Visa,
                last4 = "4242",
            )
        }
    }
}
