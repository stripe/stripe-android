package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultFlowControllerConfirmationHandlerTest {
    @Test
    fun `bootstrap delegates to confirmation handler`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val handler = DefaultFlowControllerConfirmationHandler(
            coroutineScope = TestScope(),
            confirmationHandler = confirmationHandler,
        )

        val metadata = PaymentMethodMetadataFactory.create()
        handler.bootstrap(metadata)

        assertThat(confirmationHandler.bootstrapTurbine.awaitItem().paymentMethodMetadata)
            .isEqualTo(metadata)
        confirmationHandler.validate()
    }
}
