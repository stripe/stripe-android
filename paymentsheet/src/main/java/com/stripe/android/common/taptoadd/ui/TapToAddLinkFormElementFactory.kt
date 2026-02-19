package com.stripe.android.common.taptoadd.ui

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkFormElement
import com.stripe.android.uicore.elements.FormElement

internal interface TapToAddLinkFormElementFactory {
    fun create(
        signupMode: LinkSignupMode,
        configuration: LinkConfiguration,
        linkConfigurationCoordinator: LinkConfigurationCoordinator,
        userInput: UserInput?,
        onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
        previousLinkSignupCheckboxSelection: Boolean,
    ): FormElement
}

internal object DefaultTapToAddLinkFormElementFactory : TapToAddLinkFormElementFactory {
    override fun create(
        signupMode: LinkSignupMode,
        configuration: LinkConfiguration,
        linkConfigurationCoordinator: LinkConfigurationCoordinator,
        userInput: UserInput?,
        onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
        previousLinkSignupCheckboxSelection: Boolean,
    ): FormElement {
        return LinkFormElement(
            signupMode = signupMode,
            configuration = configuration,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            initialLinkUserInput = userInput,
            onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
            previousLinkSignupCheckboxSelection = previousLinkSignupCheckboxSelection,
        )
    }
}
