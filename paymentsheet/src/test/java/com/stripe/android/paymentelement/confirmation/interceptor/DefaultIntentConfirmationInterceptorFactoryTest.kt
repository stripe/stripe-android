package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentFirstConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.SharedPaymentTokenConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
        ) {
            assertThat(interceptor).isInstanceOf(SharedPaymentTokenConfirmationInterceptor::class.java)
        }

    @Test
    fun `create() with DeferredIntent returns DeferredIntentConfirmationInterceptor`() =
        runScenario(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
        ) {
            assertThat(interceptor).isInstanceOf(DeferredIntentConfirmationInterceptor::class.java)
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
        block: suspend Scenario.() -> Unit,
    ) {
        val scenario = Scenario(
            interceptor = createIntentConfirmationInterceptor(initializationMode)
        )
        runTest {
            scenario.block()
        }
    }
}
