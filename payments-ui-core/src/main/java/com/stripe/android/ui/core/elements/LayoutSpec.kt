package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
@Suppress("DataClassPrivateConstructor")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LayoutSpec constructor(val items: List<FormItemSpec>) {
    companion object {
        fun create(vararg item: FormItemSpec) = LayoutSpec(item.toList())

        // Used for forms that have no elements (i.e. card because it is not supported
        // by compose form)
        fun create() = LayoutSpec(listOf(EmptyFormSpec))
    }
}
