package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class EditPaymentMethodUiScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

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
    fun testCanRemoveFalseState() {
        paparazziRule.snapshot {
            EditPaymentMethodUi(
                viewState = createViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    canUpdate = true,
                    canRemove = false,
                ),
                viewActionHandler = {},
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
        canRemove: Boolean = true,
    ): EditPaymentMethodViewState {
        return EditPaymentMethodViewState(
            status = status,
            last4 = "4242",
            selectedBrand = CardBrandChoice(
                brand = CardBrand.CartesBancaires
            ),
            canUpdate = canUpdate,
            availableBrands = listOf(
                CardBrandChoice(
                    brand = CardBrand.Visa
                ),
                CardBrandChoice(
                    brand = CardBrand.CartesBancaires
                )
            ),
            displayName = "Card".resolvableString,
            confirmRemoval = confirmRemoval,
            error = error?.resolvableString,
            canRemove = canRemove,
        )
    }
}
