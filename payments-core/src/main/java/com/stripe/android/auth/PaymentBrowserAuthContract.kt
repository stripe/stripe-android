package com.stripe.android.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.PaymentAuthWebViewActivity
import kotlinx.parcelize.Parcelize

/**
 * An [ActivityResultContract] for completing payment authentication in a browser. This will
 * be handled in either [StripeBrowserLauncherActivity] or [PaymentAuthWebViewActivity].
 */
internal class PaymentBrowserAuthContract :
    ActivityResultContract<PaymentBrowserAuthContract.Args, PaymentFlowResult.Unvalidated>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        val defaultReturnUrl = DefaultReturnUrl.create(context)
        val shouldUseBrowser =
            input.hasDefaultReturnUrl(defaultReturnUrl) || input.isInstantApp

        val statusBarColor = when (context) {
            is Activity -> context.window?.statusBarColor
            else -> null
        }

        val extras = input
            .copy(statusBarColor = statusBarColor)
            .toBundle()

        return Intent(
            context,
            when (shouldUseBrowser) {
                true -> StripeBrowserLauncherActivity::class.java
                false -> PaymentAuthWebViewActivity::class.java
            }
        ).also { intent ->
            intent.putExtras(extras)
        }
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated {
        return intent?.getParcelableExtra(EXTRA_ARGS) ?: PaymentFlowResult.Unvalidated()
    }

    @Parcelize
    internal data class Args(
        val objectId: String,
        val requestCode: Int,
        val clientSecret: String,
        val url: String,
        val returnUrl: String? = null,
        val enableLogging: Boolean = false,
        val toolbarCustomization: StripeToolbarCustomization? = null,
        val stripeAccountId: String? = null,

        /**
         * TODO(mshafrir-stripe): we should probably rename this to `canCancelSource`
         */
        val shouldCancelSource: Boolean = false,

        /**
         * For most payment methods, if the user navigates away from the webview
         * (e.g. by pressing the back button or tapping "close" in the menu bar),
         * we assume the confirmation flow has been cancelled.
         *
         * However, for some payment methods, such as OXXO, no immediate user action is required.
         * Simply displaying the web view is all we need to do, and we expect the user to
         * navigate away after this.
         */
        val shouldCancelIntentOnUserNavigation: Boolean = true,

        val statusBarColor: Int? = null,
        val publishableKey: String,
        val isInstantApp: Boolean
    ) : Parcelable {
        /**
         * Pre-requisite for using [StripeBrowserLauncherActivity].
         * If false, use [PaymentAuthWebViewActivity].
         */
        internal fun hasDefaultReturnUrl(
            defaultReturnUrl: DefaultReturnUrl
        ): Boolean {
            return returnUrl == defaultReturnUrl.value
        }

        fun toBundle() = bundleOf(EXTRA_ARGS to this)
    }

    companion object {
        private const val EXTRA_ARGS = "extra_args"

        internal fun parseArgs(
            intent: Intent
        ): Args? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}
