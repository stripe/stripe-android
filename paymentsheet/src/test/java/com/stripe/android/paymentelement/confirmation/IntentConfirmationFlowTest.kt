package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ClientAttributionMetadata
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
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.testing.FakePaymentLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class IntentConfirmationFlowTest {
    @Test
    fun `On payment intent, action should be to confirm intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
                extraParams = null,
            ),
            confirmationArgs = defaultConfirmationDefinitionParams(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isInstanceOf(IntentConfirmationDefinition.Args.Confirm::class.java)
    }

    @Test
    fun `On setup intent, action should be to confirm intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
                extraParams = null,
            ),
            confirmationArgs = defaultConfirmationDefinitionParams(
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isInstanceOf(
            IntentConfirmationDefinition.Args.Confirm::class.java
        )
    }

    @Test
    fun `On deferred intent, action should be complete if completing without confirming`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val completeAction = action.asComplete()

        assertThat(completeAction.intent)
            .isEqualTo(DEFERRED_CONFIRMATION_PARAMETERS.paymentMethodMetadata.stripeIntent)
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.None)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `On deferred intent, action should be complete in uncomplete flow if preparing payment method`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition(
            preparePaymentMethodHandler = { _, _ ->
                // No-op
            }
        )

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = DEFERRED_CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = DEFERRED_CONFIRMATION_PARAMETERS.paymentMethodMetadata.copy(
                    integrationMetadata = IntegrationMetadata.DeferredIntent.WithSharedPaymentToken(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Setup(
                                currency = "USD",
                            ),
                            sellerDetails = null,
                        )
                    )
                ),
            ),
        )

        val completeAction = action.asComplete()

        assertThat(completeAction.intent)
            .isEqualTo(DEFERRED_CONFIRMATION_PARAMETERS.paymentMethodMetadata.stripeIntent)
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.None)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isFalse()
    }

    @Test
    fun `On deferred intent, action should be confirm if completing intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "seti_123_secret_123"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION.copy(
                shouldSave = true,
            ),
            confirmationArgs = DEFERRED_CONFIRMATION_PARAMETERS,
        )

        val launchAction = action.asLaunch()
        val confirmArguments = launchAction.launcherArguments.asConfirm()
        val setupIntentParams = confirmArguments.confirmNextParams.asSetup()

        assertThat(setupIntentParams.clientSecret).isEqualTo("seti_123_secret_123")
        assertThat(confirmArguments.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On deferred intent, action should be fail if failed to create payment method`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition(
            createPaymentMethodResult = Result.failure(IllegalStateException("An error occurred!"))
        ) { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = DEFERRED_CONFIRMATION_PARAMETERS,
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
        ) { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "si_123_secret_123"
            )
        }

        val action = intentConfirmationDefinition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = DEFERRED_CONFIRMATION_PARAMETERS,
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
            confirmationArgs = defaultConfirmationDefinitionParams(
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val actualParams = action.asLaunch().launcherArguments.asConfirm().confirmNextParams

        assertThat(actualParams).isInstanceOf(ConfirmSetupIntentParams::class.java)
        assertThat((actualParams as ConfirmSetupIntentParams).clientSecret)
            .isEqualTo("seti_1GSmaFCRMbs6FrXfmjThcHan_secret_H0oC2iSB4FtW4d")
        assertThat(actualParams.toParamMap().get("set_as_default_payment_method")).isEqualTo(true)
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
            confirmationArgs = defaultConfirmationDefinitionParams(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val actualParams = action.asLaunch().launcherArguments.asConfirm().confirmNextParams

        assertThat(actualParams).isInstanceOf(ConfirmPaymentIntentParams::class.java)
        assertThat((actualParams as ConfirmPaymentIntentParams).paymentMethodCreateParams)
            .isEqualTo(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        assertThat(actualParams.clientSecret)
            .isEqualTo("pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s")
        assertThat(actualParams.toParamMap().get("set_as_default_payment_method")).isEqualTo(true)
    }

    private fun createIntentConfirmationDefinition(
        createPaymentMethodResult: Result<PaymentMethod> = Result.success(CARD_PAYMENT_METHOD),
        intentResult: Result<StripeIntent> =
            Result.success(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD),
        preparePaymentMethodHandler: PreparePaymentMethodHandler? = null,
        createIntentCallback: CreateIntentCallback? = null,
    ): IntentConfirmationDefinition {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptorFactory =
            object : IntentConfirmationInterceptor.Factory {
                override suspend fun create(
                    integrationMetadata: IntegrationMetadata,
                    customerId: String?,
                    ephemeralKeySecret: String?,
                    clientAttributionMetadata: ClientAttributionMetadata,
                ): IntentConfirmationInterceptor {
                    return createIntentConfirmationInterceptor(
                        integrationMetadata = integrationMetadata,
                        stripeRepository = FakeStripeRepository(
                            createPaymentMethodResult = createPaymentMethodResult,
                            retrieveIntent = intentResult,
                        ),
                        intentCreationCallbackProvider = {
                            createIntentCallback
                        },
                        preparePaymentMethodHandlerProvider = { preparePaymentMethodHandler }
                    )
                }
            },
            paymentLauncherFactory = {
                FakePaymentLauncher()
            }
        )
    }

    private fun defaultConfirmationDefinitionParams(
        intent: StripeIntent,
    ): ConfirmationHandler.Args {
        return ConfirmationHandler.Args(
            confirmationOption = FakeConfirmationOption(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = intent,
                shippingDetails = AddressDetails(
                    name = "John Doe",
                    phoneNumber = "1234567890"
                ),
            ),
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
            extraParams = null,
        )

        val DEFERRED_CONFIRMATION_PARAMETERS = ConfirmationHandler.Args(
            confirmationOption = FakeConfirmationOption(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(id = null, clientSecret = null),
                shippingDetails = AddressDetails(
                    name = "John Doe",
                    phoneNumber = "1234567890"
                ),
            ),
        )
    }
}
