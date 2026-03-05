package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.spms.CvcFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddCollectCvcInteractor {
    val state: StateFlow<State>

    data class State(
        val cardBrand: CardBrand,
        val last4: String?,
        val title: ResolvableString,
        val primaryButton: PrimaryButton,
        val form: Form,
    ) {
        data class PrimaryButton(
            val label: ResolvableString,
            val enabled: Boolean,
        )

        data class Form(
            val cvcElement: FormElement,
            val enabled: Boolean,
        )
    }

    fun performAction(action: Action)

    sealed interface Action {
        data object PrimaryButtonPressed : Action
    }

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddCollectCvcInteractor
    }
}

internal class DefaultTapToAddCollectCvcInteractor(
    coroutineScope: CoroutineScope,
    private val tapToAddMode: TapToAddMode,
    private val paymentMethod: PaymentMethod,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val cvcFormHelper: CvcFormHelper,
    private val onContinue: (PaymentMethodOptionsParams.Card) -> Unit,
) : TapToAddCollectCvcInteractor {
    private val _state = MutableStateFlow(
        TapToAddCollectCvcInteractor.State(
            cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = paymentMethod.card?.last4,
            title = createTitleLabel(),
            primaryButton = TapToAddCollectCvcInteractor.State.PrimaryButton(
                label = continueButtonLabel(primaryButtonLabel = null),
                enabled = cvcFormHelper.state.value is CvcFormHelper.State.Complete,
            ),
            form = TapToAddCollectCvcInteractor.State.Form(
                cvcElement = cvcFormHelper.formElement,
                enabled = true,
            )
        )
    )

    override val state: StateFlow<TapToAddCollectCvcInteractor.State> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            cvcFormHelper.state.collectLatest { cvcState ->
                _state.update { viewState ->
                    viewState.copy(
                        primaryButton = viewState.primaryButton.copy(
                            enabled = cvcState is CvcFormHelper.State.Complete
                        )
                    )
                }
            }
        }
    }

    override fun performAction(action: TapToAddCollectCvcInteractor.Action) {
        when (action) {
            TapToAddCollectCvcInteractor.Action.PrimaryButtonPressed -> {
                val cvcState = cvcFormHelper.state.value
                if (cvcState is CvcFormHelper.State.Complete) {
                    onContinue(PaymentMethodOptionsParams.Card(cvc = cvcState.cvc))
                }
            }
        }
    }

    private fun createTitleLabel(): ResolvableString {
        return when (tapToAddMode) {
            TapToAddMode.Complete -> buyButtonLabel(
                amount = paymentMethodMetadata.amount(),
                primaryButtonLabel = null,
                isForPaymentIntent = paymentMethodMetadata.stripeIntent is PaymentIntent
            )
            TapToAddMode.Continue -> continueButtonLabel(
                primaryButtonLabel = null
            )
        }
    }

    class Factory @Inject constructor(
        @ViewModelScope private val viewModelScope: CoroutineScope,
        private val tapToAddMode: TapToAddMode,
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val cvcFormHelperFactory: CvcFormHelper.Factory,
        private val tapToAddConfirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
        private val tapToAddNavigator: Provider<TapToAddNavigator>,
    ) : TapToAddCollectCvcInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddCollectCvcInteractor {
            return DefaultTapToAddCollectCvcInteractor(
                coroutineScope = viewModelScope,
                tapToAddMode = tapToAddMode,
                paymentMethod = paymentMethod,
                paymentMethodMetadata = paymentMethodMetadata,
                cvcFormHelper = cvcFormHelperFactory.create(paymentMethod),
                onContinue = { paymentMethodOptionsParams ->
                    tapToAddNavigator.get().performAction(
                        TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.Confirmation(
                                interactor = tapToAddConfirmationInteractorFactory.create(
                                    paymentMethod = paymentMethod,
                                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                                )
                            ),
                        ),
                    )
                },
            )
        }
    }
}
