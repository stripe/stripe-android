package com.stripe.android.paymentsheet.flowcontroller

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionUpdaterTest {

    private val defaultPaymentSheetConfiguration: PaymentSheet.Configuration = PaymentSheet.Configuration("Some name")

    @Test
    fun `Uses new payment selection if it exists`() {
        val newState = mockPaymentSheetStateWithPaymentIntent(paymentSelection = PaymentSelection.GooglePay)
        val updater = createUpdater()
        val result = updater(
            currentSelection = null,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Uses new payment selection if it exists and is part of paymentMethods`() {
        val newSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.card(id = "pm_1234")
        )
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentSelection = newSelection,
            customerPaymentMethods = listOf(newSelection.paymentMethod)
        )
        val updater = createUpdater()
        val result = updater(
            currentSelection = null,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(newSelection)
    }

    @Test
    fun `Uses new payment selection over current selection`() {
        val existingSelection = PaymentSelection.Saved(
            PaymentMethodFactory.card(id = "pm_1234")
        )
        val newSelection = PaymentSelection.Saved(
            PaymentMethodFactory.card(id = "pm_abcd").update(
                brand = CardBrand.AmericanExpress,
                last4 ="0005",
                addCbcNetworks = false
            )
        )

        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card"),
            customerPaymentMethods = listOf(
                existingSelection.paymentMethod,
                newSelection.paymentMethod,
            ),
            paymentSelection = newSelection
        )
        val updater = createUpdater()
        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(newSelection)
    }

    @Test
    fun `Uses current payment selection if new selection does not exist`() {
        val existingSelection = PaymentSelection.Saved(
            PaymentMethodFactory.card(id = "pm_1234")
        )

        val newConfig = PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            allowsDelayedPaymentMethods = true,
        )
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card", "sofort"),
            customerPaymentMethods = listOf(
                existingSelection.paymentMethod,
            ),
            config = newConfig
        )

        val updater = createUpdater()
        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Uses current payment selection if new selection type is no longer supported`() {
        val existingSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val newSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)

        val newConfig = PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            allowsDelayedPaymentMethods = true,
        )
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentSelection = newSelection,
            customerPaymentMethods = listOf(
                existingSelection.paymentMethod,
                newSelection.paymentMethod,
            ),
            paymentMethodTypes = listOf("card", "sofort"),
            config = newConfig
        )

        val updater = createUpdater()
        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can use existing payment selection if it's still supported`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "Sofort".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.SOFORT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val newConfig = PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            allowsDelayedPaymentMethods = true,
        )
        val newState = mockPaymentSheetStateWithPaymentIntent(
            paymentMethodTypes = listOf("card", "sofort"),
            config = newConfig
        )

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                allowsDelayedPaymentMethods = true,
            ),
            newState = newState,
            newConfig = newConfig,
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
            newConfig = defaultPaymentSheetConfiguration,
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
            newConfig = defaultPaymentSheetConfiguration,
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
            newConfig = defaultPaymentSheetConfiguration,
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
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can use external payment method if it's supported`() {
        val paymentSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
        )
        val updater = createUpdater()

        val result = updater(
            currentSelection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isEqualTo(paymentSelection)
    }

    @Test
    fun `Can't use external payment method if it's not returned by backend`() {
        val paymentSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            externalPaymentMethodSpecs = emptyList()
        )
        val updater = createUpdater()

        val result = updater(
            currentSelection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `PaymentSelection is reset when payment method requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.P24,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        // Paypal PaymentIntent does not require a mandate, but paypal SetupIntent does
        val newState = mockPaymentSheetStateWithSetupIntent(
            paymentMethodTypes = listOf("card", "p24"),
            customerPaymentMethods = PaymentMethodFixtures.createCards(3),
        )

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `PaymentSelection is preserved when payment method no longer requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
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

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when payment method still requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
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

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when config changes are not volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        val newConfig = defaultPaymentSheetConfiguration.copy(
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
        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = newConfig
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
            newConfig = newConfig,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when config changes are volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        val newConfig = defaultPaymentSheetConfiguration.copy(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "hi-jay@example.com")
        )
        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = newConfig
        )

        val updater = createUpdater()

        val result = updater(
            currentSelection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = newConfig
        )

        assertThat(result).isEqualTo(null)
    }

    private fun mockPaymentSheetStateWithPaymentIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = defaultPaymentSheetConfiguration,
        externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec> = emptyList(),
    ): PaymentSheetState.Full {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        return PaymentSheetState.Full(
            config = config.asCommonConfiguration(),
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = customerPaymentMethods
            ),
            paymentSelection = paymentSelection,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = intent.copy(
                    paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
                ),
                sharedDataSpecs = listOf(
                    SharedDataSpec("card"),
                    SharedDataSpec("paypal"),
                    SharedDataSpec("sofort"),
                ),
                externalPaymentMethodSpecs = externalPaymentMethodSpecs,
                isGooglePayReady = true,
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
            config = config.asCommonConfiguration(),
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = customerPaymentMethods
            ),
            paymentSelection = paymentSelection,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = intent.copy(
                    paymentMethodTypes = paymentMethodTypes ?: intent.paymentMethodTypes,
                ),
                isGooglePayReady = true,
            ),
        )
    }

    private fun createUpdater(): PaymentSelectionUpdater {
        return DefaultPaymentSelectionUpdater()
    }
}
