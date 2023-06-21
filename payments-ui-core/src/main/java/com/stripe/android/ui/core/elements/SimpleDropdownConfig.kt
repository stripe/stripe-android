package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.elements.DropdownConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SimpleDropdownConfig(
    @StringRes override val label: Int,
    private val items: List<DropdownItemSpec>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override val rawItems = items.map { it.apiValue }

    override val displayItems = items.map { it.displayText }

    override val showSearch: Boolean
        get() = false

    override fun getSelectedItemLabel(index: Int) = displayItems[index]

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.apiValue == rawValue }
            ?.displayText
            ?: items[0].displayText
}
