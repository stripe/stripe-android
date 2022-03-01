package com.stripe.android.paymentsheet

import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodsAdapterTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripePaymentSheetDefaultTheme
    )

    @Test
    fun `when adapter is disabled then items should be disabled`() {
        val adapter = createAdapter()
        adapter.isEnabled = false

        val viewHolder = Mockito.spy(
            adapter.onCreateViewHolder(
                FrameLayout(context),
                adapter.getItemViewType(0)
            )
        )
        adapter.bindViewHolder(viewHolder, 0)
        verify(viewHolder, times(1))
            .bind(
                paymentMethod = eq(SupportedPaymentMethod.Card),
                isSelected = eq(true),
                isEnabled = eq(false),
                onItemSelectedListener = any()
            )
    }

    private fun createAdapter() = AddPaymentMethodsAdapter(
        SupportedPaymentMethod.values(), 0
    ) { }
}
