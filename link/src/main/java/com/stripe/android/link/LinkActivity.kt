package com.stripe.android.link

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.isOnRootScreen
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.cardedit.CardEditBody
import com.stripe.android.link.ui.paymentmethod.PaymentMethodBody
import com.stripe.android.link.ui.rememberLinkAppBarState
import com.stripe.android.link.ui.signup.SignUpBody
import com.stripe.android.link.ui.verification.VerificationBodyFullFlow
import com.stripe.android.link.ui.wallet.WalletBody
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
internal class LinkActivity : ComponentActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = LinkActivityViewModel.Factory {
        starterArgs
    }

    private val viewModel: LinkActivityViewModel by viewModels { viewModelFactory }

    @VisibleForTesting
    lateinit var navController: NavHostController

    private val starterArgs by lazy {
        requireNotNull(LinkActivityContract.Args.fromIntent(intent))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.stripe_slide_up, 0)

        setContent {
            var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
            val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            val coroutineScope = rememberCoroutineScope()

            if (bottomSheetContent != null) {
                DisposableEffect(bottomSheetContent) {
                    coroutineScope.launch { sheetState.show() }
                    onDispose {
                        coroutineScope.launch { sheetState.hide() }
                    }
                }
            }

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
                    navController = rememberNavController()

                    viewModel.navigator.navigationController = navController

                    Column(Modifier.fillMaxWidth()) {
                        val linkAccount by viewModel.linkAccount.collectAsState(null)
                        val isOnRootScreen by isRootScreenFlow().collectAsState(true)
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val appBarState = rememberLinkAppBarState(
                            isRootScreen = isOnRootScreen,
                            currentRoute = backStackEntry?.destination?.route,
                            email = linkAccount?.email,
                            accountStatus = linkAccount?.accountStatus
                        )

                        BackHandler(onBack = viewModel::onBackPressed)

                        LinkAppBar(
                            state = appBarState,
                            onBackPressed = onBackPressedDispatcher::onBackPressed,
                            onLogout = viewModel::logout,
                            showBottomSheetContent = {
                                if (it == null) {
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        bottomSheetContent = null
                                    }
                                } else {
                                    bottomSheetContent = it
                                }
                            }
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

                            composable(LinkScreen.SignUp.route) {
                                SignUpBody(viewModel.injector)
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
                                    ) {
                                        if (it == null) {
                                            coroutineScope.launch {
                                                sheetState.hide()
                                                bottomSheetContent = null
                                            }
                                        } else {
                                            bottomSheetContent = it
                                        }
                                    }
                                }
                            }

                            composable(
                                LinkScreen.PaymentMethod.route,
                                arguments = listOf(
                                    navArgument(LinkScreen.PaymentMethod.loadArg) {
                                        type = NavType.BoolType
                                    }
                                )
                            ) { backStackEntry ->
                                val loadFromArgs = backStackEntry.arguments
                                    ?.getBoolean(LinkScreen.PaymentMethod.loadArg) ?: false
                                linkAccount?.let { account ->
                                    PaymentMethodBody(
                                        account,
                                        viewModel.injector,
                                        loadFromArgs
                                    )
                                }
                            }

                            composable(
                                LinkScreen.CardEdit.route,
                                arguments = listOf(
                                    navArgument(LinkScreen.CardEdit.idArg) {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val id =
                                    backStackEntry.arguments?.getString(LinkScreen.CardEdit.idArg)
                                linkAccount?.let { account ->
                                    CardEditBody(
                                        account,
                                        viewModel.injector,
                                        requireNotNull(id)
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
                                AccountStatus.SignedOut,
                                AccountStatus.Error ->
                                    LinkScreen.SignUp
                            },
                            clearBackStack = true
                        )
                    }
                    window.decorView.rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterFromActivity()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.stripe_slide_down)
    }

    private fun dismiss(result: LinkActivityResult) {
        setResult(
            result.resultCode,
            Intent().putExtras(LinkActivityContract.Result(result).toBundle())
        )
        finish()
    }

    private fun isRootScreenFlow() =
        navController.currentBackStackEntryFlow.map { navController.isOnRootScreen() }
}
