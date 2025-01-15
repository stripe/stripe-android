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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.cardedit.CardEditScreen
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
    viewModel: LinkActivityViewModel,
    navController: NavHostController,
    appBarState: LinkAppBarState,
    sheetState: ModalBottomSheetState,
    bottomSheetContent: BottomSheetContent?,
    onUpdateSheetContent: (BottomSheetContent?) -> Unit,
    onBackPressed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    DefaultLinkTheme {
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
                    viewModel.handleViewAction(LinkAction.BackPressed)
                }

                LinkAppBar(
                    state = appBarState,
                    onBackPressed = onBackPressed,
                    showBottomSheetContent = {
                        if (it == null) {
                            coroutineScope.launch {
                                sheetState.hide()
                                onUpdateSheetContent(null)
                            }
                        } else {
                            onUpdateSheetContent(it)
                        }
                    }
                )

                Screens(
                    navController = navController,
                    goBack = viewModel::goBack,
                    navigate = { screen ->
                        viewModel.navigate(screen, clearStack = false)
                    },
                    navigateAndClearStack = { screen ->
                        viewModel.navigate(screen, clearStack = true)
                    },
                    dismissWithResult = { result ->
                        viewModel.dismissWithResult?.invoke(result)
                    },
                    getLinkAccount = {
                        viewModel.linkAccount
                    },
                    showBottomSheetContent = onUpdateSheetContent,
                    hideBottomSheetContent = {
                        onUpdateSheetContent(null)
                    }
                )
            }
        }
    }
}

@Composable
private fun Screens(
    navController: NavHostController,
    getLinkAccount: () -> LinkAccount?,
    goBack: () -> Unit,
    navigate: (route: LinkScreen) -> Unit,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = LinkScreen.Loading.route
    ) {
        composable(LinkScreen.Loading.route) {
            Loader()
        }

        composable(LinkScreen.SignUp.route) {
            val viewModel: SignUpViewModel = linkViewModel {
                SignUpViewModel.factory(it)
            }
            SignUpScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(LinkScreen.Verification.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(LinkActivityResult.Failed(NoLinkAccountFoundException()))
            val viewModel: VerificationViewModel = linkViewModel { parentComponent ->
                VerificationViewModel.factory(
                    parentComponent = parentComponent,
                    goBack = goBack,
                    navigateAndClearStack = navigateAndClearStack,
                    linkAccount = linkAccount
                )
            }
            VerificationScreen(viewModel)
        }

        composable(LinkScreen.Wallet.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(LinkActivityResult.Failed(NoLinkAccountFoundException()))
            val viewModel: WalletViewModel = linkViewModel { parentComponent ->
                WalletViewModel.factory(
                    parentComponent = parentComponent,
                    linkAccount = linkAccount,
                    navigate = navigate,
                    navigateAndClearStack = navigateAndClearStack,
                    dismissWithResult = dismissWithResult
                )
            }
            WalletScreen(
                viewModel = viewModel,
                showBottomSheetContent = showBottomSheetContent,
                hideBottomSheetContent = hideBottomSheetContent
            )
        }

        composable(LinkScreen.CardEdit.route) {
            CardEditScreen()
        }

        composable(LinkScreen.PaymentMethod.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(LinkActivityResult.Failed(NoLinkAccountFoundException()))
            PaymentMethodRoute(linkAccount = linkAccount, dismissWithResult = dismissWithResult)
        }
    }
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
private fun Loader() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
