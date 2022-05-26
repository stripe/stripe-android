package com.stripe.android.link

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.paymentmethod.PaymentMethodBody
import com.stripe.android.link.ui.signup.SignUpBody
import com.stripe.android.link.ui.verification.VerificationBodyFullFlow
import com.stripe.android.link.ui.wallet.WalletBody
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class LinkActivity : ComponentActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        LinkActivityViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { starterArgs }
        )

    private val viewModel: LinkActivityViewModel by viewModels { viewModelFactory }

    @VisibleForTesting
    lateinit var navController: NavHostController

    private val starterArgs by lazy {
        requireNotNull(LinkActivityContract.Args.fromIntent(intent))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO(brnunes-stripe): Migrate to androidx.compose.foundation 1.2.0 when out of beta
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            navController = rememberNavController()

            viewModel.navigator.navigationController = navController

            DefaultLinkTheme {
                Surface(Modifier.fillMaxHeight()) {
                    Column(Modifier.fillMaxWidth()) {
                        val linkAccount by viewModel.linkAccount.collectAsState(initial = null)

                        LinkAppBar(
                            email = linkAccount?.email,
                            onCloseButtonClick = { dismiss() }
                        )

                        NavHost(navController, LinkScreen.Loading.route) {

                            composable(LinkScreen.Loading.route) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            composable(
                                LinkScreen.SignUp.route,
                                arguments = listOf(
                                    navArgument(LinkScreen.SignUp.emailArg) {
                                        type = NavType.StringType
                                        nullable = true
                                    }
                                )
                            ) { backStackEntry ->
                                val email =
                                    backStackEntry.arguments?.getString(LinkScreen.SignUp.emailArg)
                                SignUpBody(viewModel.injector, email)
                            }

                            composable(LinkScreen.Verification.route) {
                                linkAccount?.let { account ->
                                    VerificationBodyFullFlow(
                                        account,
                                        viewModel.injector
                                    )
                                }
                            }

                            composable(LinkScreen.Wallet.route) {
                                linkAccount?.let { account ->
                                    WalletBody(
                                        account,
                                        viewModel.injector
                                    )
                                }
                            }

                            composable(LinkScreen.PaymentMethod.route) {
                                linkAccount?.let { account ->
                                    PaymentMethodBody(
                                        account,
                                        viewModel.injector
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.navigator.onDismiss = ::dismiss
        viewModel.setupPaymentLauncher(this)

        // Navigate to the initial screen once the view has been laid out.
        window.decorView.rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    lifecycleScope.launch {
                        viewModel.navigator.navigateTo(
                            target = when (viewModel.linkAccountManager.accountStatus.first()) {
                                AccountStatus.Verified ->
                                    LinkScreen.Wallet
                                AccountStatus.NeedsVerification,
                                AccountStatus.VerificationStarted ->
                                    LinkScreen.Verification
                                AccountStatus.SignedOut ->
                                    LinkScreen.SignUp(starterArgs.customerEmail)
                            },
                            clearBackStack = true
                        )
                    }
                    window.decorView.rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterFromActivity()
    }

    private fun dismiss(result: LinkActivityResult = LinkActivityResult.Canceled) {
        setResult(
            result.resultCode,
            Intent().putExtras(LinkActivityContract.Result(result).toBundle())
        )
        finish()
    }
}
