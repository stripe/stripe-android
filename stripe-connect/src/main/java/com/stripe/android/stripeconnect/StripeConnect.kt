package com.stripe.android.stripeconnect

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat.startActivity
import java.util.Locale

class StripeConnect internal constructor(
    private val context: Context,
) {
    fun launchComponent(
        component: StripeConnectComponent,
        account: String?,
        selectedAppearance: Appearance?,
    ) {
        startActivity(
            context,
            StripeConnectActivity.createIntent(context, component, account, selectedAppearance),
            Bundle(),
        )
    }

    companion object {
        @JvmStatic
        fun create(
            context: Context,
        ): StripeConnect {
            return StripeConnect(context)
        }

        private const val BASE_URL = "https://stripe-connect-example.glitch.me/" // test server
//        const val BASE_URL = "https://connect-js.stripe.com/v1.0/android_webview.html" // production server

        internal fun uri(componentString: String, account: String?, locale: Locale): Uri {
            return Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter("componentType", componentString)
                .appendQueryParameter("component", componentString)
                .appendQueryParameter("locale", locale.toLanguageTag())
                .appendQueryParameter("account", account ?: "acct_1PHAcmPqW2nb5bJu") // Max's account
                .build()
        }
    }
}

enum class StripeConnectComponent {
    AccountManagement,
    AccountOnboarding,
    Documents,
    Payments,
    PaymentDetails,
    Payouts,
    PayoutsList,
}