package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.injection.InternalGooglePayPaymentMethodLauncherFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PAYMENT_INTENT
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.asSaved
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.paymentsheet.utils.RecordingInternalGooglePayPaymentMethodLauncherFactory
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as PaymentsCoreR

@RunWith(RobolectricTestRunner::class)
class GooglePayConfirmationDefinitionTest {
    @get:Rule
    val allowNoExistingPaymentMethodForGooglePayRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.allowNoExistingPaymentMethodForGooglePay,
        isEnabled = false,
    )

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
    fun `'createLauncher' should register launcher and create internal launcher with skipped ready check`() =
        RecordingInternalGooglePayPaymentMethodLauncherFactory.test(mock()) {
            val definition = createGooglePayConfirmationDefinition(factory)

            var onResultCalled = false
            val onResult: (GooglePayPaymentMethodLauncher.Result) -> Unit = { onResultCalled = true }
            DummyActivityResultCaller.test {
                definition.createLauncher(
                    activityResultCaller = activityResultCaller,
                    onResult = onResult,
                )

                val call = awaitRegisterCall()
                val registeredLauncher = awaitNextRegisteredLauncher()

                assertThat(registeredLauncher).isNotNull()

                val createCall = createGooglePayPaymentMethodLauncherCalls.awaitItem()
                assertThat(createCall.activityResultLauncher).isEqualTo(registeredLauncher)

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
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = EmptyConfirmationLauncherArgs,
            result = GooglePayPaymentMethodLauncher.Result.Completed(
                paymentMethod = paymentMethod,
            ),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val successResult = result.asNextStep()

        assertThat(successResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)

        assertThat(successResult.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        val savedOption = successResult.confirmationOption.asSaved()

        assertThat(savedOption.paymentMethod).isEqualTo(savedOption.paymentMethod)
        assertThat(savedOption.optionsParams).isNull()
        assertThat(savedOption.originatedFromWallet).isTrue()
    }

    @Test
    fun `'toResult' should return 'Failed' when 'GooglePayLauncherResult' is 'Failed'`() = runTest {
        val definition = createGooglePayConfirmationDefinition()

        val exception = IllegalStateException("Failed!")
        val result = definition.toResult(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = EmptyConfirmationLauncherArgs,
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
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = EmptyConfirmationLauncherArgs,
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
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = EmptyConfirmationLauncherArgs,
            result = GooglePayPaymentMethodLauncher.Result.Canceled,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
    }

    @Test
    fun `'Fail' action should be returned if currency code is not provided with a setup intent`() =
        runActionTest(
            merchantCurrencyCode = null,
            test = ::assertFailActionFromCurrencyFailure,
        )

    @Test
    fun `'Fail' action should be returned if currency code is not provided with a deferred intent in setup mode`() =
        runActionTest(
            intent = SetupIntentFactory.createDeferredIntent(),
            merchantCurrencyCode = null,
            test = ::assertFailActionFromCurrencyFailure,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a setup intent`() =
        runActionTest(
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a deferred intent in setup mode`() =
        runActionTest(
            intent = SetupIntentFactory.createDeferredIntent(),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is not provided with a payment intent`() =
        runActionTest(
            merchantCurrencyCode = null,
            intent = PaymentIntentFactory.create(),
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with a payment intent`() =
        runActionTest(
            intent = PaymentIntentFactory.create(),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is not provided with deferred intent in payment mode`() =
        runActionTest(
            merchantCurrencyCode = null,
            intent = PaymentIntentFactory.createDeferred(),
            test = ::assertLaunchAction,
        )

    @Test
    fun `'Launch' action should be returned if currency code is provided with deferred intent in payment mode`() =
        runActionTest(
            intent = PaymentIntentFactory.createDeferred(),
            merchantCurrencyCode = "USD",
            test = ::assertLaunchAction,
        )

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is disabled, existingPaymentMethodRequired should be true`() =
        runExistingPaymentMethodRequiredTest(
            allowNoExistingPaymentMethodForGooglePay = false,
            existingPaymentMethodRequired = true,
        )

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is enabled, existingPaymentMethodRequired should be false`() =
        runExistingPaymentMethodRequiredTest(
            allowNoExistingPaymentMethodForGooglePay = true,
            existingPaymentMethodRequired = false,
        )

    @Test
    fun `On 'launch', should present with expected parameters`() =
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
    fun `On 'launch', should present with required billing parameters, prod env, and expected card filter`() =
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
                    isEmailRequired = true,
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
    fun `On 'launch', should present with no billing parameters`() =
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

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `On 'launch', should present with expected merchant name from seller`() = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.copy(
                    sellerBusinessName = "My business, Inc.",
                ),
            ),
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher).present(
            currencyCode = "usd",
            amount = 1000L,
            config = launcherConfig(merchantName = "My business, Inc."),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    @Test
    fun `On 'launch', should use payment intent currency code if available`() = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantCurrencyCode = "USD",
                ),
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PAYMENT_INTENT.copy(currency = "CAD")
                ),
            ),
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher, times(1)).present(
            currencyCode = "CAD",
            amount = 1000L,
            config = launcherConfig(),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    @Test
    fun `On 'launch', should use payment intent currency & amount`() = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantCurrencyCode = "USD",
                    customLabel = "Merchant Inc."
                ),
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PAYMENT_INTENT.copy(currency = "CAD")
                ),
            ),
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher, times(1)).present(
            currencyCode = "CAD",
            amount = 1000L,
            config = launcherConfig(),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = "Merchant Inc.",
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    @Test
    fun `On 'launch', should use set currency & custom amount when using setup intent`() = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantCurrencyCode = "USD",
                    customAmount = 2099L,
                    customLabel = "Merchant Inc."
                ),
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = SetupIntentFactory.create(),
                ),
            ),
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher, times(1)).present(
            currencyCode = "USD",
            amount = 2099L,
            config = launcherConfig(),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = "Merchant Inc.",
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    @Test
    fun `On 'launch', should pass display items to present`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayItems = listOf(
            GooglePayDisplayItem(
                label = "Widget".resolvableString,
                type = com.stripe.android.GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 2000L,
            ),
            GooglePayDisplayItem(
                label = "Tax".resolvableString,
                type = com.stripe.android.GooglePayJsonFactory.DisplayItem.Type.TAX,
                price = 500L,
            ),
        )
        val resolvedDisplayItems = displayItems.map { displayItem ->
            displayItem.resolve(context)
        }

        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition(context = context)

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    merchantCurrencyCode = "USD",
                    displayItems = displayItems,
                ),
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PAYMENT_INTENT.copy(currency = "USD")
                ),
            ),
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher, times(1)).present(
            currencyCode = "USD",
            amount = 1000L,
            config = launcherConfig(),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = resolvedDisplayItems,
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    @Test
    fun `On 'launch', should pass shipping address parameters to present`() = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()
        val shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
            isRequired = true,
            allowedCountryCodes = setOf("US", "CA"),
            phoneNumberRequired = true,
        )

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION.copy(
                config = GOOGLE_PAY_CONFIRMATION_OPTION.config.copy(
                    shippingAddressParameters = shippingAddressParameters,
                ),
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS,
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher).present(
            currencyCode = "usd",
            amount = 1000L,
            config = launcherConfig(),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = shippingAddressParameters,
        )
    }

    private fun runActionTest(
        merchantCurrencyCode: String?,
        intent: StripeIntent = SetupIntentFactory.create(),
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
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = intent
                ),
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
        merchantCountryCodeShouldBe: String,
        billingAddressShouldBeRequired: Boolean,
        phoneNumberShouldBeRequired: Boolean,
        emailShouldBeRequired: Boolean,
        billingAddressFormatShouldBe: GooglePayPaymentMethodLauncher.BillingAddressConfig.Format,
        cardBrandFilterShouldBe: CardBrandFilter
    ) = runTest {
        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = confirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher).present(
            currencyCode = "usd",
            amount = 1000L,
            config = launcherConfig(
                environment = environmentShouldBe,
                merchantCountryCode = merchantCountryCodeShouldBe,
                merchantName = merchantNameShouldBe,
                isEmailRequired = emailShouldBeRequired,
                billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                    isRequired = billingAddressShouldBeRequired,
                    format = billingAddressFormatShouldBe,
                    isPhoneNumberRequired = phoneNumberShouldBeRequired,
                ),
            ),
            cardBrandFilter = cardBrandFilterShouldBe,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
    }

    private fun runExistingPaymentMethodRequiredTest(
        allowNoExistingPaymentMethodForGooglePay: Boolean,
        existingPaymentMethodRequired: Boolean,
    ) = runTest {
        allowNoExistingPaymentMethodForGooglePayRule.setEnabled(allowNoExistingPaymentMethodForGooglePay)

        val launcher = mock<InternalGooglePayPaymentMethodLauncher>()
        val definition = createGooglePayConfirmationDefinition()

        definition.launch(
            confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            arguments = EmptyConfirmationLauncherArgs,
            launcher = launcher,
        )

        verify(launcher).present(
            currencyCode = "usd",
            amount = 1000L,
            config = launcherConfig(existingPaymentMethodRequired = existingPaymentMethodRequired),
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
            transactionId = "pi_12345",
            label = null,
            isElements = true,
            publishableKey = null,
            displayItems = emptyList(),
            billingEmailOverride = null,
            shippingAddressParameters = null,
        )
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

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<EmptyConfirmationLauncherArgs>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.receivesResultInProcess).isTrue()
        assertThat(launchAction.launcherArguments).isEqualTo(EmptyConfirmationLauncherArgs)
    }

    private fun launcherConfig(
        environment: GooglePayEnvironment = GooglePayEnvironment.Test,
        merchantCountryCode: String = "US",
        merchantName: String = "Test merchant Inc.",
        isEmailRequired: Boolean = false,
        existingPaymentMethodRequired: Boolean = true,
        billingAddressConfig: GooglePayPaymentMethodLauncher.BillingAddressConfig =
            GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                isPhoneNumberRequired = false,
            ),
    ): GooglePayPaymentMethodLauncher.Config {
        return GooglePayPaymentMethodLauncher.Config(
            environment = environment,
            merchantCountryCode = merchantCountryCode,
            merchantName = merchantName,
            isEmailRequired = isEmailRequired,
            billingAddressConfig = billingAddressConfig,
            existingPaymentMethodRequired = existingPaymentMethodRequired,
            additionalEnabledNetworks = emptyList(),
        )
    }

    private fun createGooglePayConfirmationDefinition(
        googlePayPaymentMethodLauncherFactory: InternalGooglePayPaymentMethodLauncherFactory =
            RecordingInternalGooglePayPaymentMethodLauncherFactory.noOp(launcher = mock()),
        userFacingLogger: UserFacingLogger = FakeUserFacingLogger(),
        context: Context = ApplicationProvider.getApplicationContext(),
    ): GooglePayConfirmationDefinition {
        return GooglePayConfirmationDefinition(
            context = context,
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
            userFacingLogger = userFacingLogger,
        )
    }

    @Parcelize
    private object FakeCardBrandFilter : CardBrandFilter {
        override fun isAccepted(cardBrand: CardBrand): Boolean {
            return false
        }

        override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
            return false
        }
    }

    private class ActionScenario(
        val action: ConfirmationDefinition.Action<EmptyConfirmationLauncherArgs>,
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
                cardFundingFilter = DefaultCardFundingFilter,
            ),
        )

        private val CONFIRMATION_PARAMETERS =
            com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
    }
}
