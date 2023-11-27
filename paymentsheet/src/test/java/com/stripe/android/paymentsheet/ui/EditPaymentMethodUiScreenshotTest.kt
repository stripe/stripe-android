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
        arrayOf(SystemAppearance.LightTheme),
        arrayOf(FontSize.DefaultFont),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
    )

    @Test
    fun testEnabledState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    canUpdate = true
                ),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testDisabledState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    canUpdate = false
                ),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testErrorState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    canUpdate = true,
                    error = "Failed to update payment method!"
                ),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testUpdatingState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Updating,
                    canUpdate = true
                ),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testRemovalConfirmationState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    canUpdate = false,
                    confirmRemoval = true,
                ),
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testRemovingState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Removing,
                    canUpdate = true
                ),
                viewActionHandler = {}
            )
        }
    }

    private fun createViewState(
        status: EditPaymentMethodViewState.Status,
        canUpdate: Boolean,
        error: String? = null,
        confirmRemoval: Boolean = false,
    ): EditPaymentMethodViewState {
        return EditPaymentMethodViewState(
            status = status,
            last4 = "4242",
            selectedBrand = EditPaymentMethodViewState.CardBrandChoice(
                brand = CardBrand.CartesBancaires
            ),
            canUpdate = canUpdate,
            availableBrands = listOf(
                EditPaymentMethodViewState.CardBrandChoice(
                    brand = CardBrand.Visa
                ),
                EditPaymentMethodViewState.CardBrandChoice(
                    brand = CardBrand.CartesBancaires
                )
            ),
            displayName = "Card",
            confirmRemoval = confirmRemoval,
            error = error?.let { resolvableString(it) },
        )
    }
}
