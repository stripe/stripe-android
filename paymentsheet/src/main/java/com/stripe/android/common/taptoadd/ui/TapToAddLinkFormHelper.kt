package com.stripe.android.common.taptoadd.ui

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal interface TapToAddLinkFormHelper {
    val state: StateFlow<State>
    val formElement: FormElement?

    sealed interface State {
        data object Unused : State

        data object Incomplete : State

        data class Complete(val userInput: UserInput) : State
    }
}

internal class DefaultTapToAddLinkFormHelper @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    linkFormElementFactory: TapToAddLinkFormElementFactory,
) : TapToAddLinkFormHelper {
    private val linkState = paymentMethodMetadata.linkState

    private var storedCheckboxSelection: Boolean
        get() = savedStateHandle.get<Boolean>(TAP_TO_ADD_LINK_CHECKBOX_SELECTED_KEY) == true
        set(value) {
            savedStateHandle[TAP_TO_ADD_LINK_CHECKBOX_SELECTED_KEY] = value
        }

    private var storedLinkInput: UserInput?
        get() = savedStateHandle[TAP_TO_ADD_LINK_INPUT_KEY]
        set(value) {
            savedStateHandle[TAP_TO_ADD_LINK_INPUT_KEY] = value
        }

    private val _state = MutableStateFlow<TapToAddLinkFormHelper.State>(TapToAddLinkFormHelper.State.Unused)
    override val state: StateFlow<TapToAddLinkFormHelper.State> = _state.asStateFlow()

    override val formElement: FormElement? = if (linkState?.signupMode != null) {
        linkFormElementFactory.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            configuration = linkState.configuration,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            userInput = storedLinkInput,
            onLinkInlineSignupStateChanged = { viewState ->
                storedCheckboxSelection = viewState.isExpanded
                storedLinkInput = viewState.userInput

                _state.value = createState(
                    useLink = viewState.useLink,
                    userInput = viewState.userInput,
                )
            },
            previousLinkSignupCheckboxSelection = storedCheckboxSelection,
        )
    } else {
        null
    }

    private fun createState(
        useLink: Boolean,
        userInput: UserInput?,
    ): TapToAddLinkFormHelper.State {
        return when {
            useLink && userInput != null -> TapToAddLinkFormHelper.State.Complete(userInput)
            useLink -> TapToAddLinkFormHelper.State.Incomplete
            else -> TapToAddLinkFormHelper.State.Unused
        }
    }

    private companion object {
        const val TAP_TO_ADD_LINK_CHECKBOX_SELECTED_KEY = "STRIPE_TAD_TO_ADD_LINK_CHECKBOX_SELECTED"
        const val TAP_TO_ADD_LINK_INPUT_KEY = "STRIPE_TAD_TO_ADD_LINK_INPUT"
    }
}
