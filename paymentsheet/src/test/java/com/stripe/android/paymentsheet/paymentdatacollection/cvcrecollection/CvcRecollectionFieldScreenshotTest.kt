package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class CvcRecollectionFieldScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                element = CvcElement(
                    IdentifierSpec(),
                    CvcController(cardBrandFlow = stateFlowOf(CardBrand.Unknown))
                ),
                cardBrand = CardBrand.Visa,
                lastFour = "4242"
            )
        }
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                element = CvcElement(
                    IdentifierSpec(),
                    CvcController(
                        cardBrandFlow = stateFlowOf(CardBrand.Unknown),
                        initialValue = "424"
                    )
                ),
                cardBrand = CardBrand.Visa,
                lastFour = "4242"
            )
        }
    }
}
