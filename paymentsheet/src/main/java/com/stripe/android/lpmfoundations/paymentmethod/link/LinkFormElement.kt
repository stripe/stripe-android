package com.stripe.android.lpmfoundations.paymentmethod.link

import androidx.compose.runtime.Composable
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkElement
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class LinkFormElement(
    private val configuration: LinkInlineConfiguration,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val initialLinkUserInput: UserInput?,
    private val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
) : RenderableFormElement(
    allowsUserInteraction = true,
    identifier = IdentifierSpec.Generic("link_form")
) {
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return stateFlowOf(listOf())
    }

    @Composable
    override fun ComposeUI(enabled: Boolean) {
        LinkElement(
            initialUserInput = initialLinkUserInput,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkSignupMode = configuration.signupMode,
            configuration = configuration.linkConfiguration,
            enabled = enabled,
            onLinkSignupStateChanged = onLinkInlineSignupStateChanged,
        )
    }
}
