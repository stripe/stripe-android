package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
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
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakePaymentLauncher
import com.stripe.android.utils.FakeExternalPaymentMethodLauncher
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakeResultHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class IntentConfirmationHandlerTest {
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
                initializationMode = initializationMode,
                shippingDetails = shippingDetails,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = savedPaymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                ),
                appearance = APPEARANCE,
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
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse

        val intentConfirmationHandler = createIntentConfirmationHandler(
            intentConfirmationInterceptor = interceptor,
        )

        intentConfirmationHandler.start(
            arguments = IntentConfirmationHandler.Args(
                initializationMode = initializationMode,
                shippingDetails = null,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentSelection = PaymentSelection.New.Card(
                    paymentMethodCreateParams = newCard,
                    brand = CardBrand.Visa,
                    customerRequestedSave = customerRequestedSave,
                ),
                appearance = APPEARANCE,
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
                initializationMode = initializationMode,
                shippingDetails = null,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                appearance = APPEARANCE,
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
            IntentConfirmationHandler.Result.Succeeded(
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
            IntentConfirmationHandler.Result.Failed(
                cause = cause,
                message = message.resolvableString,
                type = IntentConfirmationHandler.ErrorType.Internal,
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
            assertThat(failedResult.type).isEqualTo(IntentConfirmationHandler.ErrorType.Fatal)
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

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
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

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Canceled)

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Canceled(
                action = IntentConfirmationHandler.CancellationAction.InformCancellation,
            )
        )
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

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        val cause = IllegalStateException("This is a failure!")

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Failed(cause))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Failed(
                cause = cause,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = IntentConfirmationHandler.ErrorType.Payment,
            )
        )
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
    fun `On init with 'SavedStateHandle', should receive result through 'awaitIntentResult'`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set("AwaitingPaymentResult", true)
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
            IntentConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
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
            IntentConfirmationHandler.Result.Succeeded(
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

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS,
        )

        paymentResultCallbackHandler.onResult(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
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
                paymentSelection = EXTERNAL_PAYMENT_METHOD.copy(
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
                paymentSelection = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "externalPaymentMethodConfirmHandler is null. Cannot process payment for payment selection: paypal"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(IntentConfirmationHandler.ErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `On external PM with no launcher, should return failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler(
            shouldRegister = false,
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val intentResult = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(intentResult.cause.message).isEqualTo(
            "externalPaymentMethodLauncher is null. Cannot process payment for payment selection: paypal"
        )
        assertThat(intentResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(intentResult.type).isEqualTo(IntentConfirmationHandler.ErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `On external PM succeeded result, should return intent succeeded result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        epmsCallbackHandler.onResult(PaymentResult.Completed)

        val intentResult = intentConfirmationHandler.awaitIntentResult()

        assertThat(intentResult).isEqualTo(
            IntentConfirmationHandler.Result.Succeeded(
                intent = DEFAULT_ARGUMENTS.intent,
                deferredIntentConfirmationType = null,
            )
        )
    }

    @Test
    fun `On external PM failed result, should return intent failed result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        val exception = APIException()

        epmsCallbackHandler.onResult(PaymentResult.Failed(exception))

        val intentResult = intentConfirmationHandler.awaitIntentResult()

        assertThat(intentResult).isEqualTo(
            IntentConfirmationHandler.Result.Failed(
                cause = exception,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = IntentConfirmationHandler.ErrorType.ExternalPaymentMethod,
            )
        )
    }

    @Test
    fun `On external PM canceled result, should return intent canceled result`() = runTest {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER

        val intentConfirmationHandler = createIntentConfirmationHandler()

        val epmsCallbackHandler = FakeResultHandler<PaymentResult>()

        intentConfirmationHandler.registerWithCallbacks(
            externalPaymentMethodsCallbackHandler = epmsCallbackHandler
        )

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = EXTERNAL_PAYMENT_METHOD,
            ),
        )

        epmsCallbackHandler.onResult(PaymentResult.Canceled)

        val intentResult = intentConfirmationHandler.awaitIntentResult()

        assertThat(intentResult).isEqualTo(
            IntentConfirmationHandler.Result.Canceled(
                action = IntentConfirmationHandler.CancellationAction.None,
            )
        )
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

        val appearance = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING.appearance

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = createBacsPaymentSelection(),
                appearance = appearance,
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
                appearance = appearance,
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
                paymentSelection = createBacsPaymentSelection(),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(IntentConfirmationHandler.ErrorType.Internal)
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
                paymentSelection = createBacsPaymentSelection(
                    name = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(IntentConfirmationHandler.ErrorType.Internal)
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
                paymentSelection = createBacsPaymentSelection(
                    email = null,
                ),
            ),
        )

        bacsMandateConfirmationLauncher.calls.expectNoEvents()

        val result = intentConfirmationHandler.awaitIntentResult().asFailed()

        assertThat(result.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(result.type).isEqualTo(IntentConfirmationHandler.ErrorType.Internal)
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

        val paymentSelection = createBacsPaymentSelection()

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = paymentSelection
            ),
        )

        bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Confirmed)

        val call = interceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithNewPaymentMethod(
                initializationMode = DEFAULT_ARGUMENTS.initializationMode,
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
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

        val paymentSelection = createBacsPaymentSelection()

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = paymentSelection
            ),
        )

        bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.ModifyDetails)

        interceptor.calls.expectNoEvents()

        assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(
            IntentConfirmationHandler.Result.Canceled(
                action = IntentConfirmationHandler.CancellationAction.ModifyPaymentDetails,
            )
        )
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

        val paymentSelection = createBacsPaymentSelection()

        intentConfirmationHandler.start(
            arguments = DEFAULT_ARGUMENTS.copy(
                paymentSelection = paymentSelection
            ),
        )

        bacsMandateConfirmationCallbackHandler.onResult(BacsMandateConfirmationResult.Cancelled)

        interceptor.calls.expectNoEvents()

        assertThat(intentConfirmationHandler.awaitIntentResult()).isEqualTo(
            IntentConfirmationHandler.Result.Canceled(
                action = IntentConfirmationHandler.CancellationAction.None,
            )
        )
    }

    private fun createIntentConfirmationHandler(
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor(),
        bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher(),
        paymentLauncher: PaymentLauncher = FakePaymentLauncher(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        shouldRegister: Boolean = true,
    ): IntentConfirmationHandler {
        return IntentConfirmationHandler(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentLauncherFactory = { paymentLauncher },
            bacsMandateConfirmationLauncherFactory = { bacsMandateConfirmationLauncher },
            context = ApplicationProvider.getApplicationContext(),
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            errorReporter = FakeErrorReporter(),
            savedStateHandle = savedStateHandle
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
            },
            lifecycleOwner = TestLifecycleOwner(),
        )
    }

    private fun IntentConfirmationHandler.Result?.asFailed(): IntentConfirmationHandler.Result.Failed {
        return this as IntentConfirmationHandler.Result.Failed
    }

    private fun createBacsPaymentSelection(
        name: String? = "John Doe",
        email: String? = "johndoe@email.com",
    ): PaymentSelection.New.GenericPaymentMethod {
        return PaymentSelection.New.GenericPaymentMethod(
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "108800"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = name,
                    email = email,
                )
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            darkThemeIconUrl = null,
            lightThemeIconUrl = null,
            iconResource = 0,
            label = "Bacs".resolvableString
        )
    }

    private companion object {
        val APPEARANCE = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING.appearance
        val DEFAULT_ARGUMENTS = IntentConfirmationHandler.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456"),
            shippingDetails = null,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            appearance = APPEARANCE
        )

        val EXTERNAL_PAYMENT_METHOD = PaymentSelection.ExternalPaymentMethod(
            type = "paypal",
            label = "Paypal".resolvableString,
            iconResource = 0,
            darkThemeIconUrl = null,
            lightThemeIconUrl = null,
            billingDetails = null
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
