package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionFixtures
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentFlowPagerAdapterTest {

    private val customerSession: CustomerSession = mock()

    private val adapter: PaymentFlowPagerAdapter by lazy {
        PaymentFlowPagerAdapter(
            ApplicationProvider.getApplicationContext(),
            PaymentSessionFixtures.CONFIG,
            customerSession
        )
    }

    @Test
    fun pageCount_updatesAfterSavingShippingInfo() {
        assertEquals(1, adapter.count)

        adapter.isShippingInfoSubmitted = true
        assertEquals(2, adapter.count)

        adapter.isShippingInfoSubmitted = false
        assertEquals(1, adapter.count)
    }
}
