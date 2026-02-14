package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal interface TapToAddConfirmationInteractor {
    val state: StateFlow<State>

    data class State(
        val cardBrand: CardBrand,
        val last4: String?,
        val title: ResolvableString,
        val primaryButton: PrimaryButton,
    ) {
        data class PrimaryButton(
            val label: ResolvableString,
            val locked: Boolean,
        )
    }

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddConfirmationInteractor
    }
}

internal class DefaultTapToAddConfirmationInteractor(
    private val tapToAddMode: TapToAddMode,
    private val paymentMethod: PaymentMethod,
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : TapToAddConfirmationInteractor {
    private val _state = MutableStateFlow(createInitialState())
    override val state: StateFlow<TapToAddConfirmationInteractor.State> = _state.asStateFlow()

    private fun createInitialState(): TapToAddConfirmationInteractor.State {
        return TapToAddConfirmationInteractor.State(
            cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = paymentMethod.card?.last4,
            title = createLabel(useAmount = true),
            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                label = createLabel(useAmount = false),
                locked = tapToAddMode == TapToAddMode.Complete,
            )
        )
    }

    private fun createLabel(useAmount: Boolean): ResolvableString {
        return when (tapToAddMode) {
            TapToAddMode.Complete -> buyButtonLabel(
                amount = paymentMethodMetadata.amount().takeIf { useAmount },
                primaryButtonLabel = null,
                isForPaymentIntent = paymentMethodMetadata.stripeIntent is PaymentIntent
            )
            TapToAddMode.Continue -> continueButtonLabel(
                primaryButtonLabel = null
            )
        }
    }

    class Factory @Inject constructor(
        private val tapToAddMode: TapToAddMode,
        private val paymentMethodMetadata: PaymentMethodMetadata,
    ) : TapToAddConfirmationInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddConfirmationInteractor {
            return DefaultTapToAddConfirmationInteractor(
                tapToAddMode = tapToAddMode,
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethod = paymentMethod,
            )
        }
    }
}
