package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
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
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.core.Logger
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.cardedit.CardEditScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.signup.SignUpScreen
import com.stripe.android.link.ui.verification.VerificationScreen
import com.stripe.android.link.ui.wallet.WalletScreen
import com.stripe.android.ui.core.CircularProgressIndicator
import kotlinx.coroutines.launch

internal class LinkActivity : ComponentActivity() {
    internal var viewModel: LinkActivityViewModel? = null

    @VisibleForTesting
    internal lateinit var navController: NavHostController

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            viewModel = ViewModelProvider(this, LinkActivityViewModel.factory())[LinkActivityViewModel::class.java]
        } catch (e: NoArgsException) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create LinkActivityViewModel", e)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        setContent {
            val vm = viewModel ?: return@setContent
            var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
            val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            val coroutineScope = rememberCoroutineScope()
            val appBarState by vm.linkState.collectAsState()

            if (bottomSheetContent != null) {
                DisposableEffect(bottomSheetContent) {
                    coroutineScope.launch { sheetState.show() }
                    onDispose {
                        coroutineScope.launch { sheetState.hide() }
                    }
                }
            }
            navController = rememberNavController()

            LaunchedEffect(Unit) {
                viewModel?.let {
                    it.navController = navController
                    it.dismissWithResult = ::dismissWithResult
                    lifecycle.addObserver(it)
                }
            }

            LinkScreen(
                appBarState = appBarState,
                sheetState = sheetState,
                bottomSheetContent = bottomSheetContent,
                onUpdateSheetContent = {
                    bottomSheetContent = it
                }
            )
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun LinkScreen(
        appBarState: LinkAppBarState,
        sheetState: ModalBottomSheetState,
        bottomSheetContent: BottomSheetContent?,
        onUpdateSheetContent: (BottomSheetContent?) -> Unit
    ) {
        val vm = viewModel ?: return
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
                        vm.handleViewAction(LinkAction.BackPressed)
                    }

                    LinkAppBar(
                        state = appBarState,
                        onBackPressed = onBackPressedDispatcher::onBackPressed,
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

                    Screens()
                }
            }
        }
    }

    @Composable
    private fun Screens() {
        NavHost(
            navController = navController,
            startDestination = LinkScreen.Loading.route
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
                SignUpScreen(
                    navController = navController
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
        viewModel?.unregisterActivity()
    }

    companion object {
        internal const val EXTRA_ARGS = "native_link_args"

        internal fun createIntent(
            context: Context,
            args: NativeLinkArgs
        ): Intent {
            return Intent(context, LinkActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): NativeLinkArgs? {
            return savedStateHandle.get<NativeLinkArgs>(EXTRA_ARGS)
        }
    }
}
