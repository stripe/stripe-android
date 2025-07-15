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
import com.stripe.android.uicore.LocalSectionSpacing
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _viewState = MutableStateFlow<InlineSignupViewState?>(null)

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        _viewState.mapAsStateFlow { viewState ->
            val isComplete = viewState?.isFormValidForSubmission ?: true
            listOf(identifier to FormFieldEntry(value = null, isComplete = isComplete))
        }

    @Composable
    override fun ComposeUI(enabled: Boolean) {
        val modifier = Modifier.run {
            if (LocalSectionSpacing.current == null) {
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
            onLinkSignupStateChanged = { viewState ->
                _viewState.value = viewState
                onLinkInlineSignupStateChanged(viewState)
            },
            modifier = modifier,
        )
    }
}
