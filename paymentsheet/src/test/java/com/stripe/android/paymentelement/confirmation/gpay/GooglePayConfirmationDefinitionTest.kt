package com.stripe.android.paymentelement.confirmation.gpay

import androidx.activity.result.ActivityResultCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.model.CardBrand
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.paymentsheet.utils.RecordingGooglePayPaymentMethodLauncherFactory
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import com.stripe.android.R as PaymentsCoreR

class GooglePayConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'GooglePay`() {
        val definition = createGooglePayConfirmationDefinition()

        assertThat(definition.key).isEqualTo("GooglePay")
    }

    @Test
    fun `'option' return casted 'GooglePayConfirmationOption'`() {
        val definition = createGooglePayConfirmationDefinition()

        assertThat(definition.option(GOOGLE_PAY_CONFIRMATION_OPTION)).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createGooglePayConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        var onResultCalled = false
        val onResult: (GooglePayPaymentMethodLauncher.Result) -> Unit = { onResultCalled = true }
        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val call = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(call.contract).isInstanceOf<GooglePayPaymentMethodLauncherContractV2>()
            assertThat(call.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = call.callback.asCallbackFor<GooglePayPaymentMethodLauncher.Result>()

            callback.onActivityResult(GooglePayPaymentMethodLauncher.Result.Completed(PaymentMethodFactory.card()))

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'toResult' should return 'NextStep' when 'GooglePayLauncherResult' is 'Completed'`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        val paymentMethod = PaymentMethodFactory.card().run {
            copy(
                card = card?.copy(
                    wallet = Wallet.GooglePayWallet(dynamicLast4 = card?.last4),
                )
            )
        }
        val result = definition.toResult(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = GooglePayPaymentMethodLauncher.Result.Completed(
                paymentMethod = paymentMethod,
            ),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val successResult = result.asNextStep()

        assertThat(successResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
        assertThat(successResult.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()
    }

    @Test
    fun `'toResult' should return 'Failed' when 'GooglePayLauncherResult' is 'Failed'`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        val exception = IllegalStateException("Failed!")
        val result = definition.toResult(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = GooglePayPaymentMethodLauncher.Result.Failed(
                errorCode = 400,
                error = exception
            ),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(PaymentsCoreR.string.stripe_internal_error.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.GooglePay(400))
    }

    @Test
    fun `'toResult' should return 'Failed' with network error message if network error code is returned`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        val exception = IllegalStateException("Failed!")
        val result = definition.toResult(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = GooglePayPaymentMethodLauncher.Result.Failed(
                errorCode = GooglePayPaymentMethodLauncher.NETWORK_ERROR,
                error = exception
            ),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message)
            .isEqualTo(PaymentsCoreR.string.stripe_failure_connection_error.resolvableString)
        assertThat(failedResult.type).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.GooglePay(GooglePayPaymentMethodLauncher.NETWORK_ERROR)
        )
    }

    @Test
    fun `'toResult' should return 'Canceled' when 'GooglePayLauncherResult' is 'Canceled'`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = GooglePayPaymentMethodLauncher.Result.Canceled,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
    }

    @Test
    fun `'Fail' action should be returned if currency code is not provided with a setup intent`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = "si_123_secret_123",
            ),
            merchantCurrencyCode = null,
            test = ::assertFailActionFromCurrencyFailure,
        )

    @Test
    fun `'Fail' action should be returned if currency code is not provided with a deferred intent in setup mode`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                ),
            ),
            merchantCurrencyCode = null,
            test = ::assertFailActionFromCurrencyFailure,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a setup intent`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = "si_123_secret_123",
            ),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a deferred intent in setup mode`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                ),
            ),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is not provided with a payment intent`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            merchantCurrencyCode = null,
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a payment intent`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is not provided with deferred intent in payment mode`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099,
                        currency = "USD",
                    ),
                ),
            ),
            merchantCurrencyCode = null,
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with deferred intent in payment mode`() =
        runActionTest(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099,
                        currency = "USD",
                    ),
                ),
            ),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `On 'launch', should create google pay launcher properly`() = runTest {
        RecordingGooglePayPaymentMethodLauncherFactory.test(mock()) {
            val definition = createGooglePayConfirmationDefinition(factory)
            val launcher = FakeActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>()

            definition.launch(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                confirmationParameters = CONFIRMATION_PARAMETERS,
                arguments = Unit,
                launcher = launcher,
            )

            val createGooglePayLauncherCall = createGooglePayPaymentMethodLauncherCalls.awaitItem()

            assertThat(createGooglePayLauncherCall.activityResultLauncher).isEqualTo(launcher)
            assertThat(createGooglePayLauncherCall.skipReadyCheck).isTrue()
            assertThat(createGooglePayLauncherCall.cardBrandFilter).isEqualTo(DefaultCardBrandFilter)

            assertThat(createGooglePayLauncherCall.config.environment).isEqualTo(GooglePayEnvironment.Test)
            assertThat(createGooglePayLauncherCall.config.merchantName).isEqualTo("Test merchant Inc.")
            assertThat(createGooglePayLauncherCall.config.merchantCountryCode).isEqualTo("US")
            assertThat(createGooglePayLauncherCall.config.allowCreditCards).isTrue()
            assertThat(createGooglePayLauncherCall.config.existingPaymentMethodRequired).isTrue()
            assertThat(createGooglePayLauncherCall.config.isEmailRequired).isFalse()
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.isRequired).isTrue()
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.isPhoneNumberRequired).isFalse()
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.format)
                .isEqualTo(GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full)
        }
    }

    @Test
    fun `On 'launch', should create google pay launcher properly with excepted parameters`() =
        runLaunchParametersTest(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            merchantNameShouldBe = "Test merchant Inc.",
            merchantCountryCodeShouldBe = "US",
            emailShouldBeRequired = false,
            billingAddressShouldBeRequired = true,
            phoneNumberShouldBeRequired = false,
            billingAddressFormatShouldBe = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
            environmentShouldBe = GooglePayEnvironment.Test,
            cardBrandFilterShouldBe = DefaultCardBrandFilter,
        )

    @Test
    fun `On 'launch', should create launcher with required billing parameters, prod env, and expected card filter`() =
        runLaunchParametersTest(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantName = "Another merchant Inc.",
                    merchantCountryCode = "CA",
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    ),
                    cardBrandFilter = FakeCardBrandFilter,
                )
            ),
            merchantNameShouldBe = "Another merchant Inc.",
            merchantCountryCodeShouldBe = "CA",
            emailShouldBeRequired = true,
            billingAddressShouldBeRequired = true,
            phoneNumberShouldBeRequired = true,
            billingAddressFormatShouldBe = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min,
            environmentShouldBe = GooglePayEnvironment.Production,
            cardBrandFilterShouldBe = FakeCardBrandFilter,
        )

    @Test
    fun `On 'launch', should create google pay launcher properly with no billing parameters`() =
        runLaunchParametersTest(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                )
            ),
            merchantNameShouldBe = "Test merchant Inc.",
            merchantCountryCodeShouldBe = "US",
            emailShouldBeRequired = false,
            billingAddressShouldBeRequired = false,
            phoneNumberShouldBeRequired = false,
            billingAddressFormatShouldBe = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min,
            environmentShouldBe = GooglePayEnvironment.Test,
            cardBrandFilterShouldBe = DefaultCardBrandFilter,
        )

    @Test
    fun `On 'launch', should use payment intent currency code if available`() = runTest {
        val googlePayLauncher = mock<GooglePayPaymentMethodLauncher>()

        RecordingGooglePayPaymentMethodLauncherFactory.test(googlePayLauncher) {
            val definition = createGooglePayConfirmationDefinition(factory)
            val launcher = FakeActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>()

            definition.launch(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                    config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                        merchantCurrencyCode = "USD",
                    ),
                ),
                confirmationParameters = CONFIRMATION_PARAMETERS.copy(
                    intent = PAYMENT_INTENT.copy(currency = "CAD"),
                ),
                arguments = Unit,
                launcher = launcher,
            )

            assertThat(createGooglePayPaymentMethodLauncherCalls.awaitItem()).isNotNull()

            verify(googlePayLauncher, times(1)).present(
                currencyCode = "CAD",
                amount = 1000L,
                transactionId = "pi_12345",
                label = null
            )
        }
    }

    @Test
    fun `On 'launch', should use payment intent currency & amount`() = runTest {
        val googlePayLauncher = mock<GooglePayPaymentMethodLauncher>()

        RecordingGooglePayPaymentMethodLauncherFactory.test(googlePayLauncher) {
            val definition = createGooglePayConfirmationDefinition(factory)
            val launcher = FakeActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>()

            definition.launch(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                    config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                        merchantCurrencyCode = "USD",
                        customLabel = "Merchant Inc."
                    ),
                ),
                confirmationParameters = CONFIRMATION_PARAMETERS.copy(
                    intent = PAYMENT_INTENT.copy(currency = "CAD"),
                ),
                arguments = Unit,
                launcher = launcher,
            )

            assertThat(createGooglePayPaymentMethodLauncherCalls.awaitItem()).isNotNull()

            verify(googlePayLauncher, times(1)).present(
                currencyCode = "CAD",
                amount = 1000L,
                transactionId = "pi_12345",
                label = "Merchant Inc."
            )
        }
    }

    @Test
    fun `On 'launch', should use set currency & custom amount when using setup intent`() = runTest {
        val googlePayLauncher = mock<GooglePayPaymentMethodLauncher>()

        RecordingGooglePayPaymentMethodLauncherFactory.test(googlePayLauncher) {
            val definition = createGooglePayConfirmationDefinition(factory)
            val launcher = FakeActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>()

            definition.launch(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                    config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                        merchantCurrencyCode = "USD",
                        customAmount = 2099L,
                        customLabel = "Merchant Inc."
                    ),
                ),
                confirmationParameters = CONFIRMATION_PARAMETERS.copy(
                    intent = SetupIntentFactory.create(),
                ),
                arguments = Unit,
                launcher = launcher,
            )

            assertThat(createGooglePayPaymentMethodLauncherCalls.awaitItem()).isNotNull()

            verify(googlePayLauncher, times(1)).present(
                currencyCode = "USD",
                amount = 2099L,
                transactionId = "pi_12345",
                label = "Merchant Inc."
            )
        }
    }

    private fun runActionTest(
        initializationMode: PaymentElementLoader.InitializationMode,
        merchantCurrencyCode: String?,
        test: (scenario: ActionScenario) -> Unit,
    ) = runTest {
        val userFacingLogger = FakeUserFacingLogger()
        val definition = createGooglePayConfirmationDefinition(userFacingLogger = userFacingLogger)

        val action = definition.action(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantCurrencyCode = merchantCurrencyCode,
                ),
            ),
            confirmationParameters = CONFIRMATION_PARAMETERS.copy(
                initializationMode = initializationMode,
                intent = SetupIntentFactory.create(),
            ),
        )

        test(
            ActionScenario(
                action = action,
                userFacingLogger = userFacingLogger,
            )
        )
    }

    private fun runLaunchParametersTest(
        confirmationOption: GooglePayConfirmationOption,
        environmentShouldBe: GooglePayEnvironment,
        merchantNameShouldBe: String,
        merchantCountryCodeShouldBe: String?,
        billingAddressShouldBeRequired: Boolean,
        phoneNumberShouldBeRequired: Boolean,
        emailShouldBeRequired: Boolean,
        billingAddressFormatShouldBe: GooglePayPaymentMethodLauncher.BillingAddressConfig.Format,
        cardBrandFilterShouldBe: CardBrandFilter
    ) {
        RecordingGooglePayPaymentMethodLauncherFactory.test(mock()) {
            val definition = createGooglePayConfirmationDefinition(factory)
            val launcher = FakeActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>()

            definition.launch(
                confirmationOption = confirmationOption,
                confirmationParameters = CONFIRMATION_PARAMETERS,
                arguments = Unit,
                launcher = launcher,
            )

            val createGooglePayLauncherCall = createGooglePayPaymentMethodLauncherCalls.awaitItem()

            // Should always be the same value
            assertThat(createGooglePayLauncherCall.activityResultLauncher).isEqualTo(launcher)
            assertThat(createGooglePayLauncherCall.skipReadyCheck).isTrue()
            assertThat(createGooglePayLauncherCall.config.allowCreditCards).isTrue()
            assertThat(createGooglePayLauncherCall.config.existingPaymentMethodRequired).isTrue()

            // Can vary on merchant's config
            assertThat(createGooglePayLauncherCall.cardBrandFilter).isEqualTo(cardBrandFilterShouldBe)
            assertThat(createGooglePayLauncherCall.config.environment).isEqualTo(environmentShouldBe)
            assertThat(createGooglePayLauncherCall.config.merchantName).isEqualTo(merchantNameShouldBe)
            assertThat(createGooglePayLauncherCall.config.merchantCountryCode).isEqualTo(merchantCountryCodeShouldBe)
            assertThat(createGooglePayLauncherCall.config.isEmailRequired).isEqualTo(emailShouldBeRequired)
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.isRequired)
                .isEqualTo(billingAddressShouldBeRequired)
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.isPhoneNumberRequired)
                .isEqualTo(phoneNumberShouldBeRequired)
            assertThat(createGooglePayLauncherCall.config.billingAddressConfig.format)
                .isEqualTo(billingAddressFormatShouldBe)
        }
    }

    private fun assertFailActionFromCurrencyFailure(
        scenario: ActionScenario,
    ) {
        val action = scenario.action

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Fail<Unit>>()

        val failAction = action.asFail()
        val failureMessage = "GooglePayConfig.currencyCode is required in order to use " +
            "Google Pay when processing a Setup Intent"

        assertThat(scenario.userFacingLogger.getLoggedMessages()).containsExactly(failureMessage)

        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message).isEqualTo(failureMessage)
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType)
            .isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration)
    }

    private fun assertLaunchAction(
        scenario: ActionScenario,
    ) {
        val action = scenario.action

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.receivesResultInProcess).isTrue()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
    }

    private fun createGooglePayConfirmationDefinition(
        googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory =
            RecordingGooglePayPaymentMethodLauncherFactory.noOp(launcher = mock()),
        userFacingLogger: UserFacingLogger = FakeUserFacingLogger()
    ): GooglePayConfirmationDefinition {
        return GooglePayConfirmationDefinition(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
            userFacingLogger = userFacingLogger,
        )
    }

    @Parcelize
    private object FakeCardBrandFilter : CardBrandFilter {
        override fun isAccepted(cardBrand: CardBrand): Boolean {
            return false
        }
    }

    private class ActionScenario(
        val action: ConfirmationDefinition.Action<Unit>,
        val userFacingLogger: FakeUserFacingLogger,
    )

    private companion object {
        private val GOOGLE_PAY_CONFIRMATION_OPTION = GooglePayConfirmationOption(
            config = GooglePayConfirmationOption.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                merchantName = "Test merchant Inc.",
                merchantCountryCode = "US",
                merchantCurrencyCode = "CA",
                customAmount = 1099,
                customLabel = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
                cardBrandFilter = DefaultCardBrandFilter,
            )
        )

        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val APPEARANCE = PaymentSheet.Appearance().copy(
            colorsDark = PaymentSheet.Colors.defaultLight,
        )

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PAYMENT_INTENT,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            appearance = APPEARANCE,
            shippingDetails = null,
        )
    }
}
