package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.ui.cardedit.CardEditScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.signup.SignUpScreen
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.wallet.WalletScreen

internal class LinkActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = LinkActivityViewModel.Factory()
    private val viewModel: LinkActivityViewModel by viewModels { viewModelFactory }

    @VisibleForTesting
    internal lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            navController = rememberNavController()

            LaunchedEffect(Unit) {
                viewModel.navController = navController
                viewModel.dismissWithResult = ::dismissWithResult
            }

            NavHost(
                navController = navController,
                startDestination = LinkScreen.SignUp.route
            ) {
                composable(LinkScreen.SignUp.route) {
                    SignUpScreen()
                }

                composable(LinkScreen.Verification.route) {
                    VerificationScreen()
                }

                composable(LinkScreen.Wallet.route) {
                    WalletScreen()
                }

                composable(LinkScreen.CardEdit.route) {
                    CardEditScreen()
                }

                composable(LinkScreen.PaymentMethod.route) {
                    PaymentMethodScreen()
                }
            }
        }
    }

    private fun dismissWithResult(result: LinkActivityResult) {
        val bundle = bundleOf(
            LinkActivityContract.EXTRA_RESULT to LinkActivityContract.Result(result)
        )
        this@LinkActivity.setResult(
            Activity.RESULT_OK,
            Intent().putExtras(bundle)
        )
        this@LinkActivity.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterActivity()
    }
}
