package com.stripe.android.paymentsheet.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.R as StripeR

@RunWith(AndroidJUnit4::class)
class PaymentSheetTopBarStateFactoryTest {

    @Test
    fun `SelectSavedPaymentMethods shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            canEdit = false,
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_close)
    }

    @Test
    fun `ManageSavedPaymentMethods shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = FakeManageScreenInteractor()),
            canEdit = false,
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_back)
    }

    @Test
    fun `AddFirstPaymentMethod shows correct navigation icon`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            canEdit = false,
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
            canEdit = false,
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
            canEdit = false,
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
            canEdit = false,
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
            canEdit = true,
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showEditMenu).isTrue()
    }

    @Test
    fun `Shows edit menu if displaying customer payment methods in manage screen`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = FakeManageScreenInteractor()),
            canEdit = true,
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
            canEdit = false,
            isLiveMode = false,
            isProcessing = false,
            isEditing = false,
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `Hides edit menu if cannot edit on manage screen`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = FakeManageScreenInteractor()),
            canEdit = false,
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
            canEdit = true,
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
            canEdit = false,
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
            canEdit = false,
            isLiveMode = true,
            isProcessing = false,
            isEditing = true,
        )

        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_done)
    }

    @Test
    fun `Shows correct edit menu label when in editing mode on manage screen`() {
        val state = buildTopBarState(
            screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = FakeManageScreenInteractor()),
            canEdit = false,
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
            canEdit = false,
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
            canEdit = false,
            isLiveMode = false,
            isProcessing = true,
            isEditing = false,
        )

        assertThat(state.isEnabled).isFalse()
    }

    private fun buildTopBarState(
        screen: PaymentSheetScreen,
        isLiveMode: Boolean,
        isProcessing: Boolean,
        isEditing: Boolean,
        canEdit: Boolean,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarStateFactory.create(
            screen = screen,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            canEdit = canEdit,
        )
    }
}
