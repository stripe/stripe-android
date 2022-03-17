package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.signup.SignUpBody
import com.stripe.android.link.ui.verification.VerificationBody
import com.stripe.android.link.ui.wallet.WalletBody

internal class LinkActivity : ComponentActivity() {

    private val viewModel: LinkActivityViewModel by viewModels {
        LinkActivityViewModel.Factory(application) { requireNotNull(starterArgs) }
    }

    @VisibleForTesting
    lateinit var navController: NavHostController

    private val starterArgs: LinkActivityContract.Args? by lazy {
        LinkActivityContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            navController = rememberNavController()

            viewModel.navigator.navigationController = navController

            DefaultLinkTheme {
                Surface {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        val linkAccount by viewModel.linkAccount.collectAsState(initial = null)

                        LinkAppBar(
                            email = linkAccount?.email,
                            onCloseButtonClick = ::dismiss
                        )

                        NavHost(navController, viewModel.startDestination) {
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
                                    VerificationBody(
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "<Placholder>\nAdd new payment method")
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.navigator.onDismiss = ::dismiss
    }

    private fun dismiss() {
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtras(LinkActivityContract.Result(LinkActivityResult.Canceled).toBundle())
        )
        finish()
    }
}
