package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Header that displays information about installments for Afterpay
 */
@Serializable
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AfterpayClearpayTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("afterpay_text")
) : FormItemSpec() {
    fun transform(currency: String?, promotion: PaymentMethodMessagePromotion?): FormElement = AfterpayClearpayHeaderElement(apiPath, currency = currency, promotion = promotion)
}
