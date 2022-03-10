package com.stripe.android.link

import android.os.Bundle
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
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.signup.SignUpBody
import com.stripe.android.link.ui.verification.VerificationBody
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

            DefaultLinkTheme {
                Surface {
                    Column(Modifier.fillMaxWidth()) {
                        val linkAccount by viewModel.linkAccount.collectAsState(initial = null)

                        LinkAppBar()

                        NavHost(navController, viewModel.startDestination.route) {
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
                            composable(LinkScreen.SignUp.route) {
                                SignUpBody(viewModel.injector)
                            }
                            composable(LinkScreen.Verification.route) {
                                VerificationBody(
                                    requireNotNull(linkAccount),
                                    viewModel.injector
                                )
                            }
                            composable(LinkScreen.Wallet.route) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Wallet Placeholder")
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.navigator.sharedFlow.onEach {
            navController.popBackStack()
            navController.navigate(it.route)
        }.launchIn(viewModel.viewModelScope)
    }
}
