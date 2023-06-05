package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.R as StripeR

@RunWith(AndroidJUnit4::class)
class PaymentSheetTopBarStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SelectSavedPaymentMethods shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = true,
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
            showEditMenu = false,
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
            showEditMenu = true,
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
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = false,
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
            showEditMenu = false,
            isLiveMode = false,
            isProcessing = true,
            isEditing = false,
        )

        assertThat(state.isEnabled).isFalse()
    }

    private fun buildTopBarState(
        screen: PaymentSheetScreen,
        showEditMenu: Boolean,
        isLiveMode: Boolean,
        isProcessing: Boolean,
        isEditing: Boolean,
    ): PaymentSheetTopBarState {
        var state: PaymentSheetTopBarState? = null

        composeTestRule.setContent {
            state = rememberPaymentSheetTopBarState(
                screen = screen,
                showEditMenu = showEditMenu,
                isLiveMode = isLiveMode,
                isProcessing = isProcessing,
                isEditing = isEditing,
            )
        }

        return state ?: throw AssertionError(
            "buildTopBarState should not produce null result"
        )
    }
}
