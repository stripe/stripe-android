package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.spms.CvcFormHelper
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConfirmationInteractor {
    val state: StateFlow<State>

    data class State(
        val cardBrand: CardBrand,
        val last4: String?,
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
                Success
            }
        }
    }

    fun performAction(action: Action)

    fun close()

    sealed interface Action {
        data object PrimaryButtonPressed : Action
        data object SuccessShown : Action
    }

    interface Factory {
        fun create(
            paymentMethod: PaymentMethod,
            linkInput: UserInput?,
        ): TapToAddConfirmationInteractor
    }
}

internal class DefaultTapToAddConfirmationInteractor(
    private val coroutineContext: CoroutineContext,
    private val paymentMethod: PaymentMethod,
    private val linkInput: UserInput?,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val cvcFormHelper: CvcFormHelper,
    private val eventReporter: EventReporter,
    private val onComplete: () -> Unit,
) : TapToAddConfirmationInteractor {
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    private val selection = cvcFormHelper.state.mapAsStateFlow { cvcState ->
        PaymentSelection.Saved(
            paymentMethod = paymentMethod,
            linkInput = linkInput
        ).withCvcState(cvcState)
    }

    private val _state = MutableStateFlow(
        createInitialState(
            initialCvcState = cvcFormHelper.state.value,
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
                        paymentSelection = selection.value,
                    )
                }

                _state.update { state ->
                    state.withConfirmationState(confirmationState)
                }
            }
        }

        coroutineScope.launch {
            cvcFormHelper.state.collectLatest { cvcState ->
                _state.update { viewState ->
                    viewState.withCvcState(cvcState)
                }
            }
        }
    }

    override fun performAction(action: TapToAddConfirmationInteractor.Action) {
        when (action) {
            TapToAddConfirmationInteractor.Action.PrimaryButtonPressed -> onPrimaryButtonPressed()
            TapToAddConfirmationInteractor.Action.SuccessShown -> {
                val currentConfirmationState = confirmationHandler.state.value

                if (
                    currentConfirmationState is ConfirmationHandler.State.Complete &&
                    currentConfirmationState.result is ConfirmationHandler.Result.Succeeded
                ) {
                    onComplete()
                }
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun onPrimaryButtonPressed() {
        if (state.value.primaryButton.state != TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle) {
            return
        }

        val confirmationOption = selection.value.toConfirmationOption(
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
        )

        coroutineScope.launch {
            confirmationHandler.start(
                arguments = ConfirmationHandler.Args(
                    confirmationOption = confirmationOption,
                    paymentMethodMetadata = paymentMethodMetadata,
                )
            )
        }
    }

    private fun createInitialState(
        initialCvcState: CvcFormHelper.State,
        initialConfirmationState: ConfirmationHandler.State
    ): TapToAddConfirmationInteractor.State {
        return TapToAddConfirmationInteractor.State(
            cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = paymentMethod.card?.last4,
            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                label = buyButtonLabel(
                    amount = paymentMethodMetadata.amount(),
                    primaryButtonLabel = null,
                    isForPaymentIntent = paymentMethodMetadata.stripeIntent is PaymentIntent
                ),
                locked = true,
                enabled = true,
                state = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            ),
            form = TapToAddConfirmationInteractor.State.Form(
                elements = cvcFormHelper.formElement?.let { listOf(it) } ?: emptyList(),
                enabled = true,
            ),
            error = null,
        )
            .withCvcState(initialCvcState)
            .withConfirmationState(initialConfirmationState)
    }

    private fun PaymentSelection.Saved.withCvcState(
        cvcState: CvcFormHelper.State,
    ): PaymentSelection.Saved {
        return copy(
            paymentMethodOptionsParams = when (cvcState) {
                is CvcFormHelper.State.Complete -> PaymentMethodOptionsParams.Card(cvc = cvcState.cvc)
                is CvcFormHelper.State.Incomplete,
                is CvcFormHelper.State.NotRequired -> null
            }
        )
    }

    private fun TapToAddConfirmationInteractor.State.withCvcState(
        cvcState: CvcFormHelper.State,
    ): TapToAddConfirmationInteractor.State {
        return copy(
            primaryButton = primaryButton.copy(
                enabled = cvcState !is CvcFormHelper.State.Incomplete,
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
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Success
                } else {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle
                }
            }
        }

        val formEnabled = when (confirmationState) {
            is ConfirmationHandler.State.Complete -> {
                confirmationState.result !is ConfirmationHandler.Result.Succeeded
            }
            is ConfirmationHandler.State.Confirming -> false
            is ConfirmationHandler.State.Idle -> true
        }

        return copy(
            form = form.copy(
                enabled = formEnabled,
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
        @UIContext private val coroutineContext: CoroutineContext,
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val cvcFormHelperFactory: CvcFormHelper.Factory,
        private val confirmationHandler: ConfirmationHandler,
        private val eventReporter: EventReporter,
        private val tapToAddNavigator: Provider<TapToAddNavigator>,
    ) : TapToAddConfirmationInteractor.Factory {
        override fun create(
            paymentMethod: PaymentMethod,
            linkInput: UserInput?,
        ): TapToAddConfirmationInteractor {
            return DefaultTapToAddConfirmationInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethod = paymentMethod,
                linkInput = linkInput,
                confirmationHandler = confirmationHandler,
                eventReporter = eventReporter,
                coroutineContext = coroutineContext,
                cvcFormHelper = cvcFormHelperFactory.create(paymentMethod),
                onComplete = {
                    tapToAddNavigator.get().performAction(
                        action = TapToAddNavigator.Action.Complete,
                    )
                },
            )
        }
    }
}
