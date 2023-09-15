package com.stripe.android.paymentsheet.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import com.stripe.android.R as StripeR

@RunWith(AndroidJUnit4::class)
class PaymentSheetTopBarStateFactoryTest {

    @Test
    fun `SelectSavedPaymentMethods shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_close)
    }

    @Test
    fun `AddFirstPaymentMethod shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_close)
    }

    @Test
    fun `AddAnotherPaymentMethod shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_back)
    }

    @Test
    fun `Shows test mode badge if not running in live mode`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showTestModeLabel).isTrue()
    }

    @Test
    fun `Hide test mode badge if running in live mode`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = true,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showTestModeLabel).isFalse()
    }

    @Test
    fun `Shows edit menu if displaying customer payment methods`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            paymentMethods = listOf(mock()),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showEditMenu).isTrue()
    }

    @Test
    fun `Hides edit menu if customer has no payment methods`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `Hides edit menu if not on the saved payment methods screen`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = listOf(mock()),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `Shows correct edit menu label when not in editing mode`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_edit)
    }

    @Test
    fun `Shows correct edit menu label when in editing mode`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = true,
            isProcessing = false,
            isEditing = true,
        )

        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_done)
    }

    @Test
    fun `Enables menu when not processing`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.isEnabled).isTrue()
    }

    @Test
    fun `Disables menu when processing`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            paymentMethods = emptyList(),
            isLiveMode = false,
            isProcessing = true,
            isEditing = false,
        )

        assertThat(state.isEnabled).isFalse()
    }

    private fun buildTopBarState(
        screen: PaymentSheetScreen,
        paymentMethods: List<PaymentMethod>,
        isLiveMode: Boolean,
        isProcessing: Boolean,
        isEditing: Boolean,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarStateFactory.create(
            screen = screen,
            paymentMethods = paymentMethods,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
        )
    }
}
