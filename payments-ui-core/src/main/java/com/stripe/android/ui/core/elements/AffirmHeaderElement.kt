package com.stripe.android.ui.core.elements

import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AffirmHeaderElement(
    override val identifier: IdentifierSpec,
    override val controller: Controller? = null
) : FormElement {
    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}

data class PaymentMethodMessageHeader(
    override val identifier: IdentifierSpec,
    override val controller: Controller? = null,
    val messagePromotion: PaymentMethodMessagePromotion
) : FormElement {
    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}
