package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class EditPaymentMethodUiScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
    )

    @Test
    fun testEnabledState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(canUpdate = true),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testDisabledState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(canUpdate = false),
                viewActionHandler = {}
            )
        }
    }

    private fun createViewState(canUpdate: Boolean): EditPaymentViewState {
        return EditPaymentViewState(
            last4 = "4242",
            selectedBrand = EditPaymentViewState.CardBrandChoice(
                id = CardBrand.CartesBancaires.code,
                label = resolvableString(CardBrand.CartesBancaires.displayName),
                icon = CardBrand.CartesBancaires.icon
            ),
            canUpdate = canUpdate,
            availableBrands = listOf(
                EditPaymentViewState.CardBrandChoice(
                    id = CardBrand.Visa.code,
                    label = resolvableString(CardBrand.Visa.displayName),
                    icon = CardBrand.Visa.icon
                ),
                EditPaymentViewState.CardBrandChoice(
                    id = CardBrand.CartesBancaires.code,
                    label = resolvableString(CardBrand.CartesBancaires.displayName),
                    icon = CardBrand.CartesBancaires.icon
                )
            )
        )
    }
}
