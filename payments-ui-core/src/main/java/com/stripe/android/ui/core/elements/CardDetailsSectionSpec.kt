package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
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
        cbcEligibility: CardBrandChoiceEligibility,
        initialValues: Map<IdentifierSpec, String?>,
    ): FormElement =
        CardDetailsSectionElement(
            context = context,
            initialValues = initialValues,
            identifier = apiPath,
            collectName = collectName,
            cbcEligibility = cbcEligibility,
        )
}
