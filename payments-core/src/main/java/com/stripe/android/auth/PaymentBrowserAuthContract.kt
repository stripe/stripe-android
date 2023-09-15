package com.stripe.android.auth

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.PaymentAuthWebViewActivity

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

        val extras = input.toBundle()

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

        val statusBarColor: Int?,
        val publishableKey: String,
        val isInstantApp: Boolean
    ) : Parcelable {

        /**
         * We are using the default generated parcelable with the addition of settting null strings
         * to empty so that if StripeBrowserProxyReturnActivity is called from an unexpected
         * external source that it doesn't crash the app.
         */
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readParcelable(StripeToolbarCustomization::class.java.classLoader),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString() ?: "",
            parcel.readByte() != 0.toByte()
        )

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
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(objectId)
            parcel.writeInt(requestCode)
            parcel.writeString(clientSecret)
            parcel.writeString(url)
            parcel.writeString(returnUrl)
            parcel.writeByte(if (enableLogging) 1 else 0)
            parcel.writeParcelable(toolbarCustomization, flags)
            parcel.writeString(stripeAccountId)
            parcel.writeByte(if (shouldCancelSource) 1 else 0)
            parcel.writeByte(if (shouldCancelIntentOnUserNavigation) 1 else 0)
            parcel.writeValue(statusBarColor)
            parcel.writeString(publishableKey)
            parcel.writeByte(if (isInstantApp) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Args> {
            override fun createFromParcel(parcel: Parcel): Args {
                return Args(parcel)
            }

            override fun newArray(size: Int): Array<Args?> {
                return arrayOfNulls(size)
            }
        }
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
