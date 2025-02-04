package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.Logger
import com.stripe.android.link.ui.FullScreenContent
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal class LinkActivity : ComponentActivity() {
    internal var viewModel: LinkActivityViewModel? = null

    private var webLauncher: ActivityResultLauncher<LinkActivityContract.Args>? = null

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

        vm.launchWebFlow = ::launchWebFlow
        vm.dismissWithResult = ::dismissWithResult
        lifecycle.addObserver(vm)

        setContent {
            val screenState by vm.linkScreenState.collectAsState()

            when (screenState) {
                ScreenState.FullScreen -> {
                    FullScreenContent(
                        viewModel = vm,
                        onBackPressed = onBackPressedDispatcher::onBackPressed
                    )
                }
                ScreenState.Loading -> Unit
                is ScreenState.VerificationDialog -> Unit
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
        webLauncher?.launch(
            LinkActivityContract.Args(
                configuration = configuration,
                startWithVerificationDialog = false,
                linkAccount = null
            )
        )
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
