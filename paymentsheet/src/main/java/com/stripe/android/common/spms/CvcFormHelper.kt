package com.stripe.android.common.spms

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

interface CvcFormHelper {
    val state: StateFlow<State>
    val formElement: FormElement?

    sealed interface State {
        data object Unused : State

        data object Incomplete : State

        data class Complete(val cvc: String) : State
    }

    interface Factory {
        fun create(
            paymentMethod: PaymentMethod,
        ): CvcFormHelper
    }
}

internal class DefaultCvcFormHelper(
    paymentMethodMetadata: PaymentMethodMetadata,
    private val savedStateHandle: SavedStateHandle,
    private val paymentMethod: PaymentMethod,
) : CvcFormHelper {

    // TODO: save input state using savedStateHandle.
    private val requiresCvcRecollection: Boolean =
        paymentMethod.type == PaymentMethod.Type.Card &&
            (paymentMethodMetadata.stripeIntent as? PaymentIntent)?.requireCvcRecollection == true

    private val cvcController = CvcController(
        cardBrandFlow = stateFlowOf(paymentMethod.card?.brand ?: CardBrand.Unknown)
    )

    private val cvcElement = if (requiresCvcRecollection) {
        CvcElement(
            _identifier = IdentifierSpec.CardCvc,
            controller = cvcController
        )
    } else {
        null
    }

    override val formElement: FormElement? = cvcElement?.let {
        SectionElement.wrap(
            sectionFieldElement = it,
            label = R.string.stripe_paymentsheet_confirm_your_cvc.resolvableString
        )
    }

    override val state: StateFlow<CvcFormHelper.State> = cvcController.formFieldValue.mapAsStateFlow { cvcInput ->
        val cvcInputValue = cvcInput.value
        if (!requiresCvcRecollection) {
            CvcFormHelper.State.Unused
        } else if (cvcInput.isComplete && cvcInputValue != null) {
            CvcFormHelper.State.Complete(cvcInputValue)
        } else {
            CvcFormHelper.State.Incomplete
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val savedStateHandle: SavedStateHandle,
    ) : CvcFormHelper.Factory {
        override fun create(
            paymentMethod: PaymentMethod
        ): CvcFormHelper {
            return DefaultCvcFormHelper(
                paymentMethodMetadata = paymentMethodMetadata,
                savedStateHandle = savedStateHandle,
                paymentMethod = paymentMethod,
            )
        }
    }
}