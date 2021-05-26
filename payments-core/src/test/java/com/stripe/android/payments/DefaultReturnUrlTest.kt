package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultReturnUrlTest {

    @Test
    fun `value should return expected URL`() {
        assertThat(
            DefaultReturnUrl.create(ApplicationProvider.getApplicationContext()).value
        ).isEqualTo(
            "stripesdk://payment_return_url/com.stripe.android.test"
        )
    }
}
