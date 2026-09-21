package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryGoPayTest {
    @Test
    fun `mandateDataForDeferredIntent returns mandate when intent is for future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.GoPay,
            requiresMandateFromCreateParams = true,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isEqualTo(MandateDataParams(MandateDataParams.Type.Online.DEFAULT))
    }

    @Test
    fun `mandateDataForDeferredIntent returns null without future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.GoPay,
            requiresMandateFromCreateParams = false,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isNull()
    }
}
