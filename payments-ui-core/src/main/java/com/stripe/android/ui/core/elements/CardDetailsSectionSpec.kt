package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Section containing card details form
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class CardDetailsSectionSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_details")
) : FormItemSpec() {
    fun transform(context: Context, initialValues: Map<IdentifierSpec, String?>): FormElement =
        CardDetailsSectionElement(
            context,
            initialValues,
            apiPath
        )
}
