package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
@Suppress("DataClassPrivateConstructor")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class LayoutSpec private constructor(val items: List<FormItemSpec>) : Parcelable {
    companion object {
        fun create(vararg item: FormItemSpec) = LayoutSpec(item.toList())

        // Used for forms that have no elements (i.e. card because it is not supported
        // by compose form)
        fun create() = LayoutSpec(listOf(EmptyFormSpec))
    }
}
