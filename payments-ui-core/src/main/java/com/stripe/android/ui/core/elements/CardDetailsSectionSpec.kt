package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Section containing card details form
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class CardDetailsSectionSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_details"),
    @SerialName("collect_name")
    val collectName: Boolean = false,
) : FormItemSpec() {
    fun transform(
        context: Context,
        isEligibleForCardBrandChoice: Boolean,
        initialValues: Map<IdentifierSpec, String?>,
        viewOnlyFields: Set<IdentifierSpec>,
    ): FormElement =
        CardDetailsSectionElement(
            context = context,
            initialValues = initialValues,
            viewOnlyFields = viewOnlyFields,
            identifier = apiPath,
            collectName = collectName,
            isEligibleForCardBrandChoice = isEligibleForCardBrandChoice,
        )
}
