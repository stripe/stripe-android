@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import java.util.UUID

@Composable
internal fun LinkElement(
    initialUserInput: UserInput?,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    configuration: LinkConfiguration,
    linkSignupMode: LinkSignupMode,
    enabled: Boolean,
    onLinkSignupStateChanged: (InlineSignupViewState) -> Unit,
) {
    val component = remember(linkConfigurationCoordinator, configuration) {
        linkConfigurationCoordinator.getComponent(configuration)
    }

    val uuid = rememberSaveable(linkConfigurationCoordinator, configuration) {
        UUID.randomUUID().toString()
    }

    val viewModel: InlineSignupViewModel = viewModel(
        key = uuid,
        factory = InlineSignupViewModel.Factory(
            initialUserInput = initialUserInput,
            signupMode = linkSignupMode,
            linkComponent = component,
        )
    )

    when (viewModel.signupMode) {
        LinkSignupMode.InsteadOfSaveForFutureUse -> {
            LinkInlineSignup(
                viewModel = viewModel,
                enabled = enabled,
                onStateChanged = onLinkSignupStateChanged,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth(),
            )
        }
        LinkSignupMode.AlongsideSaveForFutureUse -> {
            LinkOptionalInlineSignup(
                viewModel = viewModel,
                enabled = enabled,
                onStateChanged = onLinkSignupStateChanged,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
