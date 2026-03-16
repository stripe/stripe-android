package com.stripe.android.common.spms

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.taptoadd.ui.requiresTapToAddCvcCollection
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
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

internal interface CvcFormHelper {
    val state: StateFlow<State>
    val formElement: FormElement?

    sealed interface State {
        data object NotRequired : State

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
    private val savedStateHandle: SavedStateHandle,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val paymentMethod: PaymentMethod,
) : CvcFormHelper {
    private var storedCvcValue: String?
        get() = savedStateHandle[CVC_VALUE_KEY]
        set(value) {
            savedStateHandle[CVC_VALUE_KEY] = value
        }

    private val cvcController = CvcController(
        cardBrandFlow = stateFlowOf(paymentMethod.card?.brand ?: CardBrand.Unknown),
        initialValue = storedCvcValue,
    )

    private val cvcElement = CvcElement(
        _identifier = IdentifierSpec.CardCvc,
        controller = cvcController
    )

    override val formElement: FormElement? = if (
        requiresTapToAddCvcCollection(paymentMethodMetadata, paymentMethod)
    ) {
        SectionElement.wrap(
            sectionFieldElement = cvcElement,
            label = R.string.stripe_paymentsheet_confirm_your_cvc.resolvableString,
        )
    } else {
        null
    }

    override val state: StateFlow<CvcFormHelper.State> = formElement?.let {
        cvcController.formFieldValue.mapAsStateFlow { cvcInput ->
            val cvcValue = cvcInput.value
            storedCvcValue = cvcValue

            if (cvcInput.isComplete && !cvcValue.isNullOrEmpty()) {
                CvcFormHelper.State.Complete(cvcValue)
            } else {
                CvcFormHelper.State.Incomplete
            }
        }
    } ?: run {
        stateFlowOf(CvcFormHelper.State.NotRequired)
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val savedStateHandle: SavedStateHandle,
    ) : CvcFormHelper.Factory {
        override fun create(
            paymentMethod: PaymentMethod
        ): CvcFormHelper {
            return DefaultCvcFormHelper(
                savedStateHandle = savedStateHandle,
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethod = paymentMethod,
            )
        }
    }

    @VisibleForTesting
    internal companion object {
        const val CVC_VALUE_KEY = "STRIPE_SPM_CVC_VALUE"
    }
}
