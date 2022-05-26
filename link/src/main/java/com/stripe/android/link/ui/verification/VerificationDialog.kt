package com.stripe.android.link.ui.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkAppBar

/**
 * Function called when the Link verification dialog has been dismissed. The boolean returned
 * indicates whether the verification succeeded.
 * When called, [LinkPaymentLauncher.accountStatus] will contain the up to date account status.
 */
typealias LinkVerificationCallback = (success: Boolean) -> Unit

@Composable
fun LinkVerificationDialog(
    linkLauncher: LinkPaymentLauncher,
    verificationCallback: LinkVerificationCallback
) {
    // Must be inside a NavController so that the VerificationViewModel scope is destroyed when the
    // dialog is dismissed, and when called again a new scope is created.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dialog"
    ) {
        composable("dialog") {
            var openDialog by remember { mutableStateOf(true) }

            val injector = requireNotNull(linkLauncher.injector)
            val linkAccount = linkLauncher.linkAccountManager.linkAccount.collectAsState()

            linkAccount.value?.let { account ->
                if (openDialog) {
                    Dialog(
                        onDismissRequest = {
                            openDialog = false
                            verificationCallback(false)
                        },
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                    ) {
                        DefaultLinkTheme {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column {
                                    LinkAppBar(
                                        email = account.email,
                                        onCloseButtonClick = {
                                            openDialog = false
                                            verificationCallback(false)
                                        }
                                    )
                                    VerificationBody(
                                        headerStringResId = R.string.verification_header_prefilled,
                                        messageStringResId = R.string.verification_message,
                                        showChangeEmailMessage = false,
                                        linkAccount = account,
                                        injector = injector,
                                        onVerificationCompleted = {
                                            openDialog = false
                                            verificationCallback(true)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
