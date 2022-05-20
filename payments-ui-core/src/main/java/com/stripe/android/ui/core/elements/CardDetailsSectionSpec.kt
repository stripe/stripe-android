package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Section containing card details form
 */
@Serializable
@SerialName("card_details")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class CardDetailsSectionSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec() {
    fun transform(context: Context, initialValues: Map<IdentifierSpec, String?>): FormElement =
        CardDetailsSectionElement(
            context,
            initialValues,
            api_path
        )
}
