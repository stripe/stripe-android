package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.taptoadd.ui.TapToAddCardLayout
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class TapToAddCardLayoutScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(10.dp),
    )

    @Test
    fun visa() {
        paparazziRule.snapshot {
            TapToAddCardLayout(
                cardBrand = CardBrand.Visa,
                last4 = "4242"
            )
        }
    }

    @Test
    fun mastercard() {
        paparazziRule.snapshot {
            TapToAddCardLayout(
                cardBrand = CardBrand.MasterCard,
                last4 = "4242"
            )
        }
    }

    @Test
    fun discover() {
        paparazziRule.snapshot {
            TapToAddCardLayout(
                cardBrand = CardBrand.Discover,
                last4 = "4242"
            )
        }
    }

    @Test
    fun amex() {
        paparazziRule.snapshot {
            TapToAddCardLayout(
                cardBrand = CardBrand.AmericanExpress,
                last4 = "4242"
            )
        }
    }

    @Test
    fun jcb() {
        paparazziRule.snapshot {
            TapToAddCardLayout(
                cardBrand = CardBrand.JCB,
                last4 = "4242"
            )
        }
    }
}
