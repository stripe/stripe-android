package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class CvcRecollectionScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    private fun interactor(cvc: String? = null, isTestMode: Boolean = true): CvcRecollectionInteractor {
        return DefaultCvcRecollectionInteractor(
            args = Args(
                lastFour = "4242",
                cardBrand = CardBrand.Visa,
                cvc = cvc,
                isTestMode = isTestMode
            ),
        )
    }

    private fun controller(): CvcController {
        return CvcController(
            cardBrandFlow = stateFlowOf(CardBrand.Visa)
        )
    }

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = false,
                controller = controller(),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testEmptyPaymentScreenDisplayMode() {
        paparazziRule.snapshot {
            CvcRecollectionPaymentSheetScreen(
                interactor = interactor()
            )
        }
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = false,
                controller = controller(),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testFilledTestMode() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = true,
                controller = controller(),
                viewActionHandler = {}
            )
        }
    }
}
