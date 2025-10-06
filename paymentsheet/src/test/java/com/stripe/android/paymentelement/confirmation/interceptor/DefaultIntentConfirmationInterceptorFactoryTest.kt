package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentCallbackNotFoundException
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentFirstConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.SharedPaymentTokenConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.interceptor.DeferredIntentConfirmationInterceptorTest.Companion.DEFAULT_DEFERRED_INTENT
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DefaultIntentConfirmationInterceptorFactoryTest {

    @Test
    fun `create() with sharedPaymentToken returns SharedPaymentTokenConfirmationInterceptor`() =
        runScenario(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                    sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                        businessName = "My business, Inc.",
                        networkId = "network_id",
                        externalId = "external_id"
                    )
                ),
            ),
            preparePaymentMethodHandlerProvider = Provider { PreparePaymentMethodHandler { _, _ -> } },
        ) {
            assertThat(interceptor).isInstanceOf(SharedPaymentTokenConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with sharedPaymentToken fails if PreparePaymentMethodHandler is not set`() = runTest {
        assertFailsWith<DeferredIntentCallbackNotFoundException> {
            createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
            )
        }
    }

    @Test
    fun `create() with DeferredIntent returns DeferredIntentConfirmationInterceptor`() =
        runScenario(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            intentCreationCallbackProvider = Provider {
                CreateIntentCallback { _, _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123")
                }
            },
        ) {
            assertThat(interceptor).isInstanceOf(DeferredIntentConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with DeferredIntent fails if CreateIntentCallback is not set`() = runTest {
        assertFailsWith<DeferredIntentCallbackNotFoundException> {
            createIntentConfirmationInterceptor(
                initializationMode = DEFAULT_DEFERRED_INTENT,
            )
        }
    }

    @Test
    fun `Message for live key when error without confirm callback is user friendly`() = runTest {
        val exception = assertFailsWith<DeferredIntentCallbackNotFoundException> {
            createIntentConfirmationInterceptor(
                initializationMode = DEFAULT_DEFERRED_INTENT,
                publishableKeyProvider = { "pk_live_12345" },
            )
        }
        assertEquals(
            PaymentsCoreR.string.stripe_internal_error.resolvableString,
            exception.resolvableError
        )
    }

    @Test
    fun `Succeeds if callback is found before timeout time`() {

        val dispatcher = StandardTestDispatcher()
        var callback: CreateIntentCallback? = null

        runTest(dispatcher) {
            val errorReporter = FakeErrorReporter()
            val createJob = async {
                createIntentConfirmationInterceptor(
                    initializationMode = DEFAULT_DEFERRED_INTENT,
                    errorReporter = errorReporter,
                    intentCreationCallbackProvider = Provider { callback },
                )
            }
            dispatcher.scheduler.advanceTimeBy(1000)
            assertTrue(createJob.isActive)

            callback = CreateIntentCallback { _, _ ->
                CreateIntentResult.Success(clientSecret = "pi_123")
            }

            dispatcher.scheduler.advanceTimeBy(1001)
            assertFalse(createJob.isActive)
            assertTrue(createJob.isCompleted)
            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING.eventName,
            )
        }
    }

    @Test
    fun `create() with PaymentIntent returns IntentFirstConfirmationInterceptor`() =
        runScenario(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        ) {
            assertThat(interceptor).isInstanceOf(IntentFirstConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with SetupIntent returns IntentFirstConfirmationInterceptor`() =
        runScenario(
            initializationMode = InitializationMode.SetupIntent("cs_123")
        ) {
            assertThat(interceptor).isInstanceOf(IntentFirstConfirmationInterceptor::class.java)
        }

    private data class Scenario(
        val interceptor: IntentConfirmationInterceptor,
    )

    private fun runScenario(
        initializationMode: InitializationMode,
        intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
        preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null },
        block: suspend Scenario.() -> Unit,
    ) {
        runTest {
            Scenario(
                interceptor = createIntentConfirmationInterceptor(
                    initializationMode = initializationMode,
                    intentCreationCallbackProvider = intentCreationCallbackProvider,
                    preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                ),
            ).block()
        }
    }
}
