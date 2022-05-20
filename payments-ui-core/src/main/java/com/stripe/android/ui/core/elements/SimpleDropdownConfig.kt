package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SimpleDropdownConfig(
    @StringRes override val label: Int,
    private val items: List<DropdownItemSpec>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override val displayItems: List<String> =
        items.map { it.display_text }

    override fun getSelectedItemLabel(index: Int) = displayItems[index]

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.api_value == rawValue }
            ?.display_text
            ?: items[0].display_text

    override fun convertToRaw(displayName: String) =
        items
            .filter { it.display_text == displayName }
            .map { it.api_value }
            .firstOrNull()
}
