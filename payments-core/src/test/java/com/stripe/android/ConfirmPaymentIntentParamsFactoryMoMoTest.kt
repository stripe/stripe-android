package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryMoMoTest {
    @Test
    fun `mandateDataForDeferredIntent returns mandate when intent is for future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.MoMo,
            requiresMandateFromCreateParams = true,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isEqualTo(MandateDataParams(MandateDataParams.Type.Online.DEFAULT))
    }

    @Test
    fun `mandateDataForDeferredIntent returns null without future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.MoMo,
            requiresMandateFromCreateParams = false,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isNull()
    }
}
