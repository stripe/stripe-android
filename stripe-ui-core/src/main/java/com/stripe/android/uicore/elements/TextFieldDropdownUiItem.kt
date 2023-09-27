package com.stripe.android.uicore.elements

import androidx.compose.runtime.Composable
import com.stripe.android.uicore.strings.resolve

internal data class TextFieldDropdownUiItem constructor(
    override val label: String,
    override val icon: Int?,
    val item: TextFieldIcon.Dropdown.Item
) : SingleChoiceDropdownItem {
    companion object {
        @Composable
        fun create(item: TextFieldIcon.Dropdown.Item): TextFieldDropdownUiItem {
            return TextFieldDropdownUiItem(
                label = item.label.resolve(),
                icon = item.icon,
                item = item
            )
        }
    }
}
