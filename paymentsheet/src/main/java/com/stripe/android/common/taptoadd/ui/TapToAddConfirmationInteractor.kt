package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.paymentsheet.utils.reportPaymentResult
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

internal interface TapToAddConfirmationInteractor {
    val state: StateFlow<State>

    data class State(
        val cardBrand: CardBrand,
        val last4: String?,
        val title: ResolvableString,
        val primaryButton: PrimaryButton,
        val form: Form,
        val error: ResolvableString?,
    ) {
        data class Form(
            val elements: List<FormElement>,
            val enabled: Boolean,
        )

        data class PrimaryButton(
            val label: ResolvableString,
            val locked: Boolean,
            val state: State,
            val enabled: Boolean,
        ) {
            enum class State {
                Idle,
                Processing,
                Complete
            }
        }
    }

    fun performAction(action: Action)

    sealed interface Action {
        data object PrimaryButtonPressed : Action
        data object ShownSuccess : Action
    }

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddConfirmationInteractor
    }
}

internal class DefaultTapToAddConfirmationInteractor(
    private val coroutineScope: CoroutineScope,
    private val tapToAddMode: TapToAddMode,
    private val paymentMethod: PaymentMethod,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val linkFormHelper: SavedPaymentMethodLinkFormHelper,
    private val eventReporter: EventReporter,
    private val onContinue: (paymentSelection: PaymentSelection.Saved) -> Unit,
    private val onComplete: () -> Unit,
) : TapToAddConfirmationInteractor {
    private val selection = PaymentSelection.Saved(
        paymentMethod = paymentMethod,
    )

    private val _state = MutableStateFlow(
        createInitialState(
            initialLinkState = linkFormHelper.state.value,
            initialConfirmationState = confirmationHandler.state.value,
        )
    )
    override val state: StateFlow<TapToAddConfirmationInteractor.State> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest { confirmationState ->
                if (confirmationState is ConfirmationHandler.State.Complete) {
                    eventReporter.reportPaymentResult(
                        result = confirmationState.result,
                        paymentSelection = selection,
                    )
                }

                _state.update { state ->
                    state.withConfirmationState(confirmationState)
                }
            }
        }

        coroutineScope.launch {
            linkFormHelper.state.collectLatest { linkState ->
                _state.update { state ->
                    state.withLinkState(linkState)
                }
            }
        }
    }

    override fun performAction(action: TapToAddConfirmationInteractor.Action) {
        when (action) {
            TapToAddConfirmationInteractor.Action.PrimaryButtonPressed -> {
                when (tapToAddMode) {
                    TapToAddMode.Continue -> onPrimaryButtonWithContinueMode()
                    TapToAddMode.Complete -> onPrimaryButtonWithCompleteMode()
                }
            }
            TapToAddConfirmationInteractor.Action.ShownSuccess -> onComplete()
        }
    }

    private fun onPrimaryButtonWithContinueMode() {
        onContinue(selection)
    }

    private fun onPrimaryButtonWithCompleteMode() {
        if (state.value.primaryButton.state != TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle) {
            return
        }

        coroutineScope.launch {
            confirmationHandler.start(
                arguments = ConfirmationHandler.Args(
                    confirmationOption = PaymentMethodConfirmationOption.Saved(
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                    ),
                    paymentMethodMetadata = paymentMethodMetadata,
                )
            )
        }
    }

    private fun createInitialState(
        initialLinkState: SavedPaymentMethodLinkFormHelper.State,
        initialConfirmationState: ConfirmationHandler.State
    ): TapToAddConfirmationInteractor.State {
        return TapToAddConfirmationInteractor.State(
            cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = paymentMethod.card?.last4,
            title = createLabel(useAmount = true),
            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                label = createLabel(useAmount = false),
                locked = tapToAddMode == TapToAddMode.Complete,
                enabled = true,
                state = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            ),
            form = TapToAddConfirmationInteractor.State.Form(
                elements = linkFormHelper.formElement?.let {
                    listOf(it)
                } ?: emptyList(),
                enabled = true,
            ),
            error = null,
        )
            .withLinkState(initialLinkState)
            .withConfirmationState(initialConfirmationState)
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

    private fun TapToAddConfirmationInteractor.State.withLinkState(
        linkState: SavedPaymentMethodLinkFormHelper.State,
    ): TapToAddConfirmationInteractor.State {
        return copy(
            primaryButton = primaryButton.copy(
                enabled = linkState !is SavedPaymentMethodLinkFormHelper.State.Incomplete,
            ),
        )
    }

    private fun TapToAddConfirmationInteractor.State.withConfirmationState(
        confirmationState: ConfirmationHandler.State
    ): TapToAddConfirmationInteractor.State {
        val primaryButtonState = when (confirmationState) {
            is ConfirmationHandler.State.Idle ->
                TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle
            is ConfirmationHandler.State.Confirming ->
                TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing
            is ConfirmationHandler.State.Complete -> {
                if (confirmationState.result is ConfirmationHandler.Result.Succeeded) {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Complete
                } else {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle
                }
            }
        }

        return copy(
            form = form.copy(
                enabled = confirmationState is ConfirmationHandler.State.Idle,
            ),
            primaryButton = primaryButton.copy(
                state = primaryButtonState,
            ),
            error = if (
                confirmationState is ConfirmationHandler.State.Complete &&
                confirmationState.result is ConfirmationHandler.Result.Failed
            ) {
                confirmationState.result.message
            } else {
                null
            },
        )
    }

    class Factory @Inject constructor(
        @ViewModelScope private val viewModelScope: CoroutineScope,
        private val tapToAddMode: TapToAddMode,
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val linkFormHelper: SavedPaymentMethodLinkFormHelper,
        private val confirmationHandler: ConfirmationHandler,
        private val eventReporter: EventReporter,
        private val tapToAddNavigator: Provider<TapToAddNavigator>,
    ) : TapToAddConfirmationInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddConfirmationInteractor {
            return DefaultTapToAddConfirmationInteractor(
                tapToAddMode = tapToAddMode,
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethod = paymentMethod,
                confirmationHandler = confirmationHandler,
                eventReporter = eventReporter,
                coroutineScope = viewModelScope,
                linkFormHelper = linkFormHelper,
                onComplete = {
                    tapToAddNavigator.get().performAction(TapToAddNavigator.Action.Complete)
                },
                onContinue = { paymentSelection ->
                    tapToAddNavigator.get().performAction(TapToAddNavigator.Action.Continue(paymentSelection))
                }
            )
        }
    }
}
