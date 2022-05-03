package com.stripe.android.link.ui.verification

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.LinkAppBar

@Composable
fun LinkVerificationDialog(
    linkLauncher: LinkPaymentLauncher,
    onDialogDismissed: () -> Unit,
    onVerificationCompleted: () -> Unit
) {
    val injector = requireNotNull(linkLauncher.injector)
    val openDialog = remember { mutableStateOf(true) }
    val linkAccount = linkLauncher.linkAccountManager.linkAccount.collectAsState()

    linkAccount.value?.let { account ->
        if (openDialog.value) {
            Dialog(
                onDismissRequest = {
                    openDialog.value = false
                    onDialogDismissed()
                }
            ) {
                DefaultLinkTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.linkColors.disabledText,
                                shape = MaterialTheme.shapes.medium
                            ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            LinkAppBar(
                                email = account.email,
                                onCloseButtonClick = {
                                    openDialog.value = false
                                    onDialogDismissed()
                                }
                            )
                            VerificationBody(
                                headerStringResId = R.string.verification_header_prefilled,
                                messageStringResId = R.string.verification_message,
                                showChangeEmailMessage = false,
                                linkAccount = account,
                                injector = injector,
                                onVerificationCompleted = onVerificationCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}
