package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionUpdaterTest {

    private val defaultPaymentSheetConfiguration: PaymentSheet.Configuration = PaymentSheet.Configuration("Some name")

    @Test
    fun `Uses new payment selection if there's no existing one`() {
        val newState = mockPaymentSheetStateWithPaymentIntent(paymentSelection = PaymentSelection.GooglePay)
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)
        val result = updater(
            currentSelection = null,
            previousConfig = null,
            newState = newState,
        )
        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing payment selection if it's still supported`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "Sofort",
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.SOFORT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card", "sofort"),
            config = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                allowsDelayedPaymentMethods = true,
            )
        )

        val updater = createUpdater(
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = PAYMENT_INTENT.paymentMethodTypes + "sofort",
            ),
        )

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                allowsDelayedPaymentMethods = true,
            ),
            newState = newState,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can use existing saved payment selection if it's still supported`() {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val existingSelection = PaymentSelection.Saved(paymentMethod)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card", "cashapp"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
        )
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer allowed`() {
        val existing = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val newState = mockPaymentSheetStateWithPaymentIntent(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
            currentSelection = existing,
            previousConfig = null,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer available`() {
        // Cash App Pay is not supported for setup intents
        val existing = PaymentSelection.Saved(PaymentMethodFactory.cashAppPay())

        val newState = mockPaymentSheetStateWithPaymentIntent(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater(stripeIntent = SETUP_INTENT)

        val result = updater(
            currentSelection = existing,
            previousConfig = null,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can't use existing saved payment selection if it's no longer in customer payment methods`() {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val existingSelection = PaymentSelection.Saved(paymentMethod)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card", "cashapp"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )
        val updater = createUpdater(stripeIntent = PAYMENT_INTENT)

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = null,
            newState = newState,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `PaymentSelection is reset when payment method requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "paypal",
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // Paypal PaymentIntent does not require a mandate, but paypal SetupIntent does
        val newState = mockPaymentSheetStateWithSetupIntent(
            paymentMethodTypes = listOf("card", "paypal"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val updater = createUpdater(
            stripeIntent = SETUP_INTENT.copy(
                countryCode = "de",
                paymentMethodTypes = SETUP_INTENT.paymentMethodTypes + "paypal",
            ),
        )

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = null,
            newState = newState,
        )

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `PaymentSelection is preserved when payment method no longer requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "paypal",
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL.copy(
                requiresMandate = true
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // Paypal PaymentIntent does not require a mandate, but paypal SetupIntent does
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("paypal"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val updater = createUpdater(
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = PAYMENT_INTENT.paymentMethodTypes + "paypal",
            ),
        )

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when payment method still requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            labelResource = "paypal",
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL.copy(
                requiresMandate = true
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // Paypal PaymentIntent does not require a mandate, but paypal SetupIntent does
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("paypal"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val updater = createUpdater(
            stripeIntent = SETUP_INTENT.copy(
                countryCode = "de",
                paymentMethodTypes = PAYMENT_INTENT.paymentMethodTypes + "paypal",
            ),
        )

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    private fun mockPaymentSheetStateWithPaymentIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = PaymentSheet.Configuration("Some name"),
    ): PaymentSheetState.Full {
        val intent = PAYMENT_INTENT

        return PaymentSheetState.Full(
            config = config,
            stripeIntent = intent.copy(
                paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
            ),
            customerPaymentMethods = customerPaymentMethods,
            isGooglePayReady = true,
            linkState = null,
            paymentSelection = paymentSelection,
            isEligibleForCardBrandChoice = false,
        )
    }

    private fun mockPaymentSheetStateWithSetupIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = defaultPaymentSheetConfiguration,
    ): PaymentSheetState.Full {
        val intent = SETUP_INTENT

        return PaymentSheetState.Full(
            config = config,
            stripeIntent = intent.copy(
                paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
            ),
            customerPaymentMethods = customerPaymentMethods,
            isGooglePayReady = true,
            linkState = null,
            paymentSelection = paymentSelection,
            isEligibleForCardBrandChoice = false,
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
