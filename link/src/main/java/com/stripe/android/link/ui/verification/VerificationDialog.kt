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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.rememberLinkAppBarState

/**
 * Function called when the Link verification dialog has been dismissed. The boolean returned
 * indicates whether the verification succeeded.
 * When called, [LinkPaymentLauncher.accountStatus] will contain the up to date account status.
 */
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "This interface isn't meant for public consumption.",
)
typealias LinkVerificationCallback = (success: Boolean) -> Unit

@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkVerificationDialog(
    linkLauncher: LinkPaymentLauncher,
    onResult: (Boolean) -> Unit,
) {
    // Must be inside a NavController so that the VerificationViewModel scope is destroyed when the
    // dialog is dismissed, and when called again a new scope is created.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LinkScreen.VerificationDialog.route
    ) {
        composable(LinkScreen.VerificationDialog.route) {
            var openDialog by remember { mutableStateOf(true) }

            val component = requireNotNull(linkLauncher.component)
            val linkAccount = component.linkAccountManager.linkAccount.collectAsState()
            val linkEventsReporter = component.linkEventsReporter

            val onDismiss = {
                openDialog = false
                linkEventsReporter.on2FACancel()
                onResult(false)
            }

            val backStackEntry by navController.currentBackStackEntryAsState()

            linkAccount.value?.let { account ->
                if (openDialog) {
                    Dialog(
                        onDismissRequest = onDismiss,
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        DefaultLinkTheme {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = MaterialTheme.linkShapes.medium
                            ) {
                                Column {
                                    val appBarState = rememberLinkAppBarState(
                                        isRootScreen = true,
                                        currentRoute = backStackEntry?.destination?.route,
                                        email = account.email,
                                        accountStatus = account.accountStatus
                                    )

                                    LinkAppBar(
                                        state = appBarState,
                                        onBackPressed = onDismiss,
                                        onLogout = {
                                            // This can't be invoked from the verification dialog
                                        },
                                        showBottomSheetContent = {
                                            // This can't be invoked from the verification dialog
                                        }
                                    )

                                    VerificationBody(
                                        headerStringResId = R.string.stripe_verification_header_prefilled,
                                        messageStringResId = R.string.stripe_verification_message,
                                        showChangeEmailMessage = false,
                                        linkAccount = account,
                                        injector = component.injector,
                                        onVerificationCompleted = {
                                            openDialog = false
                                            onResult(true)
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
