package com.stripe.android.common.taptoadd

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface TapToAddConfirmationInteractor {
    val state: StateFlow<State>

    fun onAction(action: Action)

    sealed interface Action {
        data object OnPrimaryButtonPress : Action
        data object OnPrimaryButtonAnimationComplete : Action
    }

    data class State(
        val brand: CardBrand,
        val last4: String?,
        val title: ResolvableString,
        val buttonLabel: ResolvableString,
        val state: ConfirmationState,
    ) {
        sealed interface ConfirmationState {
            data class Idle(val message: ResolvableString?) : ConfirmationState
            data object Processing : ConfirmationState
            data class Completed(val intent: StripeIntent) : ConfirmationState
        }
    }
}

internal class DefaultTapToAddConfirmationInteractor(
    private val coroutineScope: CoroutineScope,
    private val customPrimaryButtonLabel: String?,
    private val mode: TapToAddMode,
    private val collectedPaymentMethod: PaymentMethod,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val onConfirmed: (intent: StripeIntent) -> Unit,
): TapToAddConfirmationInteractor {
    private val _state = MutableStateFlow(
        TapToAddConfirmationInteractor.State(
            brand = collectedPaymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = collectedPaymentMethod.card?.last4,
            title = label(useAmount = true),
            buttonLabel = label(useAmount = false),
            state = TapToAddConfirmationInteractor.State.ConfirmationState.Idle(message = null),
        )
    )
    override val state = _state.asStateFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest { confirmationState ->
                _state.update { state ->
                    state.copy(
                        state = confirmationState.toConfirmState(),
                    )
                }
            }
        }
    }

    override fun onAction(action: TapToAddConfirmationInteractor.Action) {
        when (action) {
            is TapToAddConfirmationInteractor.Action.OnPrimaryButtonPress -> {
                coroutineScope.launch {
                    confirmationHandler.start(
                        arguments = ConfirmationHandler.Args(
                            paymentMethodMetadata = paymentMethodMetadata,
                            confirmationOption = PaymentMethodConfirmationOption.Saved(
                                paymentMethod = collectedPaymentMethod,
                                optionsParams = null,
                            )
                        )
                    )
                }
            }
            is TapToAddConfirmationInteractor.Action.OnPrimaryButtonAnimationComplete -> {
                val confirmationState = state.value.state

                if (confirmationState is TapToAddConfirmationInteractor.State.ConfirmationState.Completed) {
                    onConfirmed(confirmationState.intent)
                }
            }
        }
    }

    private fun label(useAmount: Boolean) = when (mode) {
        TapToAddMode.Complete -> buyButtonLabel(
            amount = if (useAmount) {
                paymentMethodMetadata.amount()
            } else {
                null
            },
            primaryButtonLabel = customPrimaryButtonLabel,
            isForPaymentIntent = paymentMethodMetadata.stripeIntent is PaymentIntent,
        )
        TapToAddMode.Continue -> continueButtonLabel(customPrimaryButtonLabel)
    }

    private fun ConfirmationHandler.State.toConfirmState(): TapToAddConfirmationInteractor.State.ConfirmationState {
        return when (this) {
            is ConfirmationHandler.State.Idle -> {
                TapToAddConfirmationInteractor.State.ConfirmationState.Idle(message = null)
            }
            is ConfirmationHandler.State.Confirming -> {
                TapToAddConfirmationInteractor.State.ConfirmationState.Processing
            }
            is ConfirmationHandler.State.Complete -> {
                result.toConfirmState()
            }
        }
    }

    private fun ConfirmationHandler.Result.toConfirmState(): TapToAddConfirmationInteractor.State.ConfirmationState {
        return when (this) {
            is ConfirmationHandler.Result.Canceled -> {
                TapToAddConfirmationInteractor.State.ConfirmationState.Idle(message = null)
            }
            is ConfirmationHandler.Result.Failed -> {
                TapToAddConfirmationInteractor.State.ConfirmationState.Idle(message)
            }
            is ConfirmationHandler.Result.Succeeded -> {
                TapToAddConfirmationInteractor.State.ConfirmationState.Completed(intent)
            }
        }
    }
}
