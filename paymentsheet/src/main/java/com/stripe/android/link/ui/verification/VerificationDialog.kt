package com.stripe.android.link.ui.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme

@Composable
internal fun VerificationDialog(
    modifier: Modifier,
    linkAccount: LinkAccount,
) {
    val viewModel = linkViewModel<VerificationViewModel> { parentComponent ->
        VerificationViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            isDialog = true,
        )
    }

    VerificationDialogBody(
        modifier = modifier,
        viewModel = viewModel
    )
}

@Composable
internal fun VerificationDialogBody(
    modifier: Modifier = Modifier,
    viewModel: VerificationViewModel
) {
    Box(
        modifier = modifier
    ) {
        Dialog(
            onDismissRequest = {
                viewModel.onBack()
            }
        ) {
            DefaultLinkTheme {
                Surface(shape = RoundedCornerShape(16.dp)) {
                    VerificationScreen(viewModel)
                }
            }
        }
    }
}
