package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessageHeaderElement(
    override val identifier: FormFieldId,
    override val controller: Controller? = null,
    val promotion: PaymentMethodMessagePromotion
) : FormElement {
    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<FormFieldId, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}
