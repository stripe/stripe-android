package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Section containing card details form
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class CardDetailsSectionSpec(
    val identifier: IdentifierSpec
) : FormItemSpec() {
    fun transform(context: Context, initialValues: Map<IdentifierSpec, String?>): FormElement =
        CardDetailsSectionElement(
            context,
            initialValues,
            identifier
        )
}
