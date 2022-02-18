package com.stripe.android.networking

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.AppInfo
import com.stripe.android.core.version.StripeSdkVersion
import org.json.JSONObject

internal class StripeClientUserAgentHeaderFactory(
    private val systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER
) {
    fun create(
        appInfo: AppInfo? = null
    ): Map<String, String> {
        return mapOf(
            HEADER_STRIPE_CLIENT_USER_AGENT to createHeaderValue(appInfo).toString()
        )
    }

    @VisibleForTesting
    fun createHeaderValue(
        appInfo: AppInfo? = null
    ): JSONObject {
        return JSONObject(
            mapOf(
                "os.name" to "android",
                "os.version" to Build.VERSION.SDK_INT.toString(),
                "bindings.version" to StripeSdkVersion.VERSION_NAME,
                "lang" to "Java",
                "publisher" to "Stripe",
                "http.agent" to systemPropertySupplier(PROP_USER_AGENT)
            ).plus(
                appInfo?.createClientHeaders().orEmpty()
            )
        )
    }

    internal companion object {
        // this is the default user agent set by the system
        private const val PROP_USER_AGENT = "http.agent"

        private val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
            System.getProperty(name).orEmpty()
        }

        internal const val HEADER_STRIPE_CLIENT_USER_AGENT = "X-Stripe-Client-User-Agent"
    }
}
