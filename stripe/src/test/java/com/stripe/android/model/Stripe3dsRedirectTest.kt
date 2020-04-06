package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Stripe3dsRedirectTest {

    @Test
    fun create_shouldReturnExpectedObject() {
        val redirect = Stripe3dsRedirect.create(
            requireNotNull(PaymentIntentFixtures.PI_REQUIRES_3DS1.stripeSdkData)
        )
        assertThat(redirect.url)
            .isEqualTo(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
            )
    }
}
