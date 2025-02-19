package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakePaymentLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class IntentConfirmationFlowTest {
    @Test
    fun `On payment intent, action should be to confirm intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
            ),
            confirmationParameters = defaultConfirmationDefinitionParams(
                initializationMode = defaultPaymentIntentInitializationMode,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(
            IntentConfirmationDefinition.Args.Confirm(
                confirmNextParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret = "pi_123_secret_123",
                    shipping = ConfirmPaymentIntentParams.Shipping(
                        name = "John Doe",
                        phone = "1234567890",
                        address = Address(),
                    ),
                    savePaymentMethod = null,
                    setupFutureUsage = null,
                )
            )
        )
    }

    @Test
    fun `On setup intent, action should be to confirm intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
            ),
            confirmationParameters = defaultConfirmationDefinitionParams(
                initializationMode = defaultSetupIntentInitializationMode,
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(
            IntentConfirmationDefinition.Args.Confirm(
                confirmNextParams = ConfirmSetupIntentParams.create(
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret = "seti_123_secret_123",
                )
            )
        )
    }

    @Test
    fun `On deferred intent, action should be complete if completing without confirming`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationParameters = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val completeAction = action.asComplete()

        assertThat(completeAction.intent).isEqualTo(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD)
        assertThat(completeAction.confirmationOption).isEqualTo(CONFIRMATION_OPTION)
        assertThat(completeAction.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.None)
    }

    @Test
    fun `On deferred intent, action should be confirm if completing intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "seti_123_secret_123"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION.copy(
                shouldSave = true,
            ),
            confirmationParameters = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val launchAction = action.asLaunch()
        val confirmArguments = launchAction.launcherArguments.asConfirm()
        val setupIntentParams = confirmArguments.confirmNextParams.asSetup()

        assertThat(setupIntentParams.clientSecret).isEqualTo("seti_123_secret_123")
        assertThat(launchAction.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On deferred intent, action should be fail if failed to create payment method`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition(
            createPaymentMethodResult = Result.failure(IllegalStateException("An error occurred!"))
        )

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationParameters = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("An error occurred!")
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `On deferred intent, action should be fail if failed to retrieve intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition(
            intentResult = Result.failure(IllegalStateException("An error occurred!"))
        )

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "si_123_secret_123"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationParameters = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("An error occurred!")
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `setup intent, should be created with setAsDefault true`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = true,
                extraParams = PaymentMethodExtraParams.Card(
                    setAsDefault = true
                ),
            ),
            confirmationParameters = defaultConfirmationDefinitionParams(
                initializationMode = defaultSetupIntentInitializationMode,
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val actualParams = action.asLaunch().launcherArguments.asConfirm().confirmNextParams

        assertThat(actualParams).isInstanceOf(ConfirmSetupIntentParams::class.java)
        assertThat((actualParams as ConfirmSetupIntentParams).clientSecret).isEqualTo("seti_123_secret_123")
        assertThat(actualParams.setAsDefaultPaymentMethod).isEqualTo(true)
    }

    @Test
    fun `payment intent, should be created with setAsDefault true`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = true,
                extraParams = PaymentMethodExtraParams.Card(
                    setAsDefault = true
                ),
            ),
            confirmationParameters = defaultConfirmationDefinitionParams(
                initializationMode = defaultPaymentIntentInitializationMode,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val actualParams = action.asLaunch().launcherArguments.asConfirm().confirmNextParams

        assertThat(actualParams).isInstanceOf(ConfirmPaymentIntentParams::class.java)
        assertThat((actualParams as ConfirmPaymentIntentParams).paymentMethodCreateParams).isEqualTo(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        assertThat(actualParams.clientSecret).isEqualTo("pi_123_secret_123")
        assertThat(actualParams.setAsDefaultPaymentMethod).isEqualTo(true)
    }

    private fun createIntentConfirmationDefinition(
        createPaymentMethodResult: Result<PaymentMethod> = Result.success(CARD_PAYMENT_METHOD),
        intentResult: Result<StripeIntent> =
            Result.success(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD),
    ): IntentConfirmationDefinition {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptor = DefaultIntentConfirmationInterceptor(
                allowsManualConfirmation = false,
                publishableKeyProvider = {
                    "pk_123"
                },
                stripeAccountIdProvider = {
                    "acct_123"
                },
                stripeRepository = FakeStripeRepository(
                    createPaymentMethodResult = createPaymentMethodResult,
                    retrieveIntent = intentResult,
                ),
                errorReporter = FakeErrorReporter(),
            ),
            paymentLauncherFactory = {
                FakePaymentLauncher()
            }
        )
    }

    private val defaultSetupIntentInitializationMode =
        PaymentElementLoader.InitializationMode.SetupIntent(
            clientSecret = "seti_123_secret_123",
        )

    private val defaultPaymentIntentInitializationMode =
        PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = "pi_123_secret_123",
        )

    private fun defaultConfirmationDefinitionParams(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
    ) : ConfirmationDefinition.Parameters {
        return ConfirmationDefinition.Parameters(
            initializationMode = initializationMode,
            intent = intent,
            shippingDetails = AddressDetails(
                name = "John Doe",
                phoneNumber = "1234567890"
            ),
            appearance = PaymentSheet.Appearance(),
        )
    }

    private fun ConfirmStripeIntentParams.asSetup(): ConfirmSetupIntentParams {
        return this as ConfirmSetupIntentParams
    }

    private fun IntentConfirmationDefinition.Args.asConfirm(): IntentConfirmationDefinition.Args.Confirm {
        return this as IntentConfirmationDefinition.Args.Confirm
    }

    private fun <TLauncherArgs> ConfirmationDefinition.Action<TLauncherArgs>.asFail():
        ConfirmationDefinition.Action.Fail<TLauncherArgs> {
        return this as ConfirmationDefinition.Action.Fail<TLauncherArgs>
    }

    private fun <TLauncherArgs> ConfirmationDefinition.Action<TLauncherArgs>.asComplete():
        ConfirmationDefinition.Action.Complete<TLauncherArgs> {
        return this as ConfirmationDefinition.Action.Complete<TLauncherArgs>
    }

    private fun <TLauncherArgs> ConfirmationDefinition.Action<TLauncherArgs>.asLaunch():
        ConfirmationDefinition.Action.Launch<TLauncherArgs> {
        return this as ConfirmationDefinition.Action.Launch<TLauncherArgs>
    }

    private companion object {
        val CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            shouldSave = false,
        )

        val DEFERRED_CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "USD",
                    )
                )
            ),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            shippingDetails = AddressDetails(
                name = "John Doe",
                phoneNumber = "1234567890"
            ),
            appearance = PaymentSheet.Appearance(),
        )
    }
}
