package com.stripe.android.paymentmethodmessaging.element

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import kotlinx.parcelize.Parcelize

@Parcelize
@OptIn(PaymentMethodMessagingElementPreview::class)
internal data class LearnMoreActivityArgs(
    val learnMoreUrl: String,
) : Parcelable {
    companion object {
        private const val LEARN_MORE_ARGS: String = "learn_more_args"
        fun createIntent(context: Context, args: LearnMoreActivityArgs): Intent {
            return Intent(context, LearnMoreActivity::class.java)
                .putExtra(LEARN_MORE_ARGS, args)
        }
        fun fromIntent(intent: Intent): LearnMoreActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, LEARN_MORE_ARGS, LearnMoreActivityArgs::class.java)
            }
        }

        fun addThemeQueryParam(url: String, theme: PaymentMethodMessagingElement.Appearance.Theme): String {
            val themeParam = when (theme) {
                PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> "&theme=stripe"
                PaymentMethodMessagingElement.Appearance.Theme.DARK -> "&theme=night"
                PaymentMethodMessagingElement.Appearance.Theme.FLAT -> "&theme=flat"
            }
            return url + themeParam
        }
    }
}
