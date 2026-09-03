package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.DropdownConfig
import org.junit.Test

class SimpleDropdownConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        resolvableString(R.string.stripe_payment_method_bank),
        listOf(
            DropdownItem(displayText = "Option A", apiValue = "option_a"),
            DropdownItem(displayText = "Option B", apiValue = "option_b"),
            DropdownItem(displayText = "Option C", apiValue = "option_c"),
        )
    )

    @Test
    fun `Verify getDisplayItems gets list of display strings`() {
        assertThat(config.displayItems)
            .isEqualTo(
                listOf(
                    "Option A",
                    "Option B",
                    "Option C",
                )
            )
    }

    @Test
    fun `Verify convert from value returns appropriate string`() {
        assertThat(config.convertFromRaw("option_b"))
            .isEqualTo("Option B")
    }
}
