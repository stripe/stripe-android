package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionUpdaterTest {

    @Test
    fun `Uses new payment selection if there's no existing one`() {
        val newState = mockPaymentSheetState(paymentSelection = PaymentSelection.GooglePay)
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)
        val result = updater(
            currentSelection = null,
            newState = newState,
        )
        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing new payment selection if it's still supported`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "Cash App",
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val newState = mockPaymentSheetState(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater(
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = PAYMENT_INTENT.paymentMethodTypes + "cashapp",
            ),
        )

        val result = updater(
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
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
            currentSelection = existingSelection,
            newState = newState,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer allowed`() {
        val existing = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val newState = mockPaymentSheetState(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
            currentSelection = existing,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer available`() {
        // Cash App Pay is not supported for setup intents
        val existing = PaymentSelection.Saved(PaymentMethodFactory.cashAppPay())

        val newState = mockPaymentSheetState(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater(stripeIntent = SETUP_INTENT)

        val result = updater(
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
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
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

    private fun createUpdater(
        stripeIntent: StripeIntent,
    ): PaymentSelectionUpdater {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Context>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            update(stripeIntent, serverLpmSpecs = null)
        }

        return DefaultPaymentSelectionUpdater(lpmRepository)
    }

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val SETUP_INTENT = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
    }
}
