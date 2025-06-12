package com.stripe.android.lpmfoundations.paymentmethod.link

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkElement
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class LinkFormElement(
    private val signupMode: LinkSignupMode,
    private val configuration: LinkConfiguration,
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
        val modifier = Modifier.run {
            if (StripeTheme.customSectionSpacing == null) {
                padding(vertical = 6.dp)
            } else {
                this
            }
        }

        LinkElement(
            initialUserInput = initialLinkUserInput,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkSignupMode = signupMode,
            configuration = configuration,
            enabled = enabled,
            onLinkSignupStateChanged = onLinkInlineSignupStateChanged,
            modifier = modifier,
        )
    }
}
