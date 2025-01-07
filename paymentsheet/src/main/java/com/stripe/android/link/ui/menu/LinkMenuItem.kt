package com.stripe.android.link.ui.menu

/**
 * An item to be displayed in a [LinkMenu].
 *
 * @property textResId The resource ID of the text of the item
 * @property isDestructive Whether this item should be rendered with the error text color
 */
internal interface LinkMenuItem {
    val textResId: Int
    val isDestructive: Boolean
}
