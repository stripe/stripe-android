package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.stripe.android.R as StripeR

class PaymentSheetTopBarStateFactoryTest {

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
            editable = PaymentSheetTopBarState.Editable.Maybe(
                canEdit = true,
                isEditing = false,
                onEditIconPressed = {},
            ),
        )

        assertThat(state.showEditMenu).isTrue()
    }

    @Test
    fun `showEditMenu=false when canEdit=false`() {
        val state = buildTopBarState(
            editable = PaymentSheetTopBarState.Editable.Maybe(
                canEdit = false,
                isEditing = false,
                onEditIconPressed = {},
            ),
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `showEditMenu=false when editable=Never`() {
        val state = buildTopBarState(
            editable = PaymentSheetTopBarState.Editable.Never,
        )

        assertThat(state.showEditMenu).isFalse()
    }

    @Test
    fun `editMenuLabel=edit when isEditing=false`() {
        val state = buildTopBarState(
            editable = PaymentSheetTopBarState.Editable.Maybe(
                canEdit = false,
                isEditing = false,
                onEditIconPressed = {},
            ),
        )

        assertThat(state.isEditing).isEqualTo(false)
        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_edit)
    }

    @Test
    fun `editMenuLabel=done when isEditing=true`() {
        val state = buildTopBarState(
            editable = PaymentSheetTopBarState.Editable.Maybe(
                canEdit = false,
                isEditing = true,
                onEditIconPressed = {},
            ),
        )

        assertThat(state.isEditing).isEqualTo(true)
        assertThat(state.editMenuLabel).isEqualTo(StripeR.string.stripe_done)
    }

    @Test
    fun `isEditing=false when editable=Never`() {
        val state = buildTopBarState(
            editable = PaymentSheetTopBarState.Editable.Never,
        )

        assertThat(state.isEditing).isEqualTo(false)
    }

    private fun buildTopBarState(
        isLiveMode: Boolean = false,
        editable: PaymentSheetTopBarState.Editable = PaymentSheetTopBarState.Editable.Never,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarStateFactory.create(
            isLiveMode = isLiveMode,
            editable,
        )
    }
}
