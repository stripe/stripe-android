package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
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
    fun testEmptyState() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                cvcControllerFlow = cvcControllerFlowWithValue(),
                isProcessing = false,
                animationDuration = 0,
                animationDelay = 0
            )
        }
    }

    @Test
    fun testErrorState() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                cvcControllerFlow = cvcControllerFlowWithValue("12"),
                isProcessing = false,
                animationDuration = 0,
                animationDelay = 0
            )
        }
    }

    private fun cvcControllerFlowWithValue(value: String? = null) =
        stateFlowOf(
            CvcController(
                cvcTextFieldConfig = CvcConfig(),
                cardBrandFlow = stateFlowOf(CardBrand.Visa),
                initialValue = value
            )
        )
}
