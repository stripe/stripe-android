package com.stripe.android.link

import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.verification.VerificationViewModel

@Composable
internal fun EagerPath(
    linkAccount: LinkAccount?,
    viewModel: LinkActivityViewModel,
) {
    VerificationDialog(
        linkAccount = linkAccount,
        navigateAndClearStack = { screen ->
            viewModel.navigate(screen, clearStack = true)
        },
        goBack = {
            viewModel.goBack()
        }
    )
}

@Composable
private fun VerificationDialog(
    linkAccount: LinkAccount?,
    navigateAndClearStack: (LinkScreen) -> Unit,
    goBack: () -> Unit
) {
    val viewModel: VerificationViewModel = linkViewModel { parentComponent ->
        VerificationViewModel.factory(
            parentComponent = parentComponent,
            goBack = goBack,
            navigateAndClearStack = navigateAndClearStack,
            linkAccount = linkAccount
        )
    }
    Dialog (onDismissRequest = {}) {
        DefaultLinkTheme {
            Card {
                VerificationScreen(viewModel)
            }
        }
    }
}