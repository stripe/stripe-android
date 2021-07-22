package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec


internal class SimpleDropdownConfig(
    @StringRes override val label: Int,
    private val items: List<SectionFieldSpec.Item>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override fun getDisplayItems(): List<String> =
        items.map { it.displayString }

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.rawString == rawValue }
            ?.displayString
            ?: items[0].displayString

    override fun convertToRaw(displayName: String) =
        items
            .filter { it.displayString == displayName }
            .map { it.rawString }
            .firstOrNull()
}
