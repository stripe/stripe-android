@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentelement.confirmation.intent

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.FakeCheckoutSessionRefresher
import com.stripe.android.core.Logger
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakePaymentLauncher
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.asCallbackFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class CheckoutSessionConfirmationFlowTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `authentication results retain the response and hold the operation gate through polling`(
        @TestParameter authentication: Authentication,
    ) = runScenario {
        DummyActivityResultCaller.test {
            handler.register(activityResultCaller, fakeLifecycleOwner())
            val callback = awaitRegisterCall().callback.asCallbackFor<InternalPaymentResult>()
            awaitNextRegisteredLauncher()
            handler.start(requireNotNull(coordinator.tryBeginConfirmation { arguments }))
            factoryCalls.awaitItem()
            assertThat(interceptor.calls.awaitItem()).isEqualTo(option)
            assertThat(paymentLauncher.calls.awaitItem())
                .isEqualTo(FakePaymentLauncher.Call.HandleNextActionWithIntent.Intent(intent))

            val completedIntent = PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            val authenticationError = IllegalStateException("Authentication failed")
            refresher.enqueueRefreshAction {}
            callback.onActivityResult(
                when (authentication) {
                    Authentication.COMPLETED -> InternalPaymentResult.Completed(completedIntent)
                    Authentication.CANCELED -> InternalPaymentResult.Canceled
                    Authentication.FAILED -> InternalPaymentResult.Failed(authenticationError)
                }
            )
            if (authentication == Authentication.COMPLETED) {
                finishPolling(completedIntent)
            } else {
                assertThat(refresher.calls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
                val merchantResult = merchantResults.awaitItem()
                if (authentication == Authentication.CANCELED) {
                    assertThat(merchantResult).isInstanceOf(CheckoutController.Result.Canceled::class.java)
                } else {
                    assertThat((merchantResult as CheckoutController.Result.Failed).error)
                        .isSameInstanceAs(authenticationError)
                }
            }
        }
    }

    private suspend fun Scenario.finishPolling(completedIntent: StripeIntent) {
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(coordinator.isUpdating.value).isTrue()
        merchantResults.expectNoEvents()
        val mutationStarted = Turbine<Unit>()
        val mutation = scope.backgroundScope.async {
            coordinator.runMutation {
                mutationStarted.add(Unit)
                Result.success(Unit)
            }
        }
        scope.testScheduler.runCurrent()
        mutationStarted.expectNoEvents()
        pollRelease.complete(Unit)
        assertThat(savedIntents.awaitItem()).isEqualTo(completedIntent)
        val committed = refresher.calls.awaitItem() as FakeCheckoutSessionRefresher.Call.Commit
        assertThat(committed.response.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(committed.response.paymentIntent).isEqualTo(completedIntent)
        assertThat(merchantResults.awaitItem()).isInstanceOf(CheckoutController.Result.Completed::class.java)
        assertThat(mutation.await().isSuccess).isTrue()
        mutationStarted.awaitItem()
        mutationStarted.ensureAllEventsConsumed()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario(this)
        scenario.block()
        scenario.ensureAllEventsConsumed()
    }

    private class Scenario(val scope: kotlinx.coroutines.test.TestScope) {
        val intent = PaymentIntentFactory.create(status = StripeIntent.Status.RequiresAction)
        val response = CheckoutSessionResponseFactory.create(paymentIntent = intent)
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )
        val arguments = ConfirmationHandler.Args(
            confirmationOption = option,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(response.id, "test", response),
            ),
            statusBarColor = null,
        )
        val interceptor = FakeCheckoutInterceptor(response, intent)
        val factoryCalls = Turbine<Unit>()
        val paymentLauncher = FakePaymentLauncher()
        val pollCalls = Turbine<String>()
        val pollRelease = CompletableDeferred<Unit>()
        val savedIntents = Turbine<StripeIntent>()
        val definition = IntentConfirmationDefinition(
            intentConfirmationInterceptorFactory = object : IntentConfirmationInterceptor.Factory {
                override suspend fun create(
                    integrationMetadata: IntegrationMetadata,
                    customerMetadata: CustomerMetadata?,
                    clientAttributionMetadata: ClientAttributionMetadata,
                    isLiveMode: Boolean,
                ): IntentConfirmationInterceptor {
                    factoryCalls.add(Unit)
                    return interceptor
                }
            },
            paymentLauncherFactory = { _, _, _ -> paymentLauncher },
            checkoutSessionFinalizer = CheckoutSessionConfirmationFinalizer(
                poller = CheckoutSessionPoller {
                    pollCalls.add(it)
                    pollRelease.await()
                    CheckoutSessionPoller.Outcome.COMPLETED
                },
                retrieveSession = { error("Unexpected retrieval") },
                genericErrorMessage = "Generic payment error",
            ),
        )
        val handler = DefaultConfirmationHandler(
            mediators = listOf(ConfirmationMediator(SavedStateHandle(), definition)),
            coroutineScope = scope.backgroundScope,
            savedStateHandle = SavedStateHandle(),
            errorReporter = FakeErrorReporter(),
            ioContext = UnconfinedTestDispatcher(scope.testScheduler),
            logger = Logger.noop(),
            confirmationSaver = { savedIntent, _, _ -> savedIntents.add(savedIntent) },
        )
        val refresher = FakeCheckoutSessionRefresher()
        val merchantResults = Turbine<CheckoutController.Result>()
        val coordinator = CheckoutOperationCoordinator(
            confirmationHandler = handler,
            sheetStateHolder = SheetStateHolder(SavedStateHandle()),
            sessionRefresher = refresher,
            logger = Logger.noop(),
            resultCallback = { merchantResults.add(it) },
            viewModelScope = scope.backgroundScope,
        )

        init {
            scope.backgroundScope.launch { coordinator.observeConfirmationResults() }
        }

        fun ensureAllEventsConsumed() {
            factoryCalls.ensureAllEventsConsumed()
            interceptor.calls.ensureAllEventsConsumed()
            paymentLauncher.calls.ensureAllEventsConsumed()
            pollCalls.ensureAllEventsConsumed()
            savedIntents.ensureAllEventsConsumed()
            merchantResults.ensureAllEventsConsumed()
            refresher.ensureAllEventsConsumed()
        }
    }

    private class FakeCheckoutInterceptor(
        private val response: CheckoutSessionResponse,
        private val intent: StripeIntent,
    ) : IntentConfirmationInterceptor {
        val calls = Turbine<PaymentMethodConfirmationOption.New>()

        override suspend fun intercept(
            intent: StripeIntent,
            confirmationOption: PaymentMethodConfirmationOption.New,
            shippingValues: ConfirmPaymentIntentParams.Shipping?,
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
            calls.add(confirmationOption)
            return ConfirmationDefinition.Action.Launch(
                launcherArguments = IntentConfirmationDefinition.Args.CheckoutNextAction(this.intent, response),
                receivesResultInProcess = false,
            )
        }

        override suspend fun intercept(
            intent: StripeIntent,
            confirmationOption: PaymentMethodConfirmationOption.Saved,
            shippingValues: ConfirmPaymentIntentParams.Shipping?,
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = error("Unexpected saved payment method")
    }

    enum class Authentication { COMPLETED, CANCELED, FAILED }
}
