package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
@Suppress("DataClassPrivateConstructor")
@Parcelize
internal data class LayoutSpec private constructor(val items: List<FormItemSpec>) : Parcelable {
    companion object {
        fun create(vararg item: FormItemSpec) = LayoutSpec(item.toList())

        // Used for forms that have no elements (i.e. card because it is not supported
        // by compose form)
        fun create() = LayoutSpec(listOf(EmptyFormSpec))
    }
}
