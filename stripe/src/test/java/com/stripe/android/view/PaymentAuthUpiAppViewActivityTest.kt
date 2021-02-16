package com.stripe.android.view

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PaymentAuthUpiAppViewActivityTest {

    @Test
    fun `Decoder works as expected`() {
        var nativeData = "ZHVtbXlfbmF0aXZlX2RhdGE="
        val viewModel = PaymentAuthUpiAppViewActivity.UpiAuthActivityViewModel()
        var decodedData = viewModel.decode(nativeData)

        assertEquals("dummy_native_data", decodedData)
    }
}
