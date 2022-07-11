package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Section containing card details form
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class CardDetailsSectionSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_details")
) : FormItemSpec() {
    fun transform(
        context: Context,
        initialValues: Map<IdentifierSpec, String?>,
        viewOnlyFields: Set<IdentifierSpec>
    ): FormElement =
        CardDetailsSectionElement(
            context = context,
            initialValues = initialValues,
            viewOnlyFields = viewOnlyFields,
            identifier = apiPath
        )
}
