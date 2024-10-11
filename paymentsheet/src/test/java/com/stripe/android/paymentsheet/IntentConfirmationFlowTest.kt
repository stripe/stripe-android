package com.stripe.android.paymentsheet

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
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.testing.FakePaymentLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class IntentConfirmationFlowTest {
    @Test
    fun `On payment intent, action should be to confirm intent`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        val action = intentConfirmationDefinition.action(
            confirmationOption = PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
                shippingDetails = AddressDetails(
                    name = "John Doe",
                    phoneNumber = "1234567890"
                ),
            ),
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
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
            confirmationOption = PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = PaymentSheet.InitializationMode.SetupIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
                shippingDetails = AddressDetails(
                    name = "John Doe",
                    phoneNumber = "1234567890"
                ),
            ),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
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
    fun `On deferred intent, action should be complete if completing without confirming`() = runTest {
        val intentConfirmationDefinition = createIntentConfirmationDefinition()

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(
                clientSecret = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
            )
        }

        val confirmationOption = createDeferredConfirmationOption()

        val action = intentConfirmationDefinition.action(
            confirmationOption = confirmationOption,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )

        val completeAction = action.asComplete()

        assertThat(completeAction.intent).isEqualTo(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD)
        assertThat(completeAction.confirmationOption).isEqualTo(confirmationOption)
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

        val confirmationOption = createDeferredConfirmationOption()

        val action = intentConfirmationDefinition.action(
            confirmationOption = confirmationOption.copy(
                shouldSave = true,
            ),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
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

        val confirmationOption = createDeferredConfirmationOption()

        val action = intentConfirmationDefinition.action(
            confirmationOption = confirmationOption,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("An error occurred!")
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(PaymentConfirmationErrorType.Payment)
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

        val confirmationOption = createDeferredConfirmationOption()

        val action = intentConfirmationDefinition.action(
            confirmationOption = confirmationOption,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("An error occurred!")
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(PaymentConfirmationErrorType.Payment)
    }

    private fun createDeferredConfirmationOption(): PaymentConfirmationOption.PaymentMethod.New {
        return PaymentConfirmationOption.PaymentMethod.New(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "USD",
                    )
                )
            ),
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            shouldSave = false,
            shippingDetails = AddressDetails(
                name = "John Doe",
                phoneNumber = "1234567890"
            ),
        )
    }

    private fun createIntentConfirmationDefinition(
        createPaymentMethodResult: Result<PaymentMethod> = Result.success(CARD_PAYMENT_METHOD),
        intentResult: Result<StripeIntent> =
            Result.success(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD),
    ): IntentConfirmationDefinition {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptor = DefaultIntentConfirmationInterceptor(
                isFlowController = false,
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
            ),
            paymentLauncherFactory = {
                FakePaymentLauncher()
            }
        )
    }

    private fun ConfirmStripeIntentParams.asSetup(): ConfirmSetupIntentParams {
        return this as ConfirmSetupIntentParams
    }

    private fun IntentConfirmationDefinition.Args.asConfirm(): IntentConfirmationDefinition.Args.Confirm {
        return this as IntentConfirmationDefinition.Args.Confirm
    }

    private fun <TLauncherArgs> PaymentConfirmationDefinition.ConfirmationAction<TLauncherArgs>.asFail():
        PaymentConfirmationDefinition.ConfirmationAction.Fail<TLauncherArgs> {
        return this as PaymentConfirmationDefinition.ConfirmationAction.Fail<TLauncherArgs>
    }

    private fun <TLauncherArgs> PaymentConfirmationDefinition.ConfirmationAction<TLauncherArgs>.asComplete():
        PaymentConfirmationDefinition.ConfirmationAction.Complete<TLauncherArgs> {
        return this as PaymentConfirmationDefinition.ConfirmationAction.Complete<TLauncherArgs>
    }

    private fun <TLauncherArgs> PaymentConfirmationDefinition.ConfirmationAction<TLauncherArgs>.asLaunch():
        PaymentConfirmationDefinition.ConfirmationAction.Launch<TLauncherArgs> {
        return this as PaymentConfirmationDefinition.ConfirmationAction.Launch<TLauncherArgs>
    }
}
