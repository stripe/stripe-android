package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryMoMoTest {
    @Test
    fun `mandateDataForDeferredIntent returns null when intent is for future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.MoMo,
            requiresMandateFromCreateParams = true,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isNull()
    }
}
