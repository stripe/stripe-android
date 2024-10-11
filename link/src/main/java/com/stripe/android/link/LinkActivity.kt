package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.ui.cardedit.CardEditScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.signup.SignUpScreen
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.wallet.WalletScreen

class LinkActivity : AppCompatActivity() {
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
                composable(LinkScreen.Loading.route) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                composable(LinkScreen.SignUp.route) {
                    val viewModel = viewModel<SignUpViewModel>()
                    SignUpScreen(
                        signUpViewModel = viewModel
                    )
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
