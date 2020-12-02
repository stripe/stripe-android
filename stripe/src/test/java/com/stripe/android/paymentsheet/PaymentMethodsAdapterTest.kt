package com.stripe.android.paymentsheet

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val paymentSelections = mutableListOf<PaymentSelection>()
    private var addCardClicks = 0

    @Test
    fun `item count when Google Pay is enabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = true)
        assertThat(adapter.itemCount)
            .isEqualTo(8)
    }

    @Test
    fun `item count when Google Pay is disabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = false)
        assertThat(adapter.itemCount)
            .isEqualTo(7)
    }

    @Test
    fun `getItemId() when Google Pay is enabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = true)
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentMethodsAdapter.ADD_NEW_ID)
        assertThat(adapter.getItemId(1))
            .isEqualTo(PaymentMethodsAdapter.GOOGLE_PAY_ID)
    }

    @Test
    fun `getItemId() when Google Pay is disabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = false)
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentMethodsAdapter.ADD_NEW_ID)
        assertThat(adapter.getItemId(1))
            .isNotEqualTo(PaymentMethodsAdapter.GOOGLE_PAY_ID)
    }

    @Test
    fun `getItemViewType() when Google Pay is enabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = true)
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentMethodsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentMethodsAdapter.ViewType.GooglePay.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentMethodsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `getItemViewType() when Google Pay is disabled should return expected value`() {
        val adapter = createAdapter(shouldShowGooglePay = false)
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentMethodsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentMethodsAdapter.ViewType.Card.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentMethodsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `click on Google Pay view should update payment selection`() {
        val adapter = createAdapter(shouldShowGooglePay = true)
        val googlePayViewHolder = adapter.onCreateViewHolder(
            FrameLayout(context),
            adapter.getItemViewType(1)
        )
        googlePayViewHolder.itemView.performClick()

        assertThat(paymentSelections)
            .isEqualTo(listOf(PaymentSelection.GooglePay))
    }

    private fun createAdapter(
        shouldShowGooglePay: Boolean,
        paymentMethodsCount: Int = 6
    ): PaymentMethodsAdapter {
        return PaymentMethodsAdapter(
            paymentSelection = null,
            paymentMethodSelectedListener = {
                paymentSelections.add(it)
            },
            addCardClickListener = {
                addCardClicks++
            }
        ).also {
            it.shouldShowGooglePay = shouldShowGooglePay
            it.paymentMethods = PaymentMethodFixtures.createCards(paymentMethodsCount)
        }
    }
}
