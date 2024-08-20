package com.stripe.android.stripeconnect

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat.startActivity


class StripeConnect internal constructor(
    private val context: Context,
) {
    fun launchComponent(component: StripeConnectComponent) {
        startActivity(
            context,
            StripeConnectActivity.createIntent(context, component),
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
//        const val BASE_URL = "https://connect-js.stripe.com/v1.0/ios_webview.html" // production server

        internal fun uri(componentString: String): Uri {
            return Uri.parse(BASE_URL)
                .buildUpon()
                .appendQueryParameter("componentType", componentString)
                .appendQueryParameter("component", componentString)
                .appendQueryParameter("locale", "en-us")
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