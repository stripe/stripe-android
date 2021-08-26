package com.stripe.android.paymentsheet

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSheetContractTest {

    @Test
    fun `parseResult() with missing data should return failed result`() {
        assertThat(PaymentSheetContract().parseResult(0, Intent()))
            .isInstanceOf(PaymentSheetResult.Failed::class.java)
    }
}
