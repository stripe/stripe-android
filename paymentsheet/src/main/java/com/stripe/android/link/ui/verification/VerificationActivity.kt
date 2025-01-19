package com.stripe.android.link.ui.verification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.window.Dialog
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkExpressArgs
import com.stripe.android.link.LinkExpressContract
import com.stripe.android.link.LinkExpressResult
import com.stripe.android.link.NativeLinkArgs
import com.stripe.android.link.NoArgsException
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.paymentsheet.BuildConfig

class VerificationActivity: ComponentActivity() {
    internal var viewModel: VerificationViewModel? = null

    @VisibleForTesting
    internal lateinit var navController: NavHostController

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            viewModel = ViewModelProvider(
                owner = this,
                factory = VerificationViewModel.factory(
                    goBack = {
                        dismissWithResult(LinkExpressResult.Canceled)
                    },
                    onVerified = { linkAccount ->
                        dismissWithResult(LinkExpressResult.Authenticated(linkAccount))
                    },
                    onError = { error ->
                        dismissWithResult(LinkExpressResult.Failed(error))
                    }
                )
            )[VerificationViewModel::class.java]
        } catch (e: NoArgsException) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create VerificationViewModel", e)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val vm = viewModel ?: return

        setContent {
            Dialog(
                onDismissRequest = {}
            ) {
                DefaultLinkTheme {
                    VerificationScreen(vm)
                }
            }
        }
    }

    private fun dismissWithResult(result: LinkExpressResult) {
        val bundle = bundleOf(
            LinkExpressContract.EXTRA_RESULT to result
        )
        this@VerificationActivity.setResult(
            RESULT_COMPLETE,
            intent.putExtras(bundle)
        )
        this@VerificationActivity.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        internal const val EXTRA_ARGS = "link_express_args"
        internal const val RESULT_COMPLETE = 57576

        internal fun createIntent(
            context: Context,
            args: LinkExpressArgs
        ): Intent {
            return Intent(context, VerificationActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): LinkExpressArgs? {
            return savedStateHandle.get<LinkExpressArgs>(EXTRA_ARGS)
        }
    }
}