package com.stripe.android.link.ui.menu

import com.stripe.android.core.strings.ResolvableString

/**
 * An item to be displayed in a [LinkMenu].
 *
 * @property text The text content of the item
 * @property isDestructive Whether this item should be rendered with the error text color
 */
internal interface LinkMenuItem {
    val text: ResolvableString
    val tag: String
    val isDestructive: Boolean
}
