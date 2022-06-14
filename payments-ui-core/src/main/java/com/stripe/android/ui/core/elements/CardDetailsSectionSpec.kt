package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

/**
 * Section containing card details form
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class CardDetailsSectionSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
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

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("card_details")
    }
}
