package com.stripe.android.paymentsheet

import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
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

        val viewHolder = adapter.onCreateViewHolder(
            FrameLayout(context),
            adapter.getItemViewType(0)
        )
        adapter.bindViewHolder(viewHolder, 0)

        assertThat(viewHolder.itemView.isEnabled)
            .isFalse()
    }

    private fun createAdapter() = AddPaymentMethodsAdapter(
        SupportedPaymentMethod.values(), 0
    ) { }
}
