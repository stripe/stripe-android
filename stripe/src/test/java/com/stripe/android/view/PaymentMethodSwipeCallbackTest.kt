package com.stripe.android.view

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PaymentMethodSwipeCallbackTest {

    @Test
    fun testCalculateTransitionColor() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val calculatedColor = PaymentMethodSwipeCallback.calculateTransitionColor(
            0.25F,
            ContextCompat.getColor(context, R.color.stripe_swipe_start_payment_method),
            ContextCompat.getColor(context, R.color.stripe_swipe_threshold_payment_method)
        )
        assertEquals(-2312009, calculatedColor)
    }
}
