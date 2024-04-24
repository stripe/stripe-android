package com.stripe.android.paymentsheet.flowcontroller

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.lpmfoundations.luxe.PaymentMethodIcon
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.elements.SharedDataSpec
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
        val updater = createUpdater()
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
            paymentMethodIcon = PaymentMethodIcon.create(
                iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
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

        val updater = createUpdater()

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
        val updater = createUpdater()

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
        val updater = createUpdater()

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
        val updater = createUpdater()

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
        val updater = createUpdater()

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
            paymentMethodIcon = PaymentMethodIcon.create(
                iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // Paypal PaymentIntent does not require a mandate, but paypal SetupIntent does
        val newState = mockPaymentSheetStateWithSetupIntent(
            paymentMethodTypes = listOf("card", "paypal"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val updater = createUpdater()

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
            paymentMethodIcon = PaymentMethodIcon.create(
                iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
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

        val updater = createUpdater()

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
            paymentMethodIcon = PaymentMethodIcon.create(
                iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
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

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when config changes are not volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = defaultPaymentSheetConfiguration.copy(
                merchantDisplayName = "Some other change",
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = CountryCode.US.value,
                    amount = 5099,
                    currencyCode = "USD",
                    label = "Some product",
                    buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
                ),
                primaryButtonColor = ColorStateList.valueOf(Color.BLACK),
                appearance = PaymentSheet.Appearance(
                    colorsLight = PaymentSheet.Colors.defaultDark
                )
            )
        )

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration.copy(
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = CountryCode.US.value,
                    amount = 5099,
                    currencyCode = "USD",
                    label = "A product",
                    buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Plain,
                )
            ),
            newState = newState,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when config changes are volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = defaultPaymentSheetConfiguration.copy(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id1",
                    ephemeralKeySecret = "000"
                )
            )
        )

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
        )

        assertThat(result).isEqualTo(null)
    }

    private fun mockPaymentSheetStateWithPaymentIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = defaultPaymentSheetConfiguration,
    ): PaymentSheetState.Full {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        return PaymentSheetState.Full(
            config = config,
            customerPaymentMethods = customerPaymentMethods,
            isGooglePayReady = true,
            linkState = null,
            paymentSelection = paymentSelection,
            isEligibleForCardBrandChoice = false,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = intent.copy(
                    paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
                ),
                sharedDataSpecs = listOf(
                    SharedDataSpec("card"),
                    SharedDataSpec("paypal"),
                    SharedDataSpec("sofort"),
                )
            ),
        )
    }

    private fun mockPaymentSheetStateWithSetupIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = defaultPaymentSheetConfiguration,
    ): PaymentSheetState.Full {
        val intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD

        return PaymentSheetState.Full(
            config = config,
            customerPaymentMethods = customerPaymentMethods,
            isGooglePayReady = true,
            linkState = null,
            paymentSelection = paymentSelection,
            isEligibleForCardBrandChoice = false,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = intent.copy(
                    paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
                )
            ),
        )
    }

    private fun createUpdater(): PaymentSelectionUpdater {
        return DefaultPaymentSelectionUpdater()
    }
}
