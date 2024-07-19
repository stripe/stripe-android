package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import org.junit.Test
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

class PaymentSheetTopBarStateFactoryTest {

    @Test
    fun `navigation is close when canNavigateBack=false`() {
        val state = buildTopBarState(
            canNavigateBack = false,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_close)
        assertThat(state.contentDescription).isEqualTo(R.string.stripe_paymentsheet_close)
    }

    @Test
    fun `navigation is back when canNavigateBack=true`() {
        val state = buildTopBarState(
            canNavigateBack = true,
        )

        assertThat(state.icon).isEqualTo(R.drawable.stripe_ic_paymentsheet_back)
        assertThat(state.contentDescription).isEqualTo(StripeUiCoreR.string.stripe_back)
    }

    @Test
    fun `showTestModeLabel=true when isLiveMode=false`() {
        val state = buildTopBarState(
            isLiveMode = false,
        )

        assertThat(state.showTestModeLabel).isTrue()
    }

    @Test
    fun `showTestModeLabel=false when isLiveMode=true`() {
        val state = buildTopBarState(
            isLiveMode = true,
        )

        assertThat(state.showTestModeLabel).isFalse()
    }

    @Test
    fun `showEditMenu=true when canEdit=true`() {
        val state = buildTopBarState(
            canEdit = true,
        )

        assertThat(state.showEditMenu).isTrue()
    }

    @Test
    fun `showEditMenu=false when canEdit=false`() {
        val state = buildTopBarState(
            canEdit = false,
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `editMenuLabel=edit when isEditing=false`() {
        val state = buildTopBarState(
            isEditing = false,
        )

        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_edit)
    }

    @Test
    fun `editMenuLabel=done when isEditing=true`() {
        val state = buildTopBarState(
            isEditing = true,
        )

        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_done)
    }

    private fun buildTopBarState(
        canNavigateBack: Boolean = false,
        isLiveMode: Boolean = false,
        isEditing: Boolean = false,
        canEdit: Boolean = false,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarStateFactory.create(
            hasBackStack = canNavigateBack,
            isLiveMode = isLiveMode,
            isEditing = isEditing,
            canEdit = canEdit,
        )
    }
}
