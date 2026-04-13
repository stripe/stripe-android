package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.FormElement
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
import com.stripe.android.ui.core.R as StripeUiCoreR

internal interface TapToAddCardAddedInteractor {
    val state: StateFlow<State>

    data class State(
        val cardBrand: CardBrand,
        val last4: String?,
        val title: ResolvableString,
        val primaryButton: PrimaryButton?,
        val form: Form,
    ) {
        data class PrimaryButton(
            val label: ResolvableString,
            val enabled: Boolean,
        )

        data class Form(
            val elements: List<FormElement>,
            val enabled: Boolean,
        )
    }

    fun performAction(action: Action)

    fun close()

    sealed interface Action {
        data object ScreenShown : Action
        data object PrimaryButtonPressed : Action
        data object CancelPressed : Action
    }

    interface Factory {
        fun create(paymentMethod: PaymentMethod): TapToAddCardAddedInteractor
    }
}

internal class DefaultTapToAddCardAddedInteractor(
    coroutineContext: CoroutineContext,
    private val tapToAddMode: TapToAddMode,
    private val paymentMethod: PaymentMethod,
    private val eventReporter: EventReporter,
    private val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
    private val onContinue: (PaymentSelection.Saved) -> Unit,
    private val onConfirm: (PaymentMethod, UserInput?) -> Unit,
) : TapToAddCardAddedInteractor {
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    private val _state = MutableStateFlow(
        TapToAddCardAddedInteractor.State(
            cardBrand = paymentMethod.card?.brand ?: CardBrand.Unknown,
            last4 = paymentMethod.card?.last4,
            title = R.string.stripe_tap_to_add_card_added_title.resolvableString,
            primaryButton = TapToAddCardAddedInteractor.State.PrimaryButton(
                label = StripeUiCoreR.string.stripe_continue_button_label.resolvableString,
                enabled = true,
            ),
            form = TapToAddCardAddedInteractor.State.Form(
                elements = savedPaymentMethodLinkFormHelper.formElement?.let {
                    listOf(it)
                } ?: emptyList(),
                enabled = true,
            ),
        ).withLinkState(savedPaymentMethodLinkFormHelper.state.value)
    )

    override val state: StateFlow<TapToAddCardAddedInteractor.State> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            savedPaymentMethodLinkFormHelper.state.collectLatest { linkState ->
                _state.update { viewState ->
                    viewState.withLinkState(linkState)
                }
            }
        }
    }

    override fun performAction(action: TapToAddCardAddedInteractor.Action) {
        when (action) {
            TapToAddCardAddedInteractor.Action.ScreenShown -> {
                onScreenShown()
            }
            TapToAddCardAddedInteractor.Action.PrimaryButtonPressed -> {
                eventReporter.onTapToAddContinueAfterCardAdded()
                onPrimaryButtonPressed()
            }
            TapToAddCardAddedInteractor.Action.CancelPressed -> {
                eventReporter.onTapToAddCanceled(EventReporter.TapToAddCancelSource.CardAdded)
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun onScreenShown() {
        val currentState = state.value

        if (currentState.primaryButton != null) {
            return
        }

        onContinue()
    }

    private fun onPrimaryButtonPressed() {
        when (tapToAddMode) {
            TapToAddMode.Continue -> onContinue()
            TapToAddMode.Complete -> onConfirm(paymentMethod, getLinkInput())
        }
    }

    private fun onContinue() {
        onContinue(
            PaymentSelection.Saved(
                paymentMethod = paymentMethod,
                linkInput = getLinkInput(),
            )
        )
    }

    private fun getLinkInput(): UserInput? {
        return when (val state = savedPaymentMethodLinkFormHelper.state.value) {
            is SavedPaymentMethodLinkFormHelper.State.Unused,
            is SavedPaymentMethodLinkFormHelper.State.Incomplete -> null
            is SavedPaymentMethodLinkFormHelper.State.Complete -> state.userInput
        }
    }

    private fun TapToAddCardAddedInteractor.State.withLinkState(
        linkState: SavedPaymentMethodLinkFormHelper.State,
    ): TapToAddCardAddedInteractor.State {
        return copy(
            primaryButton = primaryButton?.copy(
                enabled = linkState !is SavedPaymentMethodLinkFormHelper.State.Incomplete,
            ).takeIf {
                tapToAddMode == TapToAddMode.Complete || savedPaymentMethodLinkFormHelper.isAvailable
            },
        )
    }

    class Factory @Inject constructor(
        private val tapToAddMode: TapToAddMode,
        private val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
        private val eventReporter: EventReporter,
        private val tapToAddDelayInteractorFactory: TapToAddDelayInteractor.Factory,
        private val tapToAddConfirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
        private val tapToAddNavigator: Provider<TapToAddNavigator>,
        private val tapToAddStateHolder: TapToAddStateHolder,
        @UIContext private val coroutineContext: CoroutineContext
    ) : TapToAddCardAddedInteractor.Factory {
        override fun create(paymentMethod: PaymentMethod): TapToAddCardAddedInteractor {
            return DefaultTapToAddCardAddedInteractor(
                coroutineContext = coroutineContext,
                tapToAddMode = tapToAddMode,
                paymentMethod = paymentMethod,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                eventReporter = eventReporter,
                onContinue = { paymentSelection ->
                    tapToAddNavigator.get().performAction(TapToAddNavigator.Action.Continue(paymentSelection))
                },
                onConfirm = { paymentMethod, linkInput ->
                    tapToAddStateHolder.setState(
                        TapToAddStateHolder.State.Confirmation(
                            paymentMethod = paymentMethod,
                            linkInput = linkInput,
                        )
                    )

                    val screen = TapToAddNavigator.Screen.Confirmation(
                        interactor = tapToAddConfirmationInteractorFactory.create(
                            paymentMethod = paymentMethod,
                            linkInput = linkInput,
                        )
                    )

                    tapToAddNavigator.get().performAction(
                        TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.Delay(
                                interactor = tapToAddDelayInteractorFactory.create(
                                    paymentMethod = paymentMethod,
                                    delayedScreen = screen,
                                )
                            )
                        ),
                    )
                },
            )
        }
    }
}
