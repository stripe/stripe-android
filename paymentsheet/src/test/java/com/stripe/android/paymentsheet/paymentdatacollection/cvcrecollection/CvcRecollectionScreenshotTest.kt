package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class CvcRecollectionScreenshotTest {
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

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                lastFour = "4242",
                isTestMode = false,
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
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
                lastFour = "4242",
                isTestMode = false,
                viewActionHandler = {},
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
            )
        }
    }

    @Test
    fun testFilledTestMode() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                lastFour = "4242",
                isTestMode = true,
                viewActionHandler = {},
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
            )
        }
    }
}
