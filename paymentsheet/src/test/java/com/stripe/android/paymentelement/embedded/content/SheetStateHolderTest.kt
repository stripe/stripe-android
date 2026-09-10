package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class SheetStateHolderTest {

    @Test
    fun `sheet state version increments and survives recreation`() {
        val savedStateHandle = SavedStateHandle()
        val sheetStateHolder = SheetStateHolder(savedStateHandle)

        assertThat(sheetStateHolder.sheetStateVersion).isEqualTo(0)

        sheetStateHolder.sheetIsOpen = true
        val recreatedSheetStateHolder = SheetStateHolder(savedStateHandle)

        assertThat(recreatedSheetStateHolder.sheetIsOpen).isTrue()
        assertThat(recreatedSheetStateHolder.sheetStateVersion).isEqualTo(1)

        recreatedSheetStateHolder.sheetIsOpen = false

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(sheetStateHolder.sheetStateVersion).isEqualTo(2)
    }
}
