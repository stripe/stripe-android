package com.stripe.android.paymentsheet.flowcontroller

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
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
            selection = null,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )
        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing payment selection if it's still supported`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "Sofort".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
            iconResourceNight = null,
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
            selection = existingSelection,
            previousConfig = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                allowsDelayedPaymentMethods = true,
            ),
            newState = newState,
            newConfig = newConfig,
            walletButtonsAlreadyShown = false,
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
            selection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )
        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer allowed`() {
        val existing = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val newState = mockPaymentSheetStateWithPaymentIntent(paymentMethodTypes = listOf("card", "cashapp"))
        val updater = createUpdater()

        val result = updater(
            selection = existing,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
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
            selection = existing,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
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
            selection = existingSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
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
            selection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
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
            selection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Can use custom payment method if it's supported`() {
        val paymentSelection =
            PaymentMethodFixtures.createCustomPaymentMethod(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD)
        )
        val updater = createUpdater()

        val result = updater(
            selection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )
        assertThat(result).isEqualTo(paymentSelection)
    }

    @Test
    fun `Can't use custom payment method if it's not returned by backend`() {
        val paymentSelection =
            PaymentMethodFixtures.createCustomPaymentMethod(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD)

        val newState = mockPaymentSheetStateWithPaymentIntent(
            displayableCustomPaymentMethods = emptyList()
        )
        val updater = createUpdater()

        val result = updater(
            selection = paymentSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `PaymentSelection is reset when payment method requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            iconResourceNight = null,
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
            selection = existingSelection,
            previousConfig = null,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `PaymentSelection is preserved when payment method no longer requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            iconResourceNight = null,
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
            selection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when payment method still requires mandate after updating intent`() {
        val existingSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "paypal".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_paypal,
            iconResourceNight = null,
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
            selection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = defaultPaymentSheetConfiguration,
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is preserved when config changes are not volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        @Suppress("DEPRECATION")
        val newConfig = defaultPaymentSheetConfiguration.newBuilder()
            .merchantDisplayName("Some other change")
            .googlePay(
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = CountryCode.US.value,
                    amount = 5099,
                    currencyCode = "USD",
                    label = "Some product",
                    buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
                )
            ).appearance(
                PaymentSheet.Appearance(
                    colorsLight = PaymentSheet.Colors.defaultDark,
                    primaryButton = PaymentSheet.PrimaryButton(
                        colorsLight = PaymentSheet.PrimaryButtonColors(
                            background = Color.BLACK,
                            onBackground = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                            border = StripeThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb(),
                        ),
                        colorsDark = PaymentSheet.PrimaryButtonColors(
                            background = Color.BLACK,
                            onBackground = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                            border = StripeThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb(),
                        )
                    ),
                )
            )
            .build()
        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = newConfig
        )

        val updater = createUpdater()

        val result = updater(
            selection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration.newBuilder()
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = CountryCode.US.value,
                        amount = 5099,
                        currencyCode = "USD",
                        label = "A product",
                        buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Plain,
                    )
                ).build(),
            newState = newState,
            newConfig = newConfig,
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(existingSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when config changes are volatile`() {
        val existingSelection = PaymentSelection.GooglePay

        val newConfig = defaultPaymentSheetConfiguration.newBuilder()
            .defaultBillingDetails(PaymentSheet.BillingDetails(email = "hi-jay@example.com"))
            .build()
        val newState = mockPaymentSheetStateWithPaymentIntent(
            config = newConfig
        )

        val updater = createUpdater()

        val result = updater(
            selection = existingSelection,
            previousConfig = defaultPaymentSheetConfiguration,
            newState = newState,
            newConfig = newConfig,
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(null)
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `If using wallet buttons config option with only GPay visible and selection is Link, should be Link`() {
        val updater = createUpdater()

        val result = updater(
            selection = PaymentSelection.Link(linkExpressMode = LinkExpressMode.DISABLED),
            previousConfig = null,
            newState = mockPaymentSheetStateWithPaymentIntent(),
            newConfig = defaultPaymentSheetConfiguration.newBuilder()
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                            walletButtonsView = mapOf(
                                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                            ),
                        ),
                    ),
                ).build(),
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(PaymentSelection.Link(linkExpressMode = LinkExpressMode.DISABLED))
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `If using wallet buttons config option with only Link visible and selection is GPay, should be GPay`() {
        val updater = createUpdater()

        val result = updater(
            selection = PaymentSelection.GooglePay,
            previousConfig = null,
            newState = mockPaymentSheetStateWithPaymentIntent(),
            newConfig = defaultPaymentSheetConfiguration.newBuilder()
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                            walletButtonsView = mapOf(
                                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                            ),
                        ),
                    ),
                ).build(),
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `If using wallet buttons config with specific wallet and existing selection matches, should preserve it`() {
        val updater = createUpdater()

        val result = updater(
            selection = PaymentSelection.Link(linkExpressMode = LinkExpressMode.DISABLED),
            previousConfig = null,
            newState = mockPaymentSheetStateWithPaymentIntent(),
            newConfig = defaultPaymentSheetConfiguration.newBuilder()
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                            walletButtonsView = mapOf(
                                PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                                PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                            ),
                        ),
                    ),
                ).build(),
            walletButtonsAlreadyShown = false,
        )

        assertThat(result).isEqualTo(PaymentSelection.Link(linkExpressMode = LinkExpressMode.DISABLED))
    }

    private fun mockPaymentSheetStateWithPaymentIntent(
        paymentMethodTypes: List<String>? = null,
        paymentSelection: PaymentSelection? = null,
        customerPaymentMethods: List<PaymentMethod> = emptyList(),
        config: PaymentSheet.Configuration = defaultPaymentSheetConfiguration,
        externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec> = emptyList(),
        displayableCustomPaymentMethods: List<DisplayableCustomPaymentMethod> = emptyList(),
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
                linkState = LinkState(
                    configuration = mock(),
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                externalPaymentMethodSpecs = externalPaymentMethodSpecs,
                displayableCustomPaymentMethods = displayableCustomPaymentMethods,
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
