package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentSessionFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PaymentFlowPagerAdapterTest {

    private val adapter = PaymentFlowPagerAdapter(
        ApplicationProvider.getApplicationContext(),
        PaymentSessionFixtures.CONFIG
    )

    @Test
    fun pageCount_updatesAfterSavingShippingInfo() {
        assertEquals(1, adapter.count)

        adapter.isShippingInfoSubmitted = true
        assertEquals(2, adapter.count)

        adapter.isShippingInfoSubmitted = false
        assertEquals(1, adapter.count)
    }
}
