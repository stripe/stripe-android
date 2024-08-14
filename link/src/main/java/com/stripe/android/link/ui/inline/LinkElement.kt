@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkConfigurationCoordinator

@Composable
fun LinkElement(
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    linkSignupMode: LinkSignupMode?,
    enabled: Boolean,
    horizontalPadding: Dp,
    onLinkSignupStateChanged: (InlineSignupViewState) -> Unit,
) {
    val component = linkConfigurationCoordinator?.component

    if (component != null && linkSignupMode != null) {
        val viewModel: InlineSignupViewModel = viewModel(
            factory = InlineSignupViewModel.Factory(
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
                        .padding(horizontal = horizontalPadding, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
            LinkSignupMode.AlongsideSaveForFutureUse -> {
                LinkOptionalInlineSignup(
                    viewModel = viewModel,
                    enabled = enabled,
                    onStateChanged = onLinkSignupStateChanged,
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
