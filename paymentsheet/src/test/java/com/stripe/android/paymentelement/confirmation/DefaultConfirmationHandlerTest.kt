package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.Address
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakePaymentLauncher
import com.stripe.android.utils.FakeExternalPaymentMethodLauncher
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakeResultHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

@RunWith(AndroidJUnit4::class)
class DefaultConfirmationHandlerTest {
    @Test
    fun `On 'init', state should be idle`() = runTest {
        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On 'start' with existing payment method, should call interceptor properly`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "ci_123")
        val shippingDetails = AddressDetails(
            name = "John Doe",
            phoneNumber = "11234567890",
            address = PaymentSheet.Address(
                line1 = "123 Apple Street",
                line2 = "Unit 47",
                city = "South San Francisco",
                state = "CA",
                country = "US",
                postalCode = "99899",
            )
        )
        val savedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
            cvc = "507"
        )

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        defaultConfirmationHandler.start(
            arguments = ConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    initializationMode = initializationMode,
                    shippingDetails = shippingDetails,
                    paymentMethod = savedPaymentMethod,
                    optionsParams = paymentMethodOptionsParams,
                ),
            ),
        )

        val call = interceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                initializationMode = initializationMode,
                shippingValues = ConfirmPaymentIntentParams.Shipping(
                    name = "John Doe",
                    address = Address.Builder()
                        .setLine1("123 Apple Street")
                        .setLine2("Unit 47")
                        .setCity("South San Francisco")
                        .setState("CA")
                        .setCountry("US")
                        .setPostalCode("99899")
                        .build(),
                    phone = "11234567890"
                ),
                paymentMethod = savedPaymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
            )
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On 'start', state should be updated to confirming`() = runTest {
        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = ConfirmationHandler.Args(
                    intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    confirmationOption = PaymentMethodConfirmationOption.Saved(
                        initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                            clientSecret = "pi_456_secret_456"
                        ),
                        shippingDetails = null,
                        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        optionsParams = null,
                    ),
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = DEFAULT_ARGUMENTS.confirmationOption,
                )
            )

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On 'start' with new payment method, should call interceptor properly`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "ci_123")
        val newCard = PaymentMethodCreateParams.createCard(
            CardParams(
                number = "4242424242424242",
                expMonth = 5,
                expYear = 2035,
            )
        )

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        defaultConfirmationHandler.start(
            arguments = ConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentMethodConfirmationOption.New(
                    initializationMode = initializationMode,
                    shippingDetails = null,
                    createParams = newCard,
                    optionsParams = null,
                    shouldSave = true,
                ),
            ),
        )

        val call = interceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithNewPaymentMethod(
                initializationMode = initializationMode,
                shippingValues = null,
                paymentMethodCreateParams = newCard,
                paymentMethodOptionsParams = null,
                customerRequestedSave = true,
            )
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On 'start' with deferred intent initialization, should call interceptor properly`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 100L,
                    currency = "CAD",
                    setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    captureMethod = PaymentSheet.IntentConfiguration.CaptureMethod.AutomaticAsync,
                ),
                paymentMethodTypes = listOf("card"),
                onBehalfOf = "Merchant",
            )
        )

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        defaultConfirmationHandler.start(
            arguments = ConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    initializationMode = initializationMode,
                    shippingDetails = null,
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = null,
                ),
            ),
        )

        val call = interceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                initializationMode = initializationMode,
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                paymentMethodOptionsParams = null,
                shippingValues = null,
            )
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On 'start' while handler is already confirming, should not do anything`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()
        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        interceptor.calls.skipItems(1)

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On intercepted intent complete, should receive 'Succeeded' result through 'awaitIntentResult'`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        interceptor.enqueueCompleteStep()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val result = defaultConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
            )
        )
    }

    @Test
    fun `On intercepted intent next step failed, should be 'Failed' result through 'awaitIntentResult'`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val cause = IllegalStateException("An error occurred!")
        val message = "Could not continue intent confirmation!"

        interceptor.enqueueFailureStep(
            cause = cause,
            message = message,
        )

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val result = defaultConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Failed(
                cause = cause,
                message = message.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `On confirmation attempt without registering callbacks, should return 'Failed' result`() =
        runTest {
            val interceptor = FakeIntentConfirmationInterceptor()
            val paymentLauncher = FakePaymentLauncher()

            interceptor.enqueueConfirmStep(ConfirmPaymentIntentParams.create(clientSecret = "pi_1234"))

            val defaultConfirmationHandler = createDefaultConfirmationHandler(
                intentConfirmationInterceptor = interceptor,
                paymentLauncher = paymentLauncher,
                shouldRegister = false,
            )

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            val result = defaultConfirmationHandler.awaitIntentResult()

            val failedResult = result.asFailed()

            val message = "No launcher for IntentConfirmationDefinition was found, did you call register?"

            assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
            assertThat(failedResult.cause.message).isEqualTo(message)
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
        }

    @Test
    fun `On 'PaymentIntent' requires confirmation, should call 'PaymentLauncher' to handle confirmation`() = runTest {
        val confirmParams = ConfirmPaymentIntentParams.create(clientSecret = "pi_1234")
        val paymentLauncher = FakePaymentLauncher()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(confirmParams)
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val call = paymentLauncher.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakePaymentLauncher.Call.Confirm.PaymentIntent(
                params = confirmParams
            )
        )
    }

    @Test
    fun `On 'SetupIntent' requires confirmation, should call 'PaymentLauncher' to handle confirmation`() = runTest {
        val confirmParams = ConfirmSetupIntentParams.create(
            clientSecret = "pi_1234",
            paymentMethodType = PaymentMethod.Type.Card
        )
        val paymentLauncher = FakePaymentLauncher()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(confirmParams)
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val call = paymentLauncher.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakePaymentLauncher.Call.Confirm.SetupIntent(
                params = confirmParams
            )
        )
    }

    @Test
    fun `On 'PaymentIntent' requires next action, should call 'PaymentLauncher' to handle next action`() = runTest {
        val paymentLauncher = FakePaymentLauncher()
        val clientSecret = "pi_1234"
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep(clientSecret)
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val call = paymentLauncher.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakePaymentLauncher.Call.HandleNextAction.PaymentIntent(
                clientSecret = clientSecret
            )
        )
    }

    @Test
    fun `On 'SetupIntent' requires next action, should call 'PaymentLauncher' to handle next action`() = runTest {
        val paymentLauncher = FakePaymentLauncher()
        val clientSecret = "pi_1234"
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep(clientSecret)
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )

        val call = paymentLauncher.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakePaymentLauncher.Call.HandleNextAction.SetupIntent(
                clientSecret = clientSecret
            )
        )
    }

    @Test
    fun `On payment launcher result succeeded, should be 'Succeeded' result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = DEFAULT_ARGUMENTS.confirmationOption,
                )
            )

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

            val expectedResult = ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment launcher result canceled, should be 'Canceled' result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = DEFAULT_ARGUMENTS.confirmationOption,
                )
            )

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Canceled)

            val expectedResult = ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment launcher result failed, should be 'Failed' result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = DEFAULT_ARGUMENTS.confirmationOption,
                )
            )

            val cause = IllegalStateException("This is a failure!")

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Failed(cause))

            val expectedResult = ConfirmationHandler.Result.Failed(
                cause = cause,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment confirm, should store 'AwaitingConfirmationResult' data in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val data = savedStateHandle
            .get<DefaultConfirmationHandler.AwaitingConfirmationResultData>("AwaitingConfirmationResult")

        assertThat(data?.confirmationOption).isEqualTo(DEFAULT_ARGUMENTS.confirmationOption)
        assertThat(data?.receivesResultInProcess).isFalse()
    }

    @Test
    fun `On payment handle next action, should store 'AwaitingConfirmationResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep("pi_123")
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val data = savedStateHandle
            .get<DefaultConfirmationHandler.AwaitingConfirmationResultData>("AwaitingConfirmationResult")

        assertThat(data?.confirmationOption).isEqualTo(DEFAULT_ARGUMENTS.confirmationOption)
        assertThat(data?.receivesResultInProcess).isFalse()
    }

    @Test
    fun `On launch EPMs handler, should store 'AwaitingConfirmationResult' in 'SavedStateHandle'`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep("pi_123")
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD
            ),
        )

        val data = savedStateHandle
            .get<DefaultConfirmationHandler.AwaitingConfirmationResultData>("AwaitingConfirmationResult")

        assertThat(data?.confirmationOption).isEqualTo(EXTERNAL_PAYMENT_METHOD)
        assertThat(data?.receivesResultInProcess).isFalse()
    }

    @Test
    fun `On start Bacs mandate, should store 'AwaitingConfirmationResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,

        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = BACS_OPTION,
            ),
        )

        val data = savedStateHandle
            .get<DefaultConfirmationHandler.AwaitingConfirmationResultData>("AwaitingConfirmationResult")

        assertThat(data?.confirmationOption).isEqualTo(BACS_OPTION)
        assertThat(data?.receivesResultInProcess).isTrue()
    }

    @Test
    fun `On start Google Pay, should store 'AwaitingConfirmationResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = GOOGLE_PAY_OPTION,
            ),
        )

        val data = savedStateHandle
            .get<DefaultConfirmationHandler.AwaitingConfirmationResultData>("AwaitingConfirmationResult")

        assertThat(data?.confirmationOption).isEqualTo(GOOGLE_PAY_OPTION)
        assertThat(data?.receivesResultInProcess).isTrue()
    }

    @Test
    fun `On init with 'SavedStateHandle' awaiting result, should timeout & cancel after 1 second`() = runTest {
        val dispatcher = StandardTestDispatcher()

        val savedStateHandle = SavedStateHandle().apply {
            set(
                "AwaitingConfirmationResult",
                DefaultConfirmationHandler.AwaitingConfirmationResultData(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                    receivesResultInProcess = false,
                ),
            )
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,
            coroutineScope = CoroutineScope(dispatcher)
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Confirming(EXTERNAL_PAYMENT_METHOD))

            dispatcher.scheduler.advanceTimeBy(delayTime = 1.01.seconds)

            val expectedResult = ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On init with 'SavedStateHandle' awaiting result in process, continue to wait until result`() = runTest {
        val dispatcher = StandardTestDispatcher()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueCompleteStep()
        }

        val bacsArguments = DEFAULT_ARGUMENTS.copy(
            confirmationOption = BACS_OPTION,
        )

        val savedStateHandle = SavedStateHandle().apply {
            set(
                "AwaitingConfirmationResult",
                DefaultConfirmationHandler.AwaitingConfirmationResultData(
                    confirmationOption = BACS_OPTION,
                    receivesResultInProcess = true,
                ),
            )
            set("PaymentConfirmationArguments", bacsArguments)
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,
            intentConfirmationInterceptor = interceptor,
            coroutineScope = CoroutineScope(dispatcher)
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = BACS_OPTION,
                )
            )

            dispatcher.scheduler.advanceTimeBy(delayTime = 200.seconds)

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Confirmed)

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Confirming(NEW_BACS_PM_OPTION))

            val expectedResult = ConfirmationHandler.Result.Succeeded(
                intent = DEFAULT_ARGUMENTS.intent,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On init with 'SavedStateHandle' with incorrect option, error should be internal`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(
                "AwaitingConfirmationResult",
                DefaultConfirmationHandler.AwaitingConfirmationResultData(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                    receivesResultInProcess = false,
                ),
            )
            set("IntentConfirmationArguments", DEFAULT_ARGUMENTS.copy(confirmationOption = EXTERNAL_PAYMENT_METHOD))
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val failedResult = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `On init with 'SavedStateHandle', should receive result through 'awaitIntentResult'`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(
                "AwaitingConfirmationResult",
                DefaultConfirmationHandler.AwaitingConfirmationResultData(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    receivesResultInProcess = false,
                ),
            )
            set(
                "IntentConfirmationParameters",
                ConfirmationMediator.Parameters(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    intent = DEFAULT_ARGUMENTS.intent,
                    deferredIntentConfirmationType = null,
                )
            )
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = defaultConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
    }

    @Test
    fun `On fail due to invalid deferred intent usage, should fail with expected integration error`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueFailureStep(
                cause = InvalidDeferredIntentUsageException(),
                message = "An error occurred!",
            )
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val failedResult = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(failedResult.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        assertThat(failedResult.message).isEqualTo("An error occurred!".resolvableString)
    }

    @Test
    fun `On successful confirm with deferred intent, should return 'Client' confirmation type`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(
                confirmParams = ConfirmPaymentIntentParams.create("pi_123"),
                isDeferred = true,
            )
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = defaultConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
            )
        )
    }

    @Test
    fun `On successful confirm with non-deferred intent, should return null confirmation type`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        interceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.create("pi_123"),
            isDeferred = false,
        )

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = DEFAULT_ARGUMENTS.confirmationOption,
                )
            )

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

            val expectedResult = ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM, should launch external PM handler with expected params`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val fakeExternalPaymentMethodLauncher = FakeExternalPaymentMethodLauncher()

        val defaultConfirmationHandler = createDefaultConfirmationHandler().apply {
            setExternalPaymentMethodLauncher(fakeExternalPaymentMethodLauncher)
        }

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD.copy(
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "John Doe",
                        address = Address(
                            city = "South San Francisco"
                        )
                    )
                )
            ),
        )

        val launch = fakeExternalPaymentMethodLauncher.calls.awaitItem()

        assertThat(launch).isEqualTo(
            FakeExternalPaymentMethodLauncher.Launch(
                input = ExternalPaymentMethodInput(
                    type = "paypal",
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "John Doe",
                        address = Address(
                            city = "South San Francisco",
                        ),
                    ),
                )
            )
        )
    }

    @Test
    fun `On external PM with no confirm handler, should return failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null

        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "externalPaymentMethodConfirmHandler is null. Cannot process payment for payment selection: paypal"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `On external PM with no launcher, should return failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            shouldRegister = false,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "No launcher for ExternalPaymentMethodConfirmationDefinition was found, did you call register?"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On external PM succeeded result, should return intent succeeded result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = EXTERNAL_PAYMENT_METHOD,
                )
            )

            epmsCallbackHandler.onResult(PaymentResult.Completed)

            val expectedResult = ConfirmationHandler.Result.Succeeded(
                intent = DEFAULT_ARGUMENTS.intent,
                deferredIntentConfirmationType = null,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM failed result, should return intent failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = EXTERNAL_PAYMENT_METHOD,
                )
            )

            val exception = APIException()

            epmsCallbackHandler.onResult(PaymentResult.Failed(exception))

            val expectedResult = ConfirmationHandler.Result.Failed(
                cause = exception,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM canceled result, should return intent canceled result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val defaultConfirmationHandler = createDefaultConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = EXTERNAL_PAYMENT_METHOD,
                )
            )

            epmsCallbackHandler.onResult(PaymentResult.Canceled)

            val expectedResult = ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On bacs payment method, should launch mandate screen`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()
        val interceptor = FakeIntentConfirmationInterceptor()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        ).apply {
            registerWithCallbacks()
        }

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(),
            ),
        )

        val call = bacsMandateConfirmationLauncher.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeBacsMandateConfirmationLauncher.Call(
                data = BacsMandateData(
                    accountNumber = "00012345",
                    sortCode = "108800",
                    name = "John Doe",
                    email = "johndoe@email.com",
                ),
                appearance = APPEARANCE,
            )
        )

        bacsMandateConfirmationLauncher.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On bacs payment method without registering callbacks, should fail intent confirmation`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
            shouldRegister = false
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
        assertThat(result.cause.message).isEqualTo("Required value was null.")
    }

    @Test
    fun `On missing name for Bacs, should fail with internal error`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(
                    name = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
        assertThat(result.cause.message).isEqualTo(
            "Given payment selection could not be converted to Bacs data!"
        )
    }

    @Test
    fun `On missing email for Bacs, should fail with internal error`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(
                    email = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = defaultConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
        assertThat(result.cause.message).isEqualTo(
            "Given payment selection could not be converted to Bacs data!"
        )
    }

    @Test
    fun `On Bacs mandate confirmed, should continue confirmation process`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = confirmationOption
            ),
        )

        bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Confirmed)

        val call = interceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithNewPaymentMethod(
                initializationMode = confirmationOption.initializationMode,
                paymentMethodCreateParams = confirmationOption.createParams,
                shippingValues = null,
                paymentMethodOptionsParams = null,
                customerRequestedSave = false,
            )
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On modify Bacs data event, should return canceled result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = confirmationOption
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = confirmationOption,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.ModifyDetails)

            interceptor.calls.expectNoEvents()

            val expectedResult = ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On cancel Bacs data event, should return canceled result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        defaultConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = confirmationOption,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = confirmationOption,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Cancelled)

            interceptor.calls.expectNoEvents()

            val expectedResult = ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )

            assertThat(defaultConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On start Google Pay with no currency and setup intent, should fail and log`() = runTest {
        val logger = FakeUserFacingLogger()
        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            logger = logger
        )

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = GOOGLE_PAY_OPTION.copy(
                    initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                        clientSecret = "si_123_secret_123",
                    ),
                    config = GOOGLE_PAY_OPTION.config.copy(
                        merchantCurrencyCode = null,
                    )
                ),
            ),
        )

        val result = defaultConfirmationHandler.awaitIntentResult().asFailed()

        val message = "GooglePayConfig.currencyCode is required in order to use " +
            "Google Pay when processing a Setup Intent"

        assertThat(result.cause.message).isEqualTo(message)
        assertThat(result.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration)
        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)

        assertThat(logger.getLoggedMessages()).contains(message)
    }

    @Test
    fun `On start Google Pay with PI, should create and launch Google Pay launcher properly`() = runTest {
        val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
        val googlePayPaymentMethodLauncherFactory = mock<GooglePayPaymentMethodLauncherFactory> {
            on {
                create(any(), any(), any(), any(), any(), eq(DefaultCardBrandFilter))
            } doReturn googlePayPaymentMethodLauncher
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory
        )

        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                intent = paymentIntent,
                confirmationOption = GOOGLE_PAY_OPTION.copy(
                    config = GOOGLE_PAY_OPTION.config.copy(
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                        )
                    )
                ),
            ),
        )

        verify(googlePayPaymentMethodLauncherFactory).create(
            lifecycleScope = any(),
            config = eq(
                GooglePayPaymentMethodLauncher.Config(
                    environment = GooglePayEnvironment.Production,
                    merchantName = "Merchant, Inc.",
                    merchantCountryCode = "US",
                    isEmailRequired = true,
                    billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                        isRequired = true,
                        format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                        isPhoneNumberRequired = true,
                    )
                )
            ),
            readyCallback = any(),
            activityResultLauncher = any(),
            skipReadyCheck = eq(true),
            cardBrandFilter = eq(DefaultCardBrandFilter)
        )

        verify(googlePayPaymentMethodLauncher).present(
            currencyCode = paymentIntent.currency!!,
            amount = paymentIntent.amount!!,
            label = "Merchant Payments",
            transactionId = paymentIntent.id
        )
    }

    @Test
    fun `On start Google Pay with SI, should create and launch Google Pay launcher properly`() = runTest {
        val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
        val googlePayPaymentMethodLauncherFactory = mock<GooglePayPaymentMethodLauncherFactory> {
            on {
                create(any(), any(), any(), any(), any(), eq(DefaultCardBrandFilter))
            } doReturn googlePayPaymentMethodLauncher
        }

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory
        )

        val setupIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD

        defaultConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                intent = setupIntent,
                confirmationOption = GOOGLE_PAY_OPTION,
            ),
        )

        verify(googlePayPaymentMethodLauncherFactory).create(
            lifecycleScope = any(),
            config = eq(
                GooglePayPaymentMethodLauncher.Config(
                    environment = GooglePayEnvironment.Production,
                    merchantName = "Merchant, Inc.",
                    merchantCountryCode = "US",
                    isEmailRequired = false,
                    billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig()
                )
            ),
            readyCallback = any(),
            activityResultLauncher = any(),
            skipReadyCheck = eq(true),
            cardBrandFilter = eq(DefaultCardBrandFilter)
        )

        verify(googlePayPaymentMethodLauncher).present(
            currencyCode = "USD",
            amount = 5000,
            label = "Merchant Payments",
            transactionId = setupIntent.id
        )
    }

    @Test
    fun `On Google Pay launcher succeeded, should proceed to intent confirmation`() = runTest {
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor()
        val googlePayCallbackHandler = FakeResultHandler<GooglePayPaymentMethodLauncher.Result>()
        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                googlePayCallbackHandler = googlePayCallbackHandler,
            )
        }

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = GOOGLE_PAY_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = GOOGLE_PAY_OPTION,
                )
            )

            val result = GooglePayPaymentMethodLauncher.Result.Completed(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )

            googlePayCallbackHandler.onResult(result)

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = createGooglePaySavedPaymentMethodOption(
                        option = GOOGLE_PAY_OPTION,
                        result = result,
                    ),
                )
            )

            val call = intentConfirmationInterceptor.calls.awaitItem()

            assertThat(call).isEqualTo(
                FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "pi_456_secret_456"
                    ),
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    paymentMethodOptionsParams = null,
                    shippingValues = null,
                )
            )
        }
    }

    @Test
    fun `On Google Pay succeeded, should return successful result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Completed(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        ),
        skipPaymentResultCheck = false,
        internalPaymentResult = InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
    }

    @Test
    fun `On Google Pay network failure, should return expected failed result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Failed(
            error = IllegalStateException("An error occurred"),
            errorCode = GooglePayPaymentMethodLauncher.NETWORK_ERROR,
        ),
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.cause)
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.message)
            .isEqualTo(PaymentsCoreR.string.stripe_failure_connection_error.resolvableString)
        assertThat(failedResult.type)
            .isEqualTo(
                ConfirmationHandler.Result.Failed.ErrorType.GooglePay(GooglePayPaymentMethodLauncher.NETWORK_ERROR)
            )
    }

    @Test
    fun `On Google Pay internal failure, should return expected failed result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Failed(
            error = IllegalStateException("An error occurred"),
            errorCode = GooglePayPaymentMethodLauncher.INTERNAL_ERROR,
        ),
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.cause)
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.message)
            .isEqualTo(PaymentsCoreR.string.stripe_internal_error.resolvableString)
        assertThat(failedResult.type)
            .isEqualTo(
                ConfirmationHandler.Result.Failed.ErrorType.GooglePay(GooglePayPaymentMethodLauncher.INTERNAL_ERROR)
            )
    }

    @Test
    fun `On Google Pay cancel, should return expected cancel result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Canceled,
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
    }

    @Test
    fun `On Google Pay failure while confirming intent, should return expected failed result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Completed(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        ),
        skipPaymentResultCheck = false,
        internalPaymentResult = InternalPaymentResult.Failed(
            throwable = IllegalStateException("An error occurred"),
        ),
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.cause)
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.message)
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type)
            .isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `On Google Pay canceled while confirming intent, should return expected cancel result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Completed(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        ),
        skipPaymentResultCheck = false,
        internalPaymentResult = InternalPaymentResult.Canceled,
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
    }

    @Test
    fun `On Bacs succeeded, should return successful result`() = runBacsTest(
        bacsMandateConfirmationResult = BacsMandateConfirmationResult.Confirmed,
        skipPaymentResultCheck = false,
        internalPaymentResult = InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
    }

    @Test
    fun `On Bacs modify details, should return expected cancel result`() = runBacsTest(
        bacsMandateConfirmationResult = BacsMandateConfirmationResult.ModifyDetails,
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails,
            )
        )
    }

    @Test
    fun `On Bacs cancel, should return expected cancel result`() = runBacsTest(
        bacsMandateConfirmationResult = BacsMandateConfirmationResult.Cancelled,
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        assertThat(result).isEqualTo(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        )
    }

    @Test
    fun `On Bacs cancelled while confirming intent, should return expected failed result`() = runBacsTest(
        bacsMandateConfirmationResult = BacsMandateConfirmationResult.Confirmed,
        skipPaymentResultCheck = false,
        internalPaymentResult = InternalPaymentResult.Failed(
            throwable = IllegalStateException("An error occurred"),
        ),
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.cause)
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.message)
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type)
            .isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    private fun runGooglePayTest(
        googlePayResult: GooglePayPaymentMethodLauncher.Result,
        internalPaymentResult: InternalPaymentResult?,
        skipPaymentResultCheck: Boolean,
        test: suspend (result: ConfirmationHandler.Result) -> Unit
    ) = runTest {
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(
                confirmParams = ConfirmPaymentIntentParams.create(
                    clientSecret = "pi_123_secret_123"
                )
            )
        }

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()
        val googlePayCallbackHandler = FakeResultHandler<GooglePayPaymentMethodLauncher.Result>()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                paymentResultCallbackHandler = paymentResultCallbackHandler,
                googlePayCallbackHandler = googlePayCallbackHandler,
            )
        }

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = GOOGLE_PAY_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = GOOGLE_PAY_OPTION,
                )
            )

            googlePayCallbackHandler.onResult(googlePayResult)

            if (skipPaymentResultCheck) {
                test(awaitItem().asCompleted().result)

                return@test
            }

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    createGooglePaySavedPaymentMethodOption(
                        option = GOOGLE_PAY_OPTION,
                        result = googlePayResult,
                    )
                )
            )

            internalPaymentResult?.let { result ->
                paymentResultCallbackHandler.onResult(result)
            } ?: throw IllegalStateException("Cannot continue test without a payment result!")

            test(awaitItem().asCompleted().result)
        }
    }

    private fun runBacsTest(
        bacsMandateConfirmationResult: BacsMandateConfirmationResult,
        internalPaymentResult: InternalPaymentResult?,
        skipPaymentResultCheck: Boolean,
        test: suspend (result: ConfirmationHandler.Result) -> Unit
    ) = runTest {
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(
                confirmParams = ConfirmPaymentIntentParams.create(
                    clientSecret = "pi_123_secret_123"
                )
            )
        }

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()
        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        val defaultConfirmationHandler = createDefaultConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                paymentResultCallbackHandler = paymentResultCallbackHandler,
                bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
            )
        }

        defaultConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)

            defaultConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = BACS_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                ConfirmationHandler.State.Confirming(
                    option = BACS_OPTION,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(bacsMandateConfirmationResult)

            if (skipPaymentResultCheck) {
                test(awaitItem().asCompleted().result)

                return@test
            }

            assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Confirming(NEW_BACS_PM_OPTION))

            internalPaymentResult?.let { result ->
                paymentResultCallbackHandler.onResult(result)
            } ?: throw IllegalStateException("Cannot continue test without a payment result!")

            test(awaitItem().asCompleted().result)
        }
    }

    private fun createDefaultConfirmationHandler(
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor(),
        bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher(),
        googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory =
            object : GooglePayPaymentMethodLauncherFactory {
                override fun create(
                    lifecycleScope: CoroutineScope,
                    config: GooglePayPaymentMethodLauncher.Config,
                    readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                    activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                    skipReadyCheck: Boolean,
                    cardBrandFilter: CardBrandFilter
                ): GooglePayPaymentMethodLauncher = mock()
            },
        paymentLauncher: PaymentLauncher = FakePaymentLauncher(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        logger: UserFacingLogger = FakeUserFacingLogger(),
        shouldRegister: Boolean = true,
        coroutineScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
    ): DefaultConfirmationHandler {
        return DefaultConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentLauncherFactory = { paymentLauncher },
            bacsMandateConfirmationLauncherFactory = { bacsMandateConfirmationLauncher },
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
            coroutineScope = coroutineScope,
            errorReporter = FakeErrorReporter(),
            savedStateHandle = savedStateHandle,
            logger = logger,
        ).apply {
            if (shouldRegister) {
                registerWithCallbacks()
            }
        }
    }

    private fun ConfirmationHandler.setExternalPaymentMethodLauncher(
        launcher: ActivityResultLauncher<ExternalPaymentMethodInput>
    ) {
        register(
            activityResultCaller = mock {
                on {
                    registerForActivityResult<ExternalPaymentMethodInput, PaymentResult>(
                        any(),
                        any()
                    )
                } doReturn launcher
            },
            lifecycleOwner = TestLifecycleOwner(),
        )
    }

    private fun ConfirmationHandler.registerWithCallbacks(
        paymentResultCallbackHandler: FakeResultHandler<InternalPaymentResult> =
            FakeResultHandler(),
        externalPaymentMethodsCallbackHandler: FakeResultHandler<PaymentResult> =
            FakeResultHandler(),
        bacsMandateConfirmationCallbackHandler: FakeResultHandler<BacsMandateConfirmationResult> =
            FakeResultHandler(),
        googlePayCallbackHandler: FakeResultHandler<GooglePayPaymentMethodLauncher.Result> =
            FakeResultHandler()
    ) {
        register(
            activityResultCaller = mock {
                val captor = argumentCaptor<ActivityResultCallback<Any>>()

                on {
                    registerForActivityResult(
                        any<ActivityResultContract<Any, Any>>(),
                        captor.capture()
                    )
                } doReturn mock()

                paymentResultCallbackHandler.register {
                    captor.firstValue.onActivityResult(it)
                }

                externalPaymentMethodsCallbackHandler.register {
                    captor.secondValue.onActivityResult(it)
                }

                bacsMandateConfirmationCallbackHandler.register {
                    captor.thirdValue.onActivityResult(it)
                }

                googlePayCallbackHandler.register {
                    captor.allValues[3].onActivityResult(it)
                }
            },
            lifecycleOwner = TestLifecycleOwner(),
        )
    }

    private fun createGooglePaySavedPaymentMethodOption(
        option: GooglePayConfirmationOption,
        result: GooglePayPaymentMethodLauncher.Result,
    ): PaymentMethodConfirmationOption.Saved {
        return PaymentMethodConfirmationOption.Saved(
            initializationMode = option.initializationMode,
            shippingDetails = option.shippingDetails,
            paymentMethod = result.asCompleted().paymentMethod,
            optionsParams = null,
        )
    }

    private fun GooglePayPaymentMethodLauncher.Result.asCompleted(): GooglePayPaymentMethodLauncher.Result.Completed {
        return this as GooglePayPaymentMethodLauncher.Result.Completed
    }

    private fun ConfirmationHandler.Result?.asFailed(): ConfirmationHandler.Result.Failed {
        return this as ConfirmationHandler.Result.Failed
    }

    private fun ConfirmationHandler.State.asCompleted(): ConfirmationHandler.State.Complete {
        return this as ConfirmationHandler.State.Complete
    }

    private fun createBacsPaymentConfirmationOption(
        name: String? = "John Doe",
        email: String? = "johndoe@email.com",
    ): BacsConfirmationOption {
        return BacsConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_456_secret_456"
            ),
            shippingDetails = null,
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "108800"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = name,
                    email = email,
                )
            ),
            appearance = APPEARANCE,
            optionsParams = null,
        )
    }

    private companion object {
        val APPEARANCE = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING.appearance
        val DEFAULT_ARGUMENTS = ConfirmationHandler.Args(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_456_secret_456"
                ),
                shippingDetails = null,
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
        )

        val EXTERNAL_PAYMENT_METHOD = ExternalPaymentMethodConfirmationOption(
            type = "paypal",
            billingDetails = null
        )

        val BACS_OPTION = BacsConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_456_secret_456"
            ),
            shippingDetails = null,
            appearance = APPEARANCE,
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "808080",
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    email = "johndoe@email.com"
                ),
            ),
            optionsParams = null,
        )

        val NEW_BACS_PM_OPTION = PaymentMethodConfirmationOption.New(
            initializationMode = BACS_OPTION.initializationMode,
            createParams = BACS_OPTION.createParams,
            shippingDetails = null,
            optionsParams = null,
            shouldSave = false,
        )

        val GOOGLE_PAY_OPTION = GooglePayConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_456_secret_456"
            ),
            shippingDetails = null,
            config = GooglePayConfirmationOption.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                merchantName = "Merchant, Inc.",
                merchantCurrencyCode = "USD",
                merchantCountryCode = "US",
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                customAmount = 5000,
                customLabel = "Merchant Payments",
                cardBrandFilter = DefaultCardBrandFilter
            )
        )

        /**
         * The external payment method confirm handler is not used in [ExternalPaymentMethodInterceptor] which is
         * not tested here but is instead meant to be used in the launched activity the interceptor attempts to launch.
         * Since we only care that [DefaultConfirmationHandler] is actually attempting to launch the EPM handler as well
         * as its interactions, we don't do anything here except for using the handler to validate that we can launch
         * the EPM handler.
         */
        val EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER = ExternalPaymentMethodConfirmHandler { _, _ ->
            // Do nothing
        }
    }
}
