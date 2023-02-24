package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class UpiSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Upi
) : FormItemSpec() {
    fun transform(): SectionElement {
        return createSectionElement(
            sectionFieldElement = UpiElement(identifier = IdentifierSpec.Vpa),
            label = R.string.stripe_paymentsheet_buy_using_upi_id
        )
    }
}
