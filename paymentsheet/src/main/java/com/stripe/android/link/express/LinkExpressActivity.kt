package com.stripe.android.link.express

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.Logger
import com.stripe.android.link.NoArgsException
import com.stripe.android.link.ui.verification.VerificationDialogBody
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.paymentsheet.BuildConfig

class LinkExpressActivity : ComponentActivity() {
    internal var viewModelFactory: ViewModelProvider.Factory = LinkExpressViewModel.factory()
    internal var viewModel: LinkExpressViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            viewModel = ViewModelProvider(
                owner = this,
                factory = viewModelFactory
            )[LinkExpressViewModel::class.java]
        } catch (e: NoArgsException) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create LinkExpressViewModel", e)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val vm = viewModel ?: return
        vm.dismissWithResult = ::dismissWithResult

        setContent {
            val verificationViewModel = viewModel<VerificationViewModel>(
                factory = VerificationViewModel.factory(
                    onVerificationSucceeded = vm::onVerificationSucceeded,
                    onChangeEmailClicked = vm::onChangeEmailClicked,
                    onDismissClicked = vm::onDismissClicked,
                    linkAccount = vm.linkAccount,
                    isDialog = true,
                    parentComponent = vm.activityRetainedComponent
                )
            )
            VerificationDialogBody(
                viewModel = verificationViewModel
            )
        }
    }

    private fun dismissWithResult(result: LinkExpressResult) {
        val bundle = bundleOf(
            LinkExpressContract.EXTRA_RESULT to result
        )
        this@LinkExpressActivity.setResult(
            RESULT_COMPLETE,
            intent.putExtras(bundle)
        )
        this@LinkExpressActivity.finish()
    }

    companion object {
        internal const val EXTRA_ARGS = "link_express_args"
        internal const val RESULT_COMPLETE = 57576

        internal fun createIntent(
            context: Context,
            args: LinkExpressArgs
        ): Intent {
            return Intent(context, LinkExpressActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): LinkExpressArgs? {
            return savedStateHandle.get<LinkExpressArgs>(EXTRA_ARGS)
        }
    }
}
