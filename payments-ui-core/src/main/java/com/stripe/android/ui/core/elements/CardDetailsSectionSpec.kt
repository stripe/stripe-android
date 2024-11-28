package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
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
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_details"),
    @SerialName("collect_name")
    val collectName: Boolean = false,
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    ): FormElement =
        CardDetailsSectionElement(
            initialValues = initialValues,
            identifier = apiPath,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory
        )
}
