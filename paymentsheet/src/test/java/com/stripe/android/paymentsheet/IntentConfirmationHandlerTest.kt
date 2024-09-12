package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
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
class IntentConfirmationHandlerTest {
    @Test
    fun `On 'init', state should be idle`() = runTest {
        val intentConfirmationHandler = createIntentConfirmationHandler()

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On 'start' with existing payment method, should call interceptor properly`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "ci_123")
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        intentConfirmationHandler.start(
            arguments = IntentConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentConfirmationOption.PaymentMethod.Saved(
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
        val intentConfirmationHandler = createIntentConfirmationHandler()

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = IntentConfirmationHandler.Args(
                    intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    confirmationOption = PaymentConfirmationOption.PaymentMethod.Saved(
                        initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                            clientSecret = "pi_456_secret_456"
                        ),
                        shippingDetails = null,
                        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        optionsParams = null,
                    ),
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On 'start' with new payment method, should call interceptor properly`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "ci_123")
        val newCard = PaymentMethodCreateParams.createCard(
            CardParams(
                number = "4242424242424242",
                expMonth = 5,
                expYear = 2035,
            )
        )

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        intentConfirmationHandler.start(
            arguments = IntentConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentConfirmationOption.PaymentMethod.New(
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

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        intentConfirmationHandler.start(
            arguments = IntentConfirmationHandler.Args(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = PaymentConfirmationOption.PaymentMethod.Saved(
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
        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        interceptor.calls.skipItems(1)

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        interceptor.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `On intercepted intent complete, should receive 'Succeeded' result through 'awaitIntentResult'`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        interceptor.enqueueCompleteStep()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Succeeded(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Failed(
                cause = cause,
                message = message.resolvableString,
                type = PaymentConfirmationErrorType.Payment,
            )
        )
    }

    @Test
    fun `On confirmation attempt without registering callbacks, should return 'Failed' result`() =
        runTest {
            val interceptor = FakeIntentConfirmationInterceptor()
            val paymentLauncher = FakePaymentLauncher()

            interceptor.enqueueConfirmStep(ConfirmPaymentIntentParams.create(clientSecret = "pi_1234"))

            val intentConfirmationHandler = createIntentConfirmationHandler(
                intentConfirmationInterceptor = interceptor,
                paymentLauncher = paymentLauncher,
                shouldRegister = false,
            )

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            val result = intentConfirmationHandler.awaitIntentResult()

            val failedResult = result.asFailed()

            val message = "No 'PaymentLauncher' instance was created before starting confirmation. " +
                "Did you call register?"

            assertThat(failedResult.cause).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(failedResult.cause.message).isEqualTo(message)
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(PaymentConfirmationErrorType.Fatal)
        }

    @Test
    fun `On 'PaymentIntent' requires confirmation, should call 'PaymentLauncher' to handle confirmation`() = runTest {
        val confirmParams = ConfirmPaymentIntentParams.create(clientSecret = "pi_1234")
        val paymentLauncher = FakePaymentLauncher()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(confirmParams)
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            paymentLauncher = paymentLauncher,
        )

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

            val expectedResult = PaymentConfirmationResult.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment launcher result canceled, should be 'Canceled' result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Canceled)

            val expectedResult = PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.InformCancellation,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment launcher result failed, should be 'Failed' result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            val cause = IllegalStateException("This is a failure!")

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Failed(cause))

            val expectedResult = PaymentConfirmationResult.Failed(
                cause = cause,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = PaymentConfirmationErrorType.Payment,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On payment confirm, should store 'isAwaitingPaymentResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(ConfirmPaymentIntentParams.create("pi_123"))
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        assertThat(savedStateHandle.get<Boolean>("AwaitingPaymentResult")).isTrue()
    }

    @Test
    fun `On payment handle next action, should store 'isAwaitingPaymentResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep("pi_123")
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        assertThat(savedStateHandle.get<Boolean>("AwaitingPaymentResult")).isTrue()
    }

    @Test
    fun `On launch EPMs handler, should store 'AwaitingPaymentResult' in 'SavedStateHandle'`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val savedStateHandle = SavedStateHandle()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep("pi_123")
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            savedStateHandle = savedStateHandle,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD
            ),
        )

        assertThat(savedStateHandle.get<Boolean>("AwaitingPaymentResult")).isTrue()
    }

    @Test
    fun `On start Bacs mandate, should store 'AwaitingPreConfirmResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,

        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(),
            ),
        )

        assertThat(savedStateHandle.get<Boolean>("AwaitingPreConfirmResult")).isTrue()
    }

    @Test
    fun `On start Google Pay, should store 'AwaitingPreConfirmResult' in 'SavedStateHandle'`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = GOOGLE_PAY_OPTION,
            ),
        )

        assertThat(savedStateHandle.get<Boolean>("AwaitingPreConfirmResult")).isTrue()
    }

    @Test
    fun `On init with 'SavedStateHandle' awaiting payment result, should timeout & cancel after 1 second`() = runTest {
        val dispatcher = StandardTestDispatcher()

        val savedStateHandle = SavedStateHandle().apply {
            set("AwaitingPaymentResult", true)
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,
            coroutineScope = CoroutineScope(dispatcher)
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            dispatcher.scheduler.advanceTimeBy(delayTime = 1.01.seconds)

            val expectedResult = PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.None
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On init with 'SavedStateHandle' awaiting pre confirm result, continue to wait until result`() = runTest {
        val dispatcher = StandardTestDispatcher()
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueCompleteStep()
        }

        val confirmationOption = createBacsPaymentConfirmationOption()
        val bacsArguments = DEFAULT_ARGUMENTS.copy(
            confirmationOption = confirmationOption
        )

        val savedStateHandle = SavedStateHandle().apply {
            set("AwaitingPreConfirmResult", true)
            set("IntentConfirmationArguments", bacsArguments)
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,
            intentConfirmationInterceptor = interceptor,
            coroutineScope = CoroutineScope(dispatcher)
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        intentConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = true,
                )
            )

            dispatcher.scheduler.advanceTimeBy(delayTime = 200.seconds)

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Confirmed)

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            val expectedResult = PaymentConfirmationResult.Succeeded(
                intent = DEFAULT_ARGUMENTS.intent,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On init with 'SavedStateHandle' with incorrect option, error should be internal`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set("AwaitingPaymentResult", true)
            set("IntentConfirmationArguments", DEFAULT_ARGUMENTS.copy(confirmationOption = EXTERNAL_PAYMENT_METHOD))
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val failedResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(failedResult.type).isEqualTo(PaymentConfirmationErrorType.Internal)
    }

    @Test
    fun `On init with 'SavedStateHandle', should receive result through 'awaitIntentResult'`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set("AwaitingPaymentResult", true)
            set("IntentConfirmationArguments", DEFAULT_ARGUMENTS)
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            savedStateHandle = savedStateHandle,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Succeeded(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val failedResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(failedResult.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
        assertThat(failedResult.type).isEqualTo(PaymentConfirmationErrorType.Payment)
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Succeeded(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val paymentResultCallbackHandler = FakeResultHandler<InternalPaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            paymentResultCallbackHandler = paymentResultCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS,
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = DEFAULT_ARGUMENTS.confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

            val expectedResult = PaymentConfirmationResult.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM, should launch external PM handler with expected params`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val fakeExternalPaymentMethodLauncher = FakeExternalPaymentMethodLauncher()

        val intentConfirmationHandler = createIntentConfirmationHandler().apply {
            setExternalPaymentMethodLauncher(fakeExternalPaymentMethodLauncher)
        }

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler()

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "externalPaymentMethodConfirmHandler is null. Cannot process payment for payment selection: paypal"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(PaymentConfirmationErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `On external PM with no launcher, should return failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler(
            shouldRegister = false,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "externalPaymentMethodLauncher is null. Cannot process payment for payment selection: paypal"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(PaymentConfirmationErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `On external PM succeeded result, should return intent succeeded result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            epmsCallbackHandler.onResult(PaymentResult.Completed)

            val expectedResult = PaymentConfirmationResult.Succeeded(
                intent = DEFAULT_ARGUMENTS.intent,
                deferredIntentConfirmationType = null,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM failed result, should return intent failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            val exception = APIException()

            epmsCallbackHandler.onResult(PaymentResult.Failed(exception))

            val expectedResult = PaymentConfirmationResult.Failed(
                cause = exception,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = PaymentConfirmationErrorType.ExternalPaymentMethod,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On external PM canceled result, should return intent canceled result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = EXTERNAL_PAYMENT_METHOD,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            epmsCallbackHandler.onResult(PaymentResult.Canceled)

            val expectedResult = PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.None,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On bacs payment method, should launch mandate screen`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()
        val interceptor = FakeIntentConfirmationInterceptor()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        ).apply {
            registerWithCallbacks()
        }

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
            shouldRegister = false
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(PaymentConfirmationErrorType.Internal)
        assertThat(result.cause.message).isEqualTo("Required value was null.")
    }

    @Test
    fun `On missing name for Bacs, should fail with internal error`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(
                    name = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(PaymentConfirmationErrorType.Internal)
        assertThat(result.cause.message).isEqualTo(
            "Given payment selection could not be converted to Bacs data!"
        )
    }

    @Test
    fun `On missing email for Bacs, should fail with internal error`() = runTest {
        val bacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncher,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = createBacsPaymentConfirmationOption(
                    email = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(PaymentConfirmationErrorType.Internal)
        assertThat(result.cause.message).isEqualTo(
            "Given payment selection could not be converted to Bacs data!"
        )
    }

    @Test
    fun `On Bacs mandate confirmed, should continue confirmation process`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        intentConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        intentConfirmationHandler.start(
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        intentConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = confirmationOption
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = true,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.ModifyDetails)

            interceptor.calls.expectNoEvents()

            val expectedResult = PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.ModifyPaymentDetails,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On cancel Bacs data event, should return canceled result`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor()

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
            shouldRegister = false,
        )

        val bacsMandateConfirmationCallbackHandler = FakeResultHandler<BacsMandateConfirmationResult>()

        intentConfirmationHandler.registerWithCallbacks(
            bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
        )

        val confirmationOption = createBacsPaymentConfirmationOption()

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = confirmationOption,
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = confirmationOption,
                    inPreconfirmFlow = true,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Cancelled)

            interceptor.calls.expectNoEvents()

            val expectedResult = PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.None,
            )

            assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(expectedResult)
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Complete(expectedResult))

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `On start Google Pay with no currency and setup intent, should fail and log`() = runTest {
        val logger = FakeUserFacingLogger()
        val intentConfirmationHandler = createIntentConfirmationHandler(
            logger = logger
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                confirmationOption = GOOGLE_PAY_OPTION.copy(
                    initializationMode = PaymentSheet.InitializationMode.SetupIntent(
                        clientSecret = "si_123_secret_123",
                    ),
                    config = GOOGLE_PAY_OPTION.config.copy(
                        merchantCurrencyCode = null,
                    )
                ),
            ),
        )

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        val message = "GooglePayConfig.currencyCode is required in order to use " +
            "Google Pay when processing a Setup Intent"

        assertThat(result.cause.message).isEqualTo(message)
        assertThat(result.type).isEqualTo(PaymentConfirmationErrorType.MerchantIntegration)
        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)

        assertThat(logger.getLoggedMessages()).contains(message)
    }

    @Test
    fun `On start Google Pay with PI, should create and launch Google Pay launcher properly`() = runTest {
        val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
        val googlePayPaymentMethodLauncherFactory = mock<GooglePayPaymentMethodLauncherFactory> {
            on { create(any(), any(), any(), any(), any()) } doReturn googlePayPaymentMethodLauncher
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory
        )

        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        intentConfirmationHandler.start(
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
            skipReadyCheck = eq(true)
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
            on { create(any(), any(), any(), any(), any()) } doReturn googlePayPaymentMethodLauncher
        }

        val intentConfirmationHandler = createIntentConfirmationHandler(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory
        )

        val setupIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD

        intentConfirmationHandler.start(
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
            skipReadyCheck = eq(true)
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
        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                googlePayCallbackHandler = googlePayCallbackHandler,
            )
        }

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = GOOGLE_PAY_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = GOOGLE_PAY_OPTION,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = GOOGLE_PAY_OPTION,
                    inPreconfirmFlow = true,
                )
            )

            googlePayCallbackHandler.onResult(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                )
            )

            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            val call = intentConfirmationInterceptor.calls.awaitItem()

            assertThat(call).isEqualTo(
                FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
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
            PaymentConfirmationResult.Succeeded(
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
            .isEqualTo(PaymentConfirmationErrorType.GooglePay(GooglePayPaymentMethodLauncher.NETWORK_ERROR))
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
            .isEqualTo(PaymentConfirmationErrorType.GooglePay(GooglePayPaymentMethodLauncher.INTERNAL_ERROR))
    }

    @Test
    fun `On Google Pay cancel, should return expected cancel result`() = runGooglePayTest(
        googlePayResult = GooglePayPaymentMethodLauncher.Result.Canceled,
        skipPaymentResultCheck = true,
        internalPaymentResult = null,
    ) { result ->
        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.InformCancellation
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
            .isEqualTo(PaymentConfirmationErrorType.Payment)
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
            PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.InformCancellation
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
            PaymentConfirmationResult.Succeeded(
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
            PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.ModifyPaymentDetails,
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
            PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.None,
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
            .isEqualTo(PaymentConfirmationErrorType.Payment)
    }

    private fun runGooglePayTest(
        googlePayResult: GooglePayPaymentMethodLauncher.Result,
        internalPaymentResult: InternalPaymentResult?,
        skipPaymentResultCheck: Boolean,
        test: suspend (result: PaymentConfirmationResult) -> Unit
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                paymentResultCallbackHandler = paymentResultCallbackHandler,
                googlePayCallbackHandler = googlePayCallbackHandler,
            )
        }

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = GOOGLE_PAY_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = GOOGLE_PAY_OPTION,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = GOOGLE_PAY_OPTION,
                    inPreconfirmFlow = true,
                )
            )

            googlePayCallbackHandler.onResult(googlePayResult)

            if (skipPaymentResultCheck) {
                test(awaitItem().asCompleted().result)

                return@test
            }

            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

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
        test: suspend (result: PaymentConfirmationResult) -> Unit
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

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        ).apply {
            registerWithCallbacks(
                paymentResultCallbackHandler = paymentResultCallbackHandler,
                bacsMandateConfirmationCallbackHandler = bacsMandateConfirmationCallbackHandler,
            )
        }

        intentConfirmationHandler.state.test {
            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Idle)

            intentConfirmationHandler.start(
                arguments = DEFAULT_ARGUMENTS.copy(
                    confirmationOption = BACS_OPTION
                ),
            )

            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = BACS_OPTION,
                    inPreconfirmFlow = false,
                )
            )
            assertThat(awaitItem()).isEqualTo(
                IntentConfirmationHandler.State.Preconfirming(
                    confirmationOption = BACS_OPTION,
                    inPreconfirmFlow = true,
                )
            )

            bacsMandateConfirmationCallbackHandler.onResult(bacsMandateConfirmationResult)

            if (skipPaymentResultCheck) {
                test(awaitItem().asCompleted().result)

                return@test
            }

            assertThat(awaitItem()).isEqualTo(IntentConfirmationHandler.State.Confirming)

            internalPaymentResult?.let { result ->
                paymentResultCallbackHandler.onResult(result)
            } ?: throw IllegalStateException("Cannot continue test without a payment result!")

            test(awaitItem().asCompleted().result)
        }
    }

    private fun createIntentConfirmationHandler(
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor(),
        bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher(),
        googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory =
            object : GooglePayPaymentMethodLauncherFactory {
                override fun create(
                    lifecycleScope: CoroutineScope,
                    config: GooglePayPaymentMethodLauncher.Config,
                    readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                    activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                    skipReadyCheck: Boolean
                ): GooglePayPaymentMethodLauncher = mock()
            },
        paymentLauncher: PaymentLauncher = FakePaymentLauncher(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        logger: UserFacingLogger = FakeUserFacingLogger(),
        shouldRegister: Boolean = true,
        coroutineScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
    ): IntentConfirmationHandler {
        return IntentConfirmationHandler(
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

    private fun IntentConfirmationHandler.setExternalPaymentMethodLauncher(
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

    private fun IntentConfirmationHandler.registerWithCallbacks(
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

    private fun PaymentConfirmationResult?.asFailed(): PaymentConfirmationResult.Failed {
        return this as PaymentConfirmationResult.Failed
    }

    private fun IntentConfirmationHandler.State.asCompleted(): IntentConfirmationHandler.State.Complete {
        return this as IntentConfirmationHandler.State.Complete
    }

    private fun createBacsPaymentConfirmationOption(
        name: String? = "John Doe",
        email: String? = "johndoe@email.com",
    ): PaymentConfirmationOption.BacsPaymentMethod {
        return PaymentConfirmationOption.BacsPaymentMethod(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456"),
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
        val DEFAULT_ARGUMENTS = IntentConfirmationHandler.Args(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = PaymentConfirmationOption.PaymentMethod.Saved(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456"),
                shippingDetails = null,
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
        )

        val EXTERNAL_PAYMENT_METHOD = PaymentConfirmationOption.ExternalPaymentMethod(
            type = "paypal",
            billingDetails = null
        )

        val BACS_OPTION = PaymentConfirmationOption.BacsPaymentMethod(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456"),
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

        val GOOGLE_PAY_OPTION = PaymentConfirmationOption.GooglePay(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456"),
            shippingDetails = null,
            config = PaymentConfirmationOption.GooglePay.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                merchantName = "Merchant, Inc.",
                merchantCurrencyCode = "USD",
                merchantCountryCode = "US",
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                customAmount = 5000,
                customLabel = "Merchant Payments"
            )
        )

        /**
         * The external payment method confirm handler is not used in [ExternalPaymentMethodInterceptor] which is
         * not tested here but is instead meant to be used in the launched activity the interceptor attempts to launch.
         * Since we only care that [IntentConfirmationHandler] is actually attempting to launch the EPM handler as well
         * as its interactions, we don't do anything here except for using the handler to validate that we can launch
         * the EPM handler.
         */
        val EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER = ExternalPaymentMethodConfirmHandler { _, _ ->
            // Do nothing
        }
    }
}
