package com.stripe.android.paymentelement.embedded

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.FakePaymentElementLoader
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfigurationHandlerTest {
    @Test
    fun validationFailureReturnsFailureResult() = runTest {
        val loader = FakePaymentElementLoader()
        val handler = DefaultEmbeddedConfigurationHandler(loader)
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("").build(),
        )
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string."
        )
    }

    @Test
    fun paymentElementLoaderIsCalledWithCorrectArguments() = runTest {
        val loader = FakePaymentElementLoader()
        val handler = DefaultEmbeddedConfigurationHandler(loader)
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(result.getOrThrow())
            .isInstanceOf<PaymentElementLoader.State>()
    }
}
