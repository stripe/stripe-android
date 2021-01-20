package com.stripe.android.paymentsheet

import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentOptionsAdapterTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripePaymentSheetDefaultTheme
    )

    private val paymentSelections = mutableSetOf<PaymentSelection>()
    private var addCardClicks = 0

    @Test
    fun `item count when Google Pay is enabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = true)
        assertThat(adapter.itemCount)
            .isEqualTo(8)
    }

    @Test
    fun `item count when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = false)
        assertThat(adapter.itemCount)
            .isEqualTo(7)
    }

    @Test
    fun `getItemId() when Google Pay is enabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = true)
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentOptionsAdapter.ADD_NEW_ID)
        assertThat(adapter.getItemId(1))
            .isEqualTo(PaymentOptionsAdapter.GOOGLE_PAY_ID)
    }

    @Test
    fun `getItemId() when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = false)
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentOptionsAdapter.ADD_NEW_ID)
        assertThat(adapter.getItemId(1))
            .isNotEqualTo(PaymentOptionsAdapter.GOOGLE_PAY_ID)
    }

    @Test
    fun `getItemViewType() when Google Pay is enabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = true)
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentOptionsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentOptionsAdapter.ViewType.GooglePay.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `getItemViewType() when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = false)
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentOptionsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `click on Google Pay view should update payment selection`() {
        val adapter = createConfiguredAdapter(shouldShowGooglePay = true)
        val googlePayViewHolder = adapter.onCreateViewHolder(
            FrameLayout(context),
            adapter.getItemViewType(1)
        )
        adapter.onBindViewHolder(googlePayViewHolder, 1)
        googlePayViewHolder.itemView.performClick()

        assertThat(paymentSelections)
            .containsExactly(PaymentSelection.GooglePay)
    }

    @Test
    fun `set defaultPaymentMethodId and then paymentMethods should sort and invoke paymentMethodSelectedListener`() {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val adapter = createAdapter()

        adapter.defaultPaymentMethodId = paymentMethods.last().id
        adapter.paymentMethods = paymentMethods
        assertThat(adapter.paymentMethods.first())
            .isEqualTo(paymentMethods.last())

        assertThat(paymentSelections)
            .containsExactly(PaymentSelection.Saved(paymentMethods.last()))
    }

    @Test
    fun `set paymentMethods and then defaultPaymentMethodId should sort and invoke paymentMethodSelectedListener`() {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val adapter = createAdapter()

        adapter.paymentMethods = paymentMethods
        assertThat(adapter.paymentMethods.first())
            .isEqualTo(paymentMethods.first())

        adapter.defaultPaymentMethodId = paymentMethods.last().id
        assertThat(adapter.paymentMethods.first())
            .isEqualTo(paymentMethods.last())

        assertThat(paymentSelections)
            .containsExactly(PaymentSelection.Saved(paymentMethods.last()))
    }

    @Test
    fun `set paymentMethods and then invalid defaultPaymentMethodId should not sort and not invoke paymentMethodSelectedListener`() {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val adapter = createAdapter()
        adapter.paymentMethods = paymentMethods
        adapter.defaultPaymentMethodId = "invalid"
        assertThat(adapter.paymentMethods.first())
            .isEqualTo(paymentMethods.first())
        assertThat(paymentSelections)
            .isEmpty()
    }

    private fun createConfiguredAdapter(
        shouldShowGooglePay: Boolean,
        paymentMethodsCount: Int = 6
    ): PaymentOptionsAdapter {
        return createAdapter().also {
            it.shouldShowGooglePay = shouldShowGooglePay
            it.paymentMethods = PaymentMethodFixtures.createCards(paymentMethodsCount)
        }
    }

    private fun createAdapter(): PaymentOptionsAdapter {
        return PaymentOptionsAdapter(
            paymentSelection = null,
            paymentOptionSelectedListener = { paymentSelection, _ ->
                paymentSelections.add(paymentSelection)
            },
            addCardClickListener = {
                addCardClicks++
            }
        )
    }
}
