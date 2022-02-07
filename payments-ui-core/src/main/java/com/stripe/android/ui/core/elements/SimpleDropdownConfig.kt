package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SimpleDropdownConfig(
    @StringRes override val label: Int,
    private val items: List<DropdownItemSpec>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override fun getDisplayItems(): List<String> =
        items.map { it.text }

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.value == rawValue }
            ?.text
            ?: items[0].text

    override fun convertToRaw(displayName: String) =
        items
            .filter { it.text == displayName }
            .map { it.value }
            .firstOrNull()
}
