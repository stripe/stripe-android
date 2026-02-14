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
    fun visa() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.Visa,
                last4 = "4242"
            )
        }
    }

    @Test
    fun mastercard() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.MasterCard,
                last4 = "4242"
            )
        }
    }

    @Test
    fun discover() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.Discover,
                last4 = "4242"
            )
        }
    }

    @Test
    fun amex() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.AmericanExpress,
                last4 = "4242"
            )
        }
    }

    @Test
    fun jcb() {
        paparazziRule.snapshot {
            TapToAddCard(
                cardBrand = CardBrand.JCB,
                last4 = "4242"
            )
        }
    }
}
