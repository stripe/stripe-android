package com.stripe.android.link.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.LinkScreen.Companion.EXTRA_PAYMENT_DETAILS
import com.stripe.android.link.LinkScreen.Companion.billingDetailsUpdateFlow
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.NoPaymentDetailsFoundException
import com.stripe.android.link.linkViewModel
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkAppearanceTheme
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
import kotlinx.coroutines.launch

@SuppressWarnings("LongMethod")
@Composable
internal fun LinkContent(
    modifier: Modifier,
    navController: NavHostController,
    appBarState: LinkAppBarState,
    appearance: LinkAppearance?,
    bottomSheetContent: BottomSheetContent?,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    handleViewAction: (LinkAction) -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    getLinkAccount: () -> LinkAccount?,
    onBackPressed: () -> Unit,
    moveToWeb: (Throwable) -> Unit,
    goBack: () -> Unit,
    changeEmail: () -> Unit,
    initialDestination: LinkScreen
) {
    val coroutineScope = rememberCoroutineScope()

    DefaultLinkTheme {
        Surface(
            modifier = modifier,
            color = LinkTheme.colors.surfacePrimary,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BackHandler {
                    if (bottomSheetContent != null) {
                        coroutineScope.launch {
                            hideBottomSheetContent()
                        }
                    } else if (navController.popBackStack().not()) {
                        handleViewAction(LinkAction.BackPressed)
                    }
                }

                LinkAppBar(
                    state = appBarState,
                    onBackPressed = onBackPressed,
                )

                Screens(
                    initialDestination = initialDestination,
                    navController = navController,
                    appearance = appearance,
                    goBack = goBack,
                    moveToWeb = moveToWeb,
                    navigateAndClearStack = { screen ->
                        navigate(screen, true)
                    },
                    dismissWithResult = dismissWithResult,
                    getLinkAccount = getLinkAccount,
                    showBottomSheetContent = showBottomSheetContent,
                    changeEmail = changeEmail,
                    hideBottomSheetContent = hideBottomSheetContent,
                    onLogoutClicked = {
                        coroutineScope.launch {
                            hideBottomSheetContent()
                            handleViewAction(LinkAction.LogoutClicked)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Screens(
    navController: NavHostController,
    getLinkAccount: () -> LinkAccount?,
    appearance: LinkAppearance?,
    goBack: () -> Unit,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    moveToWeb: (Throwable) -> Unit,
    changeEmail: () -> Unit,
    initialDestination: LinkScreen,
    onLogoutClicked: () -> Unit,
) {
    LinkNavHost(
        navController = navController,
        startDestination = initialDestination.route,
    ) {
        composable(LinkScreen.Loading.route) {
            LinkLoadingScreen()
        }

        composable(LinkScreen.SignUp.route) {
            // Keep height fixed to reduce animations caused by IME toggling on both
            // this screen and Verification screen.
            MinScreenHeightBox(screenHeightPercentage = 1f) {
                SignUpRoute(
                    navigateAndClearStack = navigateAndClearStack,
                    moveToWeb = moveToWeb,
                    dismissWithResult = dismissWithResult
                )
            }
        }

        composable(LinkScreen.UpdateCard.route) { backStackEntry ->
            val paymentDetailsId = backStackEntry
                .arguments?.getString(EXTRA_PAYMENT_DETAILS)
                ?: return@composable dismissWithResult(noPaymentDetailsResult())
            UpdateCardRoute(
                paymentDetailsId = paymentDetailsId,
                appearance = appearance,
                billingDetailsUpdateFlow = backStackEntry.billingDetailsUpdateFlow(),
                dismissWithResult = dismissWithResult
            )
        }

        composable(LinkScreen.Verification.route) {
            // Keep height fixed to reduce animations caused by IME toggling on both
            // this screen and SignUp screen.
            val linkAccount = getLinkAccount() ?: return@composable dismissWithResult(noLinkAccountResult())
            MinScreenHeightBox(screenHeightPercentage = if (initialDestination == LinkScreen.SignUp) 1f else 0f) {
                VerificationRoute(
                    linkAccount = linkAccount,
                    changeEmail = changeEmail,
                    navigateAndClearStack = navigateAndClearStack,
                    goBack = goBack,
                    dismissWithResult = dismissWithResult
                )
            }
        }

        composable(LinkScreen.Wallet.route) {
            val linkAccount = getLinkAccount() ?: return@composable dismissWithResult(noLinkAccountResult())
            WalletRoute(
                linkAccount = linkAccount,
                appearance = appearance,
                navigateAndClearStack = navigateAndClearStack,
                showBottomSheetContent = showBottomSheetContent,
                hideBottomSheetContent = hideBottomSheetContent,
                dismissWithResult = dismissWithResult,
                onLogoutClicked = onLogoutClicked,
            )
        }

        composable(LinkScreen.PaymentMethod.route) {
            val linkAccount = getLinkAccount() ?: return@composable dismissWithResult(noLinkAccountResult())
            PaymentMethodRoute(
                linkAccount = linkAccount,
                appearance = appearance,
                dismissWithResult = dismissWithResult,
            )
        }
    }
}

@Composable
private fun SignUpRoute(
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    moveToWeb: (Throwable) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit
) {
    val viewModel: SignUpViewModel = linkViewModel { parentComponent ->
        SignUpViewModel.factory(
            parentComponent = parentComponent,
            navigateAndClearStack = navigateAndClearStack,
            moveToWeb = moveToWeb,
            dismissWithResult = dismissWithResult
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
    goBack: () -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit
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
            onChangeEmailClicked = changeEmail,
            dismissWithResult = dismissWithResult
        )
    }
    VerificationScreen(viewModel)
}

@Composable
private fun UpdateCardRoute(
    paymentDetailsId: String,
    appearance: LinkAppearance?,
    billingDetailsUpdateFlow: BillingDetailsUpdateFlow?,
    dismissWithResult: (LinkActivityResult) -> Unit
) {
    val viewModel: UpdateCardScreenViewModel = linkViewModel { parentComponent ->
        UpdateCardScreenViewModel.factory(
            parentComponent = parentComponent,
            paymentDetailsId = paymentDetailsId,
            billingDetailsUpdateFlow = billingDetailsUpdateFlow,
            dismissWithResult = dismissWithResult
        )
    }
    UpdateCardScreen(
        viewModel = viewModel,
        appearance = appearance
    )
}

@Composable
private fun PaymentMethodRoute(
    linkAccount: LinkAccount,
    appearance: LinkAppearance?,
    dismissWithResult: (LinkActivityResult) -> Unit,
) {
    val viewModel: PaymentMethodViewModel = linkViewModel { parentComponent ->
        PaymentMethodViewModel.factory(
            parentComponent = parentComponent,
            linkAccount = linkAccount,
            dismissWithResult = dismissWithResult
        )
    }
    PaymentMethodScreen(
        appearance = appearance,
        viewModel = viewModel,
    )
}

@Composable
private fun WalletRoute(
    linkAccount: LinkAccount,
    appearance: LinkAppearance?,
    navigateAndClearStack: (route: LinkScreen) -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    onLogoutClicked: () -> Unit,
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
        appearance = appearance,
        showBottomSheetContent = showBottomSheetContent,
        hideBottomSheetContent = hideBottomSheetContent,
        onLogoutClicked = onLogoutClicked,
    )
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
