package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CardDetails
import com.stripe.stripeterminal.external.models.SetupAttempt
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.SetupIntentCardPresentDetails
import com.stripe.stripeterminal.external.models.SetupIntentConfiguration
import com.stripe.stripeterminal.external.models.SetupIntentPaymentMethodDetails
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.fail
import com.stripe.stripeterminal.external.models.PaymentMethod as TerminalPaymentMethod

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class TapToAddCollectionHandlerTest {
    @Test
    fun `create returns UnsupportedTapToAddCollectionHandler when terminal SDK is not available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { false },
            terminalWrapper = TestTerminalWrapper.noOp(),
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
            errorReporter = FakeErrorReporter(),
            createCardPresentSetupIntentCallbackRetriever = FakeCreateCardPresentSetupIntentCallbackRetriever.noOp(
                callbackResult = Result.success(DEFAULT_CALLBACK),
            ),
        )

        assertThat(handler).isInstanceOf(UnsupportedTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `create returns DefaultTapToAddCollectionHandler when terminal SDK is available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { true },
            terminalWrapper = TestTerminalWrapper.noOp(),
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
            errorReporter = FakeErrorReporter(),
            createCardPresentSetupIntentCallbackRetriever = FakeCreateCardPresentSetupIntentCallbackRetriever.noOp(
                callbackResult = Result.success(DEFAULT_CALLBACK),
            ),
        )

        assertThat(handler).isInstanceOf(DefaultTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `handler returns FailedCollection when await fails`() {
        val error = IllegalStateException("Failed")

        runScenario(
            isConnected = false,
            awaitResult = Result.failure(error)
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(managerScenario.connectCalls.awaitItem()).isNotNull()
            assertThat(managerScenario.awaitCalls.awaitItem()).isNotNull()

            assertThat(result).isEqualTo(
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = error,
                    displayMessage = R.string.stripe_something_went_wrong.resolvableString
                )
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when callback retriever throws`() {
        val error = CallbackNotFoundException(
            message = "Callback not found",
            analyticsValue = "notFound",
            resolvableError = "Callback not implemented".resolvableString
        )

        runScenario(
            isConnected = true,
            callbackResult = Result.failure(error),
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

            assertThat(result).isEqualTo(
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = error,
                    displayMessage = error.resolvableError,
                )
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when callback returns Failure`() {
        val cause = IllegalStateException("Failed to create intent")

        runScenario(
            isConnected = true,
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Failure(cause = cause, displayMessage = "Something went wrong")
                }
            ),
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

            assertThat(result).isEqualTo(
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = cause,
                    displayMessage = "Something went wrong".resolvableString,
                )
            )
        }
    }

    @Test
    fun `handler returns Collected when flow is successfully completed with card`() = testSuccessfulFlow(
        useInterac = false,
    )

    @Test
    fun `handler returns Collected when flow is successfully completed with interac card`() = testSuccessfulFlow(
        useInterac = true,
    )

    @Test
    fun `handler returns FailedCollection and reports error when no payment method after confirmation`() =
        runScenario(
            isConnected = true,
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val result = testScope.backgroundScope.async {
                handler.collect(DEFAULT_METADATA)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            checkConfirmCall(useInterac = false, collectedSetupIntent = collectedIntent, paymentMethod = null)

            val collectionResult = result.await()
            assertThat(collectionResult)
                .isInstanceOf(TapToAddCollectionHandler.CollectionState.FailedCollection::class.java)

            val failed = collectionResult as TapToAddCollectionHandler.CollectionState.FailedCollection
            assertThat(failed.error).isInstanceOf(IllegalStateException::class.java)
            assertThat(failed.error.message).isEqualTo("No generated card payment method after collecting through tap!")

            val errorCall = errorReporter.awaitCall()
            assertThat(errorCall.errorEvent).isEqualTo(
                ErrorReporter
                    .UnexpectedErrorEvent
                    .TAP_TO_ADD_NO_GENERATED_CARD_AFTER_SUCCESSFUL_INTENT_CONFIRMATION
            )
        }

    @Test
    fun `handler returns FailedCollection when retrieveSetupIntent fails`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.UNEXPECTED_SDK_ERROR,
            errorMessage = "Failed to retrieve setup intent"
        )

        val retrieveSetupIntentCall = terminalScenario.retrieveSetupIntentCalls.awaitItem()
        retrieveSetupIntentCall.callback.onFailure(terminalException)

        assertThat(result.await()).isEqualTo(
            TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = terminalException,
                displayMessage = R.string.stripe_something_went_wrong.resolvableString
            )
        )
    }

    @Test
    fun `handler returns FailedCollection when collectSetupIntentPaymentMethod fails`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_STRIPE_API,
            errorMessage = "Card declined"
        )

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        collectPaymentMethodCall.callback.onFailure(terminalException)

        assertThat(result.await()).isEqualTo(
            TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = terminalException,
                displayMessage = R.string.stripe_something_went_wrong.resolvableString
            )
        )
    }

    @Test
    fun `handler returns Canceled when collectSetupIntentPaymentMethod fails with CANCELED`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.CANCELED,
            errorMessage = "Customer canceled",
        )

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        collectPaymentMethodCall.callback.onFailure(terminalException)

        assertThat(result.await()).isEqualTo(TapToAddCollectionHandler.CollectionState.Canceled)
    }

    @Test
    fun `handler returns FailedCollection when confirmSetupIntent fails`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_STRIPE_API,
            errorMessage = "Setup intent confirmation failed"
        )

        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedIntent)
        confirmSetupIntentCall.callback.onFailure(terminalException)

        assertThat(result.await()).isEqualTo(
            TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = terminalException,
                displayMessage = R.string.stripe_something_went_wrong.resolvableString
            )
        )
    }

    @Test
    fun `handler cancels collectSetupIntentPaymentMethod when coroutine is cancelled`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val job = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)

        job.cancel()

        verify(collectPaymentMethodCall.cancelable, timeout(1000)).cancel(any())
    }

    @Test
    fun `handler cancels confirmSetupIntent when coroutine is cancelled`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val job = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)

        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedIntent)

        job.cancel()

        verify(confirmSetupIntentCall.cancelable, timeout(1000)).cancel(any())
    }

    private fun testSuccessfulFlow(
        useInterac: Boolean,
    ) = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)
        val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")
        checkConfirmCall(
            useInterac = useInterac,
            collectedSetupIntent = collectedIntent,
            paymentMethod = paymentMethod,
        )

        val collectionResult = result.await()
        assertThat(collectionResult)
            .isInstanceOf(TapToAddCollectionHandler.CollectionState.Collected::class.java)

        val collected = collectionResult as TapToAddCollectionHandler.CollectionState.Collected

        assertThat(collected.paymentMethod).isNotNull()
        assertThat(collected.paymentMethod.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(collected.paymentMethod.id).isEqualTo("pm_4563")
        assertThat(collected.paymentMethod.card?.last4).isEqualTo("7294")
        assertThat(collected.paymentMethod.card?.brand).isEqualTo(CardBrand.MasterCard)
    }

    private fun runScenario(
        isConnected: Boolean = true,
        awaitResult: Result<Boolean> = Result.success(true),
        callbackResult: Result<CreateCardPresentSetupIntentCallback> = Result.success(DEFAULT_CALLBACK),
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        block: suspend Scenario.() -> Unit,
    ) = runTest(coroutineContext) {
        val terminalScenario = createTerminalScenario()
        val terminalWrapper = TestTerminalWrapper.noOp(terminalScenario.terminalInstance)
        val errorReporter = FakeErrorReporter()

        FakeTapToAddConnectionManager.test(
            isSupported = true,
            isConnected = isConnected,
            awaitResult = awaitResult,
        ) {
            val managerScenario = this
            FakeCreateCardPresentSetupIntentCallbackRetriever.test(
                callbackResult = callbackResult,
            ) {
                val retrieverScenario = this
                block(
                    Scenario(
                        handler = DefaultTapToAddCollectionHandler(
                            terminalWrapper = terminalWrapper,
                            connectionManager = managerScenario.tapToAddConnectionManager,
                            errorReporter = errorReporter,
                            createCardPresentSetupIntentCallbackRetriever = retrieverScenario.retriever,
                        ),
                        managerScenario = managerScenario,
                        retrieverScenario = retrieverScenario,
                        terminalScenario = terminalScenario,
                        errorReporter = errorReporter,
                        testScope = this@runTest,
                    )
                )
            }
        }

        terminalScenario.confirmSetupIntentCalls.ensureAllEventsConsumed()
        terminalScenario.retrieveSetupIntentCalls.ensureAllEventsConsumed()
        terminalScenario.collectPaymentMethodCalls.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private fun createTerminalScenario(): TerminalScenario {
        val retrieveSetupIntentCalls = Turbine<TerminalScenario.RetrieveSetupIntentCall>()
        val collectPaymentMethodCalls = Turbine<TerminalScenario.CollectPaymentMethodCall>()
        val confirmSetupIntentCalls = Turbine<TerminalScenario.ConfirmSetupIntentCall>()

        val terminalInstance: Terminal = mock {
            mockRetrieveSetupIntent(retrieveSetupIntentCalls)
            mockCollectPaymentMethod(collectPaymentMethodCalls)
            mockConfirmSetupIntent(confirmSetupIntentCalls)
        }

        return TerminalScenario(
            terminalInstance = terminalInstance,
            retrieveSetupIntentCalls = retrieveSetupIntentCalls,
            collectPaymentMethodCalls = collectPaymentMethodCalls,
            confirmSetupIntentCalls = confirmSetupIntentCalls,
        )
    }

    private suspend fun Scenario.checkRetrieveSetupIntent(clientSecret: String): SetupIntent {
        val retrievedSetupIntent = mock<SetupIntent>()
        val retrieveSetupIntentCall = terminalScenario.retrieveSetupIntentCalls.awaitItem()
        assertThat(retrieveSetupIntentCall.clientSecret).isEqualTo(clientSecret)
        retrieveSetupIntentCall.callback.onSuccess(retrievedSetupIntent)
        return retrievedSetupIntent
    }

    private suspend fun Scenario.checkCollectCall(
        retrievedSetupIntent: SetupIntent,
        expectedAllowRedisplay: AllowRedisplay = AllowRedisplay.UNSPECIFIED,
    ): SetupIntent {
        val collectedIntent = mock<SetupIntent>()
        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        assertThat(collectPaymentMethodCall.allowRedisplay).isEqualTo(expectedAllowRedisplay)
        assertThat(collectPaymentMethodCall.config)
            .isEqualTo(
                SetupIntentConfiguration.Builder()
                    .build()
            )
        collectPaymentMethodCall.callback.onSuccess(collectedIntent)
        return collectedIntent
    }

    private fun createTerminalPaymentMethod(
        id: String = "pm_generated_123",
        last4: String? = "4242",
        brand: String? = "visa",
    ): TerminalPaymentMethod {
        val cardDetails: CardDetails? = if (last4 != null || brand != null) {
            mock {
                on { this.last4 } doReturn last4
                on { this.brand } doReturn brand
            }
        } else {
            null
        }

        return mock {
            on { this.id } doReturn id
            on { this.cardDetails } doReturn cardDetails
        }
    }

    private suspend fun Scenario.checkConfirmCall(
        useInterac: Boolean = false,
        collectedSetupIntent: SetupIntent,
        paymentMethod: TerminalPaymentMethod?,
    ) {
        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedSetupIntent)

        val cardPresentDetails: SetupIntentCardPresentDetails? = paymentMethod?.let { paymentMethod ->
            mock {
                on { this.generatedCardExpanded } doReturn paymentMethod
            }
        }

        val paymentMethodDetails: SetupIntentPaymentMethodDetails? = cardPresentDetails?.let { cardPresentDetails ->
            mock {
                if (useInterac) {
                    on { this.interacPresentDetails } doReturn cardPresentDetails
                } else {
                    on { this.cardPresentDetails } doReturn cardPresentDetails
                }
            }
        }

        val latestAttempt: SetupAttempt? = paymentMethodDetails?.let { paymentMethodDetails ->
            mock {
                on { this.paymentMethodDetails } doReturn paymentMethodDetails
            }
        }

        val confirmedSetupIntent: SetupIntent = mock {
            on { this.latestAttempt } doReturn latestAttempt
        }

        confirmSetupIntentCall.callback.onSuccess(confirmedSetupIntent)
    }

    private fun KStubbing<Terminal>.mockRetrieveSetupIntent(
        retrieveSetupIntentCalls: Turbine<TerminalScenario.RetrieveSetupIntentCall>
    ) {
        on {
            retrieveSetupIntent(
                clientSecret = any<String>(),
                callback = any<SetupIntentCallback>(),
            )
        } doAnswer { invocation ->
            val setupIntentClientSecret = invocation.arguments[0] as? String
                ?: fail("Invalid argument: Not a setup intent!")

            val callback = invocation.arguments[1] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            retrieveSetupIntentCalls.add(
                TerminalScenario.RetrieveSetupIntentCall(
                    clientSecret = setupIntentClientSecret,
                    callback = callback,
                )
            )
        }
    }

    private fun KStubbing<Terminal>.mockCollectPaymentMethod(
        collectPaymentMethodCalls: Turbine<TerminalScenario.CollectPaymentMethodCall>
    ) {
        on {
            collectSetupIntentPaymentMethod(
                intent = any<SetupIntent>(),
                allowRedisplay = any<AllowRedisplay>(),
                config = any<SetupIntentConfiguration>(),
                callback = any<SetupIntentCallback>()
            )
        } doAnswer { invocation ->
            val setupIntent = invocation.arguments[0] as? SetupIntent
                ?: fail("Invalid argument: Not a setup intent!")

            val allowRedisplay = invocation.arguments[1] as? AllowRedisplay
                ?: fail("Invalid argument: Not an allow redisplay value!")

            val config = invocation.arguments[2] as? SetupIntentConfiguration
                ?: fail("Invalid argument: Not a setup intent config!")

            val callback = invocation.arguments[3] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            val cancelable = mock<Cancelable>()

            collectPaymentMethodCalls.add(
                TerminalScenario.CollectPaymentMethodCall(
                    intent = setupIntent,
                    allowRedisplay = allowRedisplay,
                    config = config,
                    callback = callback,
                    cancelable = cancelable,
                )
            )

            cancelable
        }
    }

    private fun KStubbing<Terminal>.mockConfirmSetupIntent(
        confirmSetupIntentCalls: Turbine<TerminalScenario.ConfirmSetupIntentCall>
    ) {
        on {
            confirmSetupIntent(
                intent = any<SetupIntent>(),
                callback = any<SetupIntentCallback>()
            )
        } doAnswer { invocation ->
            val setupIntent = invocation.arguments[0] as? SetupIntent
                ?: fail("Invalid argument: Not a setup intent!")

            val callback = invocation.arguments[1] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            val cancelable = mock<Cancelable>()

            confirmSetupIntentCalls.add(
                TerminalScenario.ConfirmSetupIntentCall(
                    intent = setupIntent,
                    callback = callback,
                    cancelable = cancelable,
                )
            )

            cancelable
        }
    }

    private class Scenario(
        val testScope: TestScope,
        val handler: TapToAddCollectionHandler,
        val managerScenario: FakeTapToAddConnectionManager.Scenario,
        val retrieverScenario: FakeCreateCardPresentSetupIntentCallbackRetriever.Scenario,
        val terminalScenario: TerminalScenario,
        val errorReporter: FakeErrorReporter,
    )

    private class TerminalScenario(
        val terminalInstance: Terminal,
        val retrieveSetupIntentCalls: ReceiveTurbine<RetrieveSetupIntentCall>,
        val collectPaymentMethodCalls: ReceiveTurbine<CollectPaymentMethodCall>,
        val confirmSetupIntentCalls: ReceiveTurbine<ConfirmSetupIntentCall>,
    ) {
        class RetrieveSetupIntentCall(
            val clientSecret: String,
            val callback: SetupIntentCallback,
        )

        class CollectPaymentMethodCall(
            val intent: SetupIntent,
            val allowRedisplay: AllowRedisplay,
            val config: SetupIntentConfiguration,
            val callback: SetupIntentCallback,
            val cancelable: Cancelable,
        )

        class ConfirmSetupIntentCall(
            val intent: SetupIntent,
            val callback: SetupIntentCallback,
            val cancelable: Cancelable,
        )
    }

    private class FakeCreateCardPresentSetupIntentCallbackRetriever private constructor(
        private val callbackResult: Result<CreateCardPresentSetupIntentCallback>,
    ) : CreateCardPresentSetupIntentCallbackRetriever {
        private val waitForCallbackCalls = Turbine<Unit>()

        override suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback {
            waitForCallbackCalls.add(Unit)
            return callbackResult.getOrThrow()
        }

        class Scenario(
            val retriever: CreateCardPresentSetupIntentCallbackRetriever,
            val waitForCallbackCalls: ReceiveTurbine<Unit>,
        )

        companion object {
            suspend fun test(
                callbackResult: Result<CreateCardPresentSetupIntentCallback>,
                block: suspend Scenario.() -> Unit,
            ) {
                val retriever = FakeCreateCardPresentSetupIntentCallbackRetriever(callbackResult)

                block(
                    Scenario(
                        retriever = retriever,
                        waitForCallbackCalls = retriever.waitForCallbackCalls,
                    )
                )

                retriever.waitForCallbackCalls.ensureAllEventsConsumed()
            }

            fun noOp(
                callbackResult: Result<CreateCardPresentSetupIntentCallback>,
            ) = FakeCreateCardPresentSetupIntentCallbackRetriever(callbackResult)
        }
    }

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
        val DEFAULT_CALLBACK = CreateCardPresentSetupIntentCallback {
            CreateIntentResult.Success("si_123_secret")
        }
    }
}
