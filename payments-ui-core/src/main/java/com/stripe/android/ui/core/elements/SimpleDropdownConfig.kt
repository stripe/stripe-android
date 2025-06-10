package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.DropdownConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SimpleDropdownConfig(
    override val label: ResolvableString,
    private val items: List<DropdownItemSpec>
) : DropdownConfig {
    override val debugLabel = "simple_dropdown"

    override val rawItems = items.map { it.apiValue }

    override val displayItems = items.map { it.displayText }

    override fun getSelectedItemLabel(index: Int) = displayItems[index]

    override fun convertFromRaw(rawValue: String) =
        items
            .firstOrNull { it.apiValue == rawValue }
            ?.displayText
            ?: items[0].displayText
}
