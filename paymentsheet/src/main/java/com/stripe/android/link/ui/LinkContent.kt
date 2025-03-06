package com.stripe.android.link.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.navigation.previousBackStackEntryAsState
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpScreen
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletScreen
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.ui.core.CircularProgressIndicator
import kotlinx.coroutines.launch

@SuppressWarnings("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun LinkContent(
    navController: NavHostController,
    appBarState: LinkAppBarState,
    sheetState: ModalBottomSheetState,
    bottomSheetContent: BottomSheetContent?,
    onUpdateSheetContent: (BottomSheetContent?) -> Unit,
    handleViewAction: (LinkAction) -> Unit,
    dismissWithResult: ((LinkActivityResult) -> Unit)?,
    getLinkAccount: () -> LinkAccount?,
    moveToWeb: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val previousBackStackEntry by navController.previousBackStackEntryAsState()

    DefaultLinkTheme {
        Surface {
            ModalBottomSheetLayout(
                sheetContent = bottomSheetContent ?: {
                    // Must have some content at startup or bottom sheet crashes when
                    // calculating its initial size
                    Box(Modifier.defaultMinSize(minHeight = 1.dp)) {}
                },
                modifier = Modifier.fillMaxHeight(),
                sheetState = sheetState,
                sheetShape = MaterialTheme.linkShapes.large.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                ),
                scrimColor = MaterialTheme.linkColors.sheetScrim
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BackHandler {
                        handleViewAction(LinkAction.BackPressed)
                    }

                    LinkAppBar(
                        state = appBarState,
                        canGoBack = previousBackStackEntry != null,
                        showBottomSheetContent = {
                            if (it == null) {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onUpdateSheetContent(null)
                                }
                            } else {
                                onUpdateSheetContent(it)
                            }
                        },
                        onLogoutClicked = {
                            handleViewAction(LinkAction.LogoutClicked)
                        }
                    )

                    Screens(
                        navController = navController,
                        moveToWeb = moveToWeb,
                        dismissWithResult = { result ->
                            dismissWithResult?.invoke(result)
                        },
                        getLinkAccount = getLinkAccount,
                        showBottomSheetContent = onUpdateSheetContent,
                        hideBottomSheetContent = {
                            onUpdateSheetContent(null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Screens(
    navController: NavHostController,
    getLinkAccount: () -> LinkAccount?,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit,
    hideBottomSheetContent: () -> Unit,
    moveToWeb: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        NavHost(
            modifier = Modifier
                .matchParentSize(),
            navController = navController,
            startDestination = LinkScreen.Loading.route,
        ) {
            composable(LinkScreen.Loading.route) {
                Loader()
            }

            composable(LinkScreen.SignUp.route) {
                SignUpRoute(moveToWeb = moveToWeb)
            }

            composable(LinkScreen.Verification.route) {
                val linkAccount = getLinkAccount()
                    ?: return@composable dismissWithResult(noLinkAccountResult())
                VerificationRoute(linkAccount = linkAccount)
            }

            composable(LinkScreen.Wallet.route) {
                val linkAccount = getLinkAccount()
                    ?: return@composable dismissWithResult(noLinkAccountResult())
                WalletRoute(
                    linkAccount = linkAccount,
                    showBottomSheetContent = showBottomSheetContent,
                    hideBottomSheetContent = hideBottomSheetContent,
                    dismissWithResult = dismissWithResult
                )
            }

            composable(LinkScreen.PaymentMethod.route) {
                val linkAccount = getLinkAccount()
                    ?: return@composable dismissWithResult(noLinkAccountResult())
                PaymentMethodRoute(linkAccount = linkAccount, dismissWithResult = dismissWithResult)
            }
        }
    }
}

@Composable
private fun SignUpRoute(moveToWeb: () -> Unit) {
    val viewModel: SignUpViewModel = linkViewModel { parentComponent ->
        SignUpViewModel.factory(
            parentComponent = parentComponent,
            moveToWeb = moveToWeb
        )
    }
    SignUpScreen(
        viewModel = viewModel,
    )
}

@Composable
private fun VerificationRoute(linkAccount: LinkAccount) {
    val viewModel: VerificationViewModel = linkViewModel { parentComponent ->
        VerificationViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            isDialog = false,
        )
    }
    VerificationScreen(viewModel)
}

@Composable
private fun PaymentMethodRoute(
    linkAccount: LinkAccount,
    dismissWithResult: (LinkActivityResult) -> Unit,
) {
    val viewModel: PaymentMethodViewModel = linkViewModel { parentComponent ->
        PaymentMethodViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            dismissWithResult = dismissWithResult
        )
    }
    PaymentMethodScreen(viewModel)
}

@Composable
private fun WalletRoute(
    linkAccount: LinkAccount,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit,
    hideBottomSheetContent: () -> Unit,
) {
    val viewModel: WalletViewModel = linkViewModel { parentComponent ->
        WalletViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            dismissWithResult = dismissWithResult
        )
    }
    WalletScreen(
        viewModel = viewModel,
        showBottomSheetContent = showBottomSheetContent,
        hideBottomSheetContent = hideBottomSheetContent
    )
}

@Composable
private fun Loader() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private fun noLinkAccountResult(): LinkActivityResult {
    return LinkActivityResult.Failed(
        error = NoLinkAccountFoundException(),
        linkAccountUpdate = LinkAccountUpdate.None
    )
}
