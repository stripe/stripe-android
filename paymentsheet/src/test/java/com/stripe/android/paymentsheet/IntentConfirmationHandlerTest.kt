package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCallback
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakePaymentLauncher
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
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
            arguments = IntentConfirmationHandler.Args(
                shippingDetails = shippingDetails,
            )
        )

        intentConfirmationHandler.start(
            initializationMode = initializationMode,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = savedPaymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
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
            arguments = IntentConfirmationHandler.Args(
                shippingDetails = null
            )
        )

        intentConfirmationHandler.start(
            initializationMode = initializationMode,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.New.Card(
                paymentMethodCreateParams = newCard,
                brand = CardBrand.Visa,
                customerRequestedSave = customerRequestedSave,
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
            arguments = IntentConfirmationHandler.Args(
                shippingDetails = null
            )
        )

        intentConfirmationHandler.start(
            initializationMode = initializationMode,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        interceptor.calls.skipItems(1)

        intentConfirmationHandler.start(

            initializationMode = INITIALIZATION_MODE.copy(clientSecret = "pi_456_secret_756"),
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            initializationMode = INITIALIZATION_MODE,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Failed(
                cause = cause,
                message = message.resolvableString,
                type = IntentConfirmationHandler.ErrorType.NextStep,
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
                initializationMode = INITIALIZATION_MODE,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        intentConfirmationHandler.start(
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        callback(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        intentConfirmationHandler.start(
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        callback(InternalPaymentResult.Canceled)

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(IntentConfirmationHandler.Result.Canceled)
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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        intentConfirmationHandler.start(
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        val cause = IllegalStateException("This is a failure!")

        callback(InternalPaymentResult.Failed(cause))

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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        callback(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        intentConfirmationHandler.start(
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        callback(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

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

        val callback = intentConfirmationHandler.registerAndRetrievePaymentResultCallback()

        intentConfirmationHandler.start(
            initializationMode = INITIALIZATION_MODE,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        callback(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

        val result = intentConfirmationHandler.awaitIntentResult()

        assertThat(result).isEqualTo(
            IntentConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
    }

    private fun createIntentConfirmationHandler(
        arguments: IntentConfirmationHandler.Args = IntentConfirmationHandler.Args(shippingDetails = null),
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor(),
        paymentLauncher: PaymentLauncher = FakePaymentLauncher(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        shouldRegister: Boolean = true
    ): IntentConfirmationHandler {
        return IntentConfirmationHandler(
            arguments = arguments,
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentLauncherFactory = { paymentLauncher },
            context = ApplicationProvider.getApplicationContext(),
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            savedStateHandle = savedStateHandle
        ).apply {
            if (shouldRegister) {
                register(
                    activityResultCaller = mock {
                        on {
                            registerForActivityResult<PaymentLauncherContract.Args, InternalPaymentResult>(
                                any(),
                                any()
                            )
                        } doReturn mock()
                    },
                    lifecycleOwner = TestLifecycleOwner(),
                )
            }
        }
    }

    private fun IntentConfirmationHandler.registerAndRetrievePaymentResultCallback():
        (result: InternalPaymentResult) -> Unit {
        val argumentCaptor = argumentCaptor<ActivityResultCallback<InternalPaymentResult>>()

        register(
            activityResultCaller = mock {
                on {
                    registerForActivityResult<PaymentLauncherContract.Args, InternalPaymentResult>(
                        any(),
                        argumentCaptor.capture()
                    )
                } doReturn mock()
            },
            lifecycleOwner = TestLifecycleOwner(),
        )

        return {
            argumentCaptor.firstValue.onActivityResult(it)
        }
    }

    private fun IntentConfirmationHandler.Result?.asFailed(): IntentConfirmationHandler.Result.Failed {
        return this as IntentConfirmationHandler.Result.Failed
    }

    private companion object {
        val INITIALIZATION_MODE = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = "pi_456_secret_456")
    }
}
