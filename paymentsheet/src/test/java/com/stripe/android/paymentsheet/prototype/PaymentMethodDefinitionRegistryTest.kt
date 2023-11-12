package com.stripe.android.paymentsheet.prototype

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PaymentMethodDefinitionRegistryTest {
    @Test
    fun ensureNoDuplicatePaymentMethodTypes() {
        assertThat(PaymentMethodDefinitionRegistry.all.size)
            .isEqualTo(PaymentMethodDefinitionRegistry.all.map { it.type.code }.toSet().size)
    }
}
