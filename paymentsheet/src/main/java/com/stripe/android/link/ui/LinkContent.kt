package com.stripe.android.link.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stripe.android.common.ui.AnimatedConstraints
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.LinkScreen.Companion.EXTRA_PAYMENT_DETAILS
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.NoPaymentDetailsFoundException
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpScreen
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.updatecard.UpdateCardScreen
import com.stripe.android.link.ui.updatecard.UpdateCardScreenViewModel
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletScreen
import com.stripe.android.link.ui.wallet.WalletViewModel

@SuppressWarnings("LongMethod")
@Composable
internal fun LinkContent(
    modifier: Modifier,
    navController: NavHostController,
    appBarState: LinkAppBarState,
    bottomSheetContent: BottomSheetContent?,
    onUpdateSheetContent: (BottomSheetContent?) -> Unit,
    handleViewAction: (LinkAction) -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    getLinkAccount: () -> LinkAccount?,
    onBackPressed: () -> Unit,
    moveToWeb: () -> Unit,
    goBack: () -> Unit,
    changeEmail: () -> Unit,
    initialDestination: LinkScreen
) {
    DefaultLinkTheme {
        Surface(
            modifier = modifier,
            color = LinkTheme.colors.surfacePrimary,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                BackHandler {
                    if (bottomSheetContent != null) {
                        onUpdateSheetContent(null)
                    } else if (navController.popBackStack().not()) {
                        handleViewAction(LinkAction.BackPressed)
                    }
                }

                LinkAppBar(
                    state = appBarState,
                    onBackPressed = onBackPressed,
                    showBottomSheetContent = onUpdateSheetContent,
                    onLogoutClicked = {
                        handleViewAction(LinkAction.LogoutClicked)
                    }
                )

                AnimatedConstraints {
                    Screens(
                        initialDestination = initialDestination,
                        navController = navController,
                        goBack = goBack,
                        moveToWeb = moveToWeb,
                        navigateAndClearStack = { screen ->
                            navigate(screen, true)
                        },
                        dismissWithResult = dismissWithResult,
                        getLinkAccount = getLinkAccount,
                        showBottomSheetContent = onUpdateSheetContent,
                        changeEmail = changeEmail,
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
    goBack: () -> Unit,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit,
    hideBottomSheetContent: () -> Unit,
    moveToWeb: () -> Unit,
    changeEmail: () -> Unit,
    initialDestination: LinkScreen,
) {
    NavHost(
        modifier = Modifier.wrapContentHeight(),
        navController = navController,
        startDestination = initialDestination.route,
    ) {
        composable(LinkScreen.Loading.route) {
            Loader()
        }

        composable(LinkScreen.SignUp.route) {
            SignUpRoute(
                navigateAndClearStack = navigateAndClearStack,
                moveToWeb = moveToWeb
            )
        }

        composable(
            LinkScreen.UpdateCard.route
        ) { backStackEntry ->
            val paymentDetailsId = backStackEntry.arguments?.getString(EXTRA_PAYMENT_DETAILS)
                ?: return@composable dismissWithResult(noPaymentDetailsResult())
            UpdateCardRoute(
                paymentDetailsId = paymentDetailsId
            )
        }

        composable(LinkScreen.Verification.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(noLinkAccountResult())
            VerificationRoute(
                linkAccount = linkAccount,
                changeEmail = changeEmail,
                navigateAndClearStack = navigateAndClearStack,
                goBack = goBack
            )
        }

        composable(LinkScreen.Wallet.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(noLinkAccountResult())
            WalletRoute(
                linkAccount = linkAccount,
                navigateAndClearStack = navigateAndClearStack,
                showBottomSheetContent = showBottomSheetContent,
                hideBottomSheetContent = hideBottomSheetContent,
                dismissWithResult = dismissWithResult
            )
        }

        composable(LinkScreen.PaymentMethod.route) {
            val linkAccount = getLinkAccount()
                ?: return@composable dismissWithResult(noLinkAccountResult())
            PaymentMethodRoute(
                linkAccount = linkAccount,
                dismissWithResult = dismissWithResult,
                goBack = goBack
            )
        }
    }
}

@Composable
private fun SignUpRoute(
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    moveToWeb: () -> Unit
) {
    val viewModel: SignUpViewModel = linkViewModel { parentComponent ->
        SignUpViewModel.factory(
            parentComponent = parentComponent,
            navigateAndClearStack = navigateAndClearStack,
            moveToWeb = moveToWeb
        )
    }
    SignUpScreen(
        viewModel = viewModel,
    )
}

@Composable
private fun VerificationRoute(
    linkAccount: LinkAccount,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    changeEmail: () -> Unit,
    goBack: () -> Unit
) {
    val viewModel: VerificationViewModel = linkViewModel { parentComponent ->
        VerificationViewModel.factory(
            parentComponent = parentComponent,
            onDismissClicked = goBack,
            linkAccount = linkAccount,
            isDialog = false,
            onVerificationSucceeded = {
                navigateAndClearStack(LinkScreen.Wallet)
            },
            onChangeEmailClicked = changeEmail
        )
    }
    VerificationScreen(viewModel)
}

@Composable
private fun UpdateCardRoute(paymentDetailsId: String) {
    val viewModel: UpdateCardScreenViewModel = linkViewModel { parentComponent ->
        UpdateCardScreenViewModel.factory(
            parentComponent = parentComponent,
            paymentDetailsId = paymentDetailsId,
        )
    }
    UpdateCardScreen(
        viewModel = viewModel,
    )
}

@Composable
private fun PaymentMethodRoute(
    linkAccount: LinkAccount,
    dismissWithResult: (LinkActivityResult) -> Unit,
    goBack: () -> Unit
) {
    val viewModel: PaymentMethodViewModel = linkViewModel { parentComponent ->
        PaymentMethodViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            dismissWithResult = dismissWithResult
        )
    }
    PaymentMethodScreen(
        viewModel = viewModel,
        onCancelClicked = goBack,
    )
}

@Composable
private fun WalletRoute(
    linkAccount: LinkAccount,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit,
    hideBottomSheetContent: () -> Unit,
) {
    val viewModel: WalletViewModel = linkViewModel { parentComponent ->
        WalletViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
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

@Composable
internal fun Loader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        LinkSpinner(
            modifier = Modifier
                .size(48.dp)
        )
    }
}

@Preview
@Composable
private fun LoaderPreview() {
    DefaultLinkTheme {
        Loader()
    }
}

private fun noLinkAccountResult(): LinkActivityResult {
    return LinkActivityResult.Failed(
        error = NoLinkAccountFoundException(),
        linkAccountUpdate = LinkAccountUpdate.None
    )
}

private fun noPaymentDetailsResult(): LinkActivityResult {
    return LinkActivityResult.Failed(
        error = NoPaymentDetailsFoundException(),
        linkAccountUpdate = LinkAccountUpdate.None
    )
}
