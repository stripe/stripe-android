package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.ConfirmationTokenConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentFirstConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.SharedPaymentTokenConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(SharedPaymentTokenSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultIntentConfirmationInterceptorFactoryTest {

    @Test
    fun `create() with sharedPaymentToken returns SharedPaymentTokenConfirmationInterceptor`() =
        runScenario(
            integrationMetadata = IntegrationMetadata.DeferredIntent.WithSharedPaymentToken(
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
    fun `create() with DeferredIntent returns DeferredIntentConfirmationInterceptor`() =
        runScenario(
            integrationMetadata = IntegrationMetadata.DeferredIntent.WithPaymentMethod(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intentCreationCallbackProvider = Provider {
                CreateIntentCallback { _, _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123")
                }
            },
        ) {
            assertThat(interceptor).isInstanceOf(DeferredIntentConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with DeferredIntent returns ConfirmationTokenConfirmationInterceptor`() =
        runScenario(
            integrationMetadata = IntegrationMetadata.DeferredIntent.WithConfirmationToken(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123")
                }
            },
        ) {
            assertThat(interceptor).isInstanceOf(ConfirmationTokenConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with IntentFirst returns IntentFirstConfirmationInterceptor`() =
        runScenario(
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        ) {
            assertThat(interceptor).isInstanceOf(IntentFirstConfirmationInterceptor::class.java)
        }

    private data class Scenario(
        val interceptor: IntentConfirmationInterceptor,
    )

    private fun runScenario(
        integrationMetadata: IntegrationMetadata,
        intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
        intentCreationConfirmationTokenCallbackProvider: Provider<CreateIntentWithConfirmationTokenCallback?> =
            Provider { null },
        preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null },
        block: suspend Scenario.() -> Unit,
    ) {
        runTest {
            Scenario(
                interceptor = createIntentConfirmationInterceptor(
                    integrationMetadata = integrationMetadata,
                    intentCreationCallbackProvider = intentCreationCallbackProvider,
                    intentCreationConfirmationTokenCallbackProvider = intentCreationConfirmationTokenCallbackProvider,
                    preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                ),
            ).block()
        }
    }
}
