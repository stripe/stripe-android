package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaymentMethodUpdateParamsTest {
    @Test
    fun `create() with 'allow_redisplay' set for card returns expected values`() {
        assertThat(
            PaymentMethodUpdateParams.createCard(
                expiryMonth = 12,
                expiryYear = 2045,
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "unspecified")

        assertThat(
            PaymentMethodUpdateParams.createCard(
                expiryMonth = 12,
                expiryYear = 2045,
                allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "limited")

        assertThat(
            PaymentMethodUpdateParams.createCard(
                expiryMonth = 12,
                expiryYear = 2045,
                allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
            ).toParamMap()
        ).containsEntry("allow_redisplay", "always")
    }
}
