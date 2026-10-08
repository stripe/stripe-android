package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryNgCardTest {
    @Test
    fun `mandateDataForDeferredIntent returns null without future use`() {
        val result = mandateDataForDeferredIntent(
            paymentMethodType = PaymentMethod.Type.NgCard,
            requiresMandateFromCreateParams = PaymentMethodCreateParams.createNgCard().requiresMandate,
            optionsParams = null,
            intentConfigSetupFutureUsage = null,
        )

        assertThat(result).isNull()
    }
}
