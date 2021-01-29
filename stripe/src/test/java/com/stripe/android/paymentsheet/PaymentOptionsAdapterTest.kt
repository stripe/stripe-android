package com.stripe.android.paymentsheet

import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
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
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            )
        )
        assertThat(adapter.itemCount)
            .isEqualTo(8)
    }

    @Test
    fun `item count when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = false
            )
        )
        assertThat(adapter.itemCount)
            .isEqualTo(7)
    }

    @Test
    fun `getItemId() when Google Pay is enabled should return expected value`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            )
        )
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentOptionsAdapter.Item.AddCard.hashCode().toLong())
        assertThat(adapter.getItemId(1))
            .isEqualTo(PaymentOptionsAdapter.Item.GooglePay.hashCode().toLong())
    }

    @Test
    fun `getItemId() when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = false
            )
        )
        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentOptionsAdapter.Item.AddCard.hashCode().toLong())
        assertThat(adapter.getItemId(1))
            .isNotEqualTo(PaymentOptionsAdapter.Item.GooglePay.hashCode().toLong())
    }

    @Test
    fun `getItemViewType() when Google Pay is enabled should return expected value`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            )
        )
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentOptionsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentOptionsAdapter.ViewType.GooglePay.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `getItemViewType() when Google Pay is disabled should return expected value`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = false
            )
        )
        assertThat(adapter.getItemViewType(0))
            .isEqualTo(PaymentOptionsAdapter.ViewType.AddCard.ordinal)
        assertThat(adapter.getItemViewType(1))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
        assertThat(adapter.getItemViewType(2))
            .isEqualTo(PaymentOptionsAdapter.ViewType.Card.ordinal)
    }

    @Test
    fun `click on Google Pay view should update payment selection`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            )
        )
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
    fun `initial selected item should be Google Pay`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            )
        )
        assertThat(adapter.selectedItem)
            .isEqualTo(PaymentOptionsAdapter.Item.GooglePay)
    }

    @Test
    fun `initial selected item should be NewCard when it exists`() {
        val newCard = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            false
        )
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = true
            ),
            newCard = newCard
        )
        assertThat(adapter.selectedItem)
            .isEqualTo(
                PaymentOptionsAdapter.Item.NewCard(newCard)
            )
    }

    @Test
    fun `initial selected item should be null when the only item is AddCard`() {
        val adapter = createConfiguredAdapter(
            CONFIG.copy(
                isGooglePayReady = false,
                paymentMethods = emptyList()
            )
        )
        assertThat(adapter.itemCount)
            .isEqualTo(1)
        assertThat(adapter.selectedItem)
            .isNull()
    }

    private fun createConfiguredAdapter(
        fragmentConfig: FragmentConfig = CONFIG,
        newCard: PaymentSelection.New.Card? = null
    ): PaymentOptionsAdapter {
        return createAdapter().also {
            it.update(fragmentConfig, newCard)
        }
    }

    private fun createAdapter(): PaymentOptionsAdapter {
        return PaymentOptionsAdapter(
            canClickSelectedItem = false,
            paymentOptionSelectedListener = { paymentSelection, _ ->
                paymentSelections.add(paymentSelection)
            },
            addCardClickListener = {
                addCardClicks++
            }
        )
    }

    private companion object {
        private val CONFIG = FragmentConfigFixtures.DEFAULT.copy(
            paymentMethods = PaymentMethodFixtures.createCards(6)
        )
    }
}
