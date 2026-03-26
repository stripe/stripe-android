package com.stripe.android.common.spms

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

internal interface SavedPaymentMethodLinkFormHelper {
    val state: StateFlow<State>
    val formElement: FormElement?

    sealed interface State {
        data object Unused : State

        data object Incomplete : State

        data class Complete(val userInput: UserInput) : State
    }
}

internal class DefaultSavedPaymentMethodLinkFormHelper @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    linkFormElementFactory: LinkFormElementFactory,
) : SavedPaymentMethodLinkFormHelper {
    private val linkState = paymentMethodMetadata.linkState

    private var storedCheckboxSelection: Boolean
        get() = savedStateHandle.get<Boolean>(SPM_LINK_CHECKBOX_SELECTED_KEY) == true
        set(value) {
            savedStateHandle[SPM_LINK_CHECKBOX_SELECTED_KEY] = value
        }

    private var storedLinkInput: UserInput?
        get() = savedStateHandle[SPM_LINK_INPUT_KEY]
        set(value) {
            savedStateHandle[SPM_LINK_INPUT_KEY] = value
        }

    private val _state = MutableStateFlow<SavedPaymentMethodLinkFormHelper.State>(
        SavedPaymentMethodLinkFormHelper.State.Unused
    )
    override val state: StateFlow<SavedPaymentMethodLinkFormHelper.State> = _state.asStateFlow()

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
    ): SavedPaymentMethodLinkFormHelper.State {
        return when {
            useLink && userInput != null -> SavedPaymentMethodLinkFormHelper.State.Complete(userInput)
            useLink -> SavedPaymentMethodLinkFormHelper.State.Incomplete
            else -> SavedPaymentMethodLinkFormHelper.State.Unused
        }
    }

    private companion object {
        const val SPM_LINK_CHECKBOX_SELECTED_KEY = "STRIPE_SPM_LINK_CHECKBOX_SELECTED"
        const val SPM_LINK_INPUT_KEY = "STRIPE_SPM_LINK_INPUT"
    }
}
