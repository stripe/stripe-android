package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.stripe.android.core.Logger
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.LinkContent
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal class LinkActivity : ComponentActivity() {
    internal var viewModel: LinkActivityViewModel? = null

    @VisibleForTesting
    internal lateinit var navController: NavHostController
    private var webLauncher: ActivityResultLauncher<LinkActivityContract.Args>? = null

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

        val vm = viewModel ?: return
        vm.registerActivityForConfirmation(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        webLauncher = registerForActivityResult(vm.activityRetainedComponent.webLinkActivityContract) { result ->
            dismissWithResult(result)
        }

        setContent {
            var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
            val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
            val coroutineScope = rememberCoroutineScope()
            val appBarState by vm.linkAppBarState.collectAsState()

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
                    it.launchWebFlow = ::launchWebFlow
                    lifecycle.addObserver(it)
                }
            }

            EventReporterProvider(
                eventReporter = vm.eventReporter
            ) {
                LinkContent(
                    viewModel = vm,
                    navController = navController,
                    appBarState = appBarState,
                    sheetState = sheetState,
                    bottomSheetContent = bottomSheetContent,
                    onUpdateSheetContent = {
                        bottomSheetContent = it
                    },
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    moveToWeb = vm::moveToWeb
                )
            }
        }
    }

    private fun dismissWithResult(result: LinkActivityResult) {
        val bundle = bundleOf(
            LinkActivityContract.EXTRA_RESULT to result
        )
        this@LinkActivity.setResult(
            RESULT_COMPLETE,
            Intent().putExtras(bundle)
        )
        this@LinkActivity.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.unregisterActivity()
    }

    fun launchWebFlow(configuration: LinkConfiguration) {
        webLauncher?.launch(LinkActivityContract.Args(configuration))
    }

    companion object {
        internal const val EXTRA_ARGS = "native_link_args"
        internal const val RESULT_COMPLETE = 73563

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
