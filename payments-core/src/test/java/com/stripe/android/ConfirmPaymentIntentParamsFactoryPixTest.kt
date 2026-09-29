package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import org.junit.Test

internal class ConfirmPaymentIntentParamsFactoryPixTest {
    @Test
    fun `mandate data is inferred when intent is for future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.Pix,
            requiresMandateFromCreateParams = true,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isEqualTo(MandateDataParams(MandateDataParams.Type.Online.DEFAULT))
    }

    @Test
    fun `mandate data is omitted without future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.Pix,
            requiresMandateFromCreateParams = false,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isNull()
    }
}
