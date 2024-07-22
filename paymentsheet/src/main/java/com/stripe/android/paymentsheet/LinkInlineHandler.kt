package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class LinkInlineHandler(
    private val coroutineScope: CoroutineScope,
    private val payWithLink: suspend (
        userInput: UserInput?,
        paymentSelection: PaymentSelection?,
        shouldCompleteLinkInlineFlow: Boolean,
    ) -> Unit,
    private val selection: StateFlow<PaymentSelection?>,
    private val updateLinkPrimaryButtonUiState: (PrimaryButton.UIState?) -> Unit,
    private val primaryButtonLabel: StateFlow<ResolvableString?>,
    private val shouldCompleteLinkFlowInline: Boolean,
) {
    private val linkInlineSignUpState = MutableStateFlow<InlineSignupViewState?>(null)

    init {
        coroutineScope.launch {
            var inLinkSignUpMode = false

            combine(
                selection,
                linkInlineSignUpState
            ) { paymentSelection, linkInlineSignUpState ->
                Pair(paymentSelection, linkInlineSignUpState)
            }.collect { pair ->
                val (paymentSelection, linkInlineSignUpState) = pair
                // Only reset custom primary button state if we haven't already
                if (paymentSelection !is PaymentSelection.New.Card) {
                    if (inLinkSignUpMode) {
                        // US bank account will update the custom primary state on its own
                        if (paymentSelection !is PaymentSelection.New.USBankAccount) {
                            updateLinkPrimaryButtonUiState(null)
                        }

                        inLinkSignUpMode = false
                    }

                    return@collect
                }

                inLinkSignUpMode = true

                if (linkInlineSignUpState != null) {
                    updatePrimaryButton(linkInlineSignUpState)
                }
            }
        }
    }

    private fun updatePrimaryButton(viewState: InlineSignupViewState) {
        val label = primaryButtonLabel.value ?: return

        updateLinkPrimaryButtonUiState(
            if (viewState.useLink) {
                val userInput = viewState.userInput
                val paymentSelection = selection.value

                if (userInput != null && paymentSelection != null) {
                    PrimaryButton.UIState(
                        label = label,
                        onClick = { payWithLinkInline(userInput) },
                        enabled = true,
                        lockVisible = shouldCompleteLinkFlowInline,
                    )
                } else {
                    PrimaryButton.UIState(
                        label = label,
                        onClick = {},
                        enabled = false,
                        lockVisible = shouldCompleteLinkFlowInline,
                    )
                }
            } else {
                null
            }
        )
    }

    private fun payWithLinkInline(userInput: UserInput?) {
        coroutineScope.launch {
            payWithLink(userInput, selection.value, shouldCompleteLinkFlowInline)
        }
    }

    fun onStateUpdated(state: InlineSignupViewState) {
        linkInlineSignUpState.value = state
    }

    companion object {
        fun create(viewModel: BaseSheetViewModel, coroutineScope: CoroutineScope): LinkInlineHandler {
            return LinkInlineHandler(
                coroutineScope = coroutineScope,
                payWithLink = viewModel.linkHandler::payWithLinkInline,
                selection = viewModel.selection,
                updateLinkPrimaryButtonUiState = { viewModel.customPrimaryButtonUiState.value = it },
                primaryButtonLabel = viewModel.primaryButtonUiState.mapAsStateFlow { it?.label },
                shouldCompleteLinkFlowInline = viewModel.isCompleteFlow,
            )
        }
    }
}
