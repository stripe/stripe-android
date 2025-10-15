package com.stripe.android.link

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.R
import com.stripe.android.core.Logger
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class LinkActivity : ComponentActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = LinkActivityViewModel.factory()

    internal var viewModel: LinkActivityViewModel? = null

    private var webLauncher: ActivityResultLauncher<LinkActivityContract.Args>? = null
    private var webAuthLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            viewModel = ViewModelProvider(this, viewModelFactory)[LinkActivityViewModel::class.java]
        } catch (e: NoArgsException) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create LinkActivityViewModel", e)
            setResult(RESULT_CANCELED)
            finish()
        }

        val vm = viewModel ?: return

        vm.linkLaunchMode.setTheme()

        vm.registerForActivityResult(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        webLauncher = registerForActivityResult(vm.activityRetainedComponent.webLinkActivityContract) { result ->
            vm.handleWebActivityResult(result)
        }

        webAuthLauncher = registerForActivityResult(WebLinkAuthActivityContract) { result ->
            vm.activityRetainedComponent.webLinkAuthChannel.results.tryEmit(result)
        }

        vm.launchWebFlow = ::launchWebFlow
        lifecycle.addObserver(vm)
        observeBackPress()

        lifecycleScope.launch {
            vm.activityRetainedComponent.webLinkAuthChannel.requests.collectLatest { url ->
                webAuthLauncher?.launch(url)
            }
        }

        setContent {
            val bottomSheetState = rememberStripeBottomSheetState(
                confirmValueChange = { vm.canDismissSheet },
            )

            LaunchedEffect(Unit) {
                vm.result.collect { result ->
                    bottomSheetState.hide()
                    dismissWithResult(result)
                }
            }

            LinkScreenContent(
                viewModel = vm,
                bottomSheetState = bottomSheetState,
            )
        }
    }

    /**
     * Set the theme to transparent if [LinkActivity] launches in confirmation mode.
     */
    private fun LinkLaunchMode.setTheme() {
        when (this) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.PaymentMethodSelection,
            is LinkLaunchMode.Authentication,
            is LinkLaunchMode.Authorization ->
                setTheme(R.style.StripePaymentSheetDefaultTheme)
            is LinkLaunchMode.Confirmation ->
                setTheme(R.style.StripeTransparentTheme)
        }
        renderEdgeToEdge()
    }

    private fun observeBackPress() {
        onBackPressedDispatcher.addCallback { viewModel?.handleBackPressed() }
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
        viewModel?.launchWebFlow = null
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    fun launchWebFlow(configuration: LinkConfiguration) {
        webLauncher?.launch(
            LinkActivityContract.Args(
                configuration = configuration,
                linkExpressMode = LinkExpressMode.DISABLED,
                linkAccountInfo = LinkAccountUpdate.Value(
                    account = null,
                    lastUpdateReason = null
                ),
                launchMode = LinkLaunchMode.Full,
                passiveCaptchaParams = null,
                attestOnIntentConfirmation = false,
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

internal const val FULL_SCREEN_CONTENT_TAG = "full_screen_content_tag"
internal const val VERIFICATION_DIALOG_CONTENT_TAG = "verification_dialog_content_tag"
