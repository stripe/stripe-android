package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class TapToAddCollectionHandlerTest {
    @Test
    fun `create returns UnsupportedTapToAddCollectionHandler when terminal SDK is not available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { false },
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
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
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
            createCardPresentSetupIntentCallbackRetriever = FakeCreateCardPresentSetupIntentCallbackRetriever.noOp(
                callbackResult = Result.success(DEFAULT_CALLBACK),
            ),
        )

        assertThat(handler).isInstanceOf(DefaultTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `handler returns Collected when already connected`() = runScenario(
        isConnected = true,
    ) {
        val result = handler.collect(DEFAULT_METADATA)

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        assertThat(result).isEqualTo(TapToAddCollectionHandler.CollectionState.Collected)
    }

    @Test
    fun `handler calls connect and returns Collected when not connected`() = runScenario(
        isConnected = false,
        awaitResult = Result.success(true)
    ) {
        val result = handler.collect(DEFAULT_METADATA)

        assertThat(managerScenario.connectCalls.awaitItem()).isNotNull()
        assertThat(managerScenario.awaitCalls.awaitItem()).isNotNull()
        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        assertThat(result).isEqualTo(TapToAddCollectionHandler.CollectionState.Collected)
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
        val error = CreateCardPresentSetupIntentCallbackNotFoundException(
            message = "Callback not found",
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
    fun `handler returns Collected when callback returns Success`() = runScenario(
        isConnected = true,
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = handler.collect(DEFAULT_METADATA)

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        assertThat(result).isEqualTo(TapToAddCollectionHandler.CollectionState.Collected)
    }

    private fun runScenario(
        isConnected: Boolean = true,
        awaitResult: Result<Boolean> = Result.success(true),
        callbackResult: Result<CreateCardPresentSetupIntentCallback> = Result.success(DEFAULT_CALLBACK),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
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
                            connectionManager = managerScenario.tapToAddConnectionManager,
                            createCardPresentSetupIntentCallbackRetriever = retrieverScenario.retriever,
                        ),
                        managerScenario = managerScenario,
                        retrieverScenario = retrieverScenario,
                    )
                )
            }
        }
    }

    private class Scenario(
        val handler: TapToAddCollectionHandler,
        val managerScenario: FakeTapToAddConnectionManager.Scenario,
        val retrieverScenario: FakeCreateCardPresentSetupIntentCallbackRetriever.Scenario,
    )

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
