package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionUpdaterTest {

    @Test
    fun `Uses new payment selection if there's no existing one`() {
        val newState = mockPaymentSheetState(paymentSelection = PaymentSelection.GooglePay)
        val result = PaymentSelectionUpdater.process(
            currentSelection = null,
            newState = newState,
        )
        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing new payment selection if it's still supported`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "Sofort",
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.SOFORT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        val newState = mockPaymentSheetState(paymentMethodTypes = listOf("card", "sofort"))

        val result = PaymentSelectionUpdater.process(
            currentSelection = existingSelection,
            newState = newState,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can use existing saved payment selection if it's still supported`() {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val existingSelection = PaymentSelection.Saved(paymentMethod)

        val newState = mockPaymentSheetState(
            paymentMethodTypes = listOf("card", "cashapp"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
        )

        val result = PaymentSelectionUpdater.process(
            currentSelection = existingSelection,
            newState = newState,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer supported`() {
        val existing = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val newState = mockPaymentSheetState(paymentMethodTypes = listOf("card", "cashapp"))

        val result = PaymentSelectionUpdater.process(
            currentSelection = existing,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can't use existing saved payment selection if it's no longer in customer payment methods`() {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val existingSelection = PaymentSelection.Saved(paymentMethod)

        val newState = mockPaymentSheetState(
            paymentMethodTypes = listOf("card", "cashapp"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val result = PaymentSelectionUpdater.process(
            currentSelection = existingSelection,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    private fun mockPaymentSheetState(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
    ): PaymentSheetState.Full {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        return PaymentSheetState.Full(
            config = null,
            stripeIntent = intent.copy(
                paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
            ),
            customerPaymentMethods = customerPaymentMethods,
            isGooglePayReady = true,
            linkState = null,
            paymentSelection = paymentSelection,
        )
    }
}
