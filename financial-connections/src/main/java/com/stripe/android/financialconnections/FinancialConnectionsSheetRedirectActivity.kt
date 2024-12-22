package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.financialconnections.debug.DebugConfiguration
import com.stripe.android.financialconnections.di.FinancialConnectionsSingletonSharedComponentHolder
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity

/**
 * This Activity handles redirects from the Institution authorization flow hand-off from native.
 * It'll process the result url in [Intent.getData] and pass them back to the opening activity.
 */
class FinancialConnectionsSheetRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Used together, FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
         * clear everything on the stack above the opening activity, including CCT.
         */
        intent.data
            ?.let { uri ->
                val uriWithDebugConfiguration = uri
                    .overrideWithDebugConfiguration()
                    .overrideIfIntegrityFailed()
                uriWithDebugConfiguration.toIntent()
                    ?.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    ?.also { it.data = uriWithDebugConfiguration }
                    ?.let { startActivity(it) }
            }
        finish()
    }

    /**
     * @return Intent, or null if deeplink cannot be mapped to an Intent (in case of unknown deep links).
     */
    private fun Uri.toIntent(): Intent? = when {
        isFinancialConnectionsScheme().not() -> null
        // auth-redirect hosts:
        // redirections from Abstract Auth (shim) in web back to native SDK
        host == HOST_AUTH_REDIRECT -> FinancialConnectionsSheetNativeActivity::class.java
        // native-link-accounts hosts:
        // redirections from app2app back to SDK (while in native flow)
        host == HOST_NATIVE_LINK_ACCOUNTS -> FinancialConnectionsSheetNativeActivity::class.java
        // link-accounts hosts:
        // - redirections from embedded web AuthFlow back to SDK (/success, /cancel, /fail)
        // - redirections from app2app back to SDK (while in web AuthFlow)
        host == HOST_LINK_ACCOUNTS -> FinancialConnectionsSheetActivity::class.java
        // native-redirect hosts:
        // redirections from embedded web AuthFlow back SDK (app2app start)
        host == HOST_NATIVE_REDIRECT -> FinancialConnectionsSheetActivity::class.java

        else -> null
    }?.let { destinationActivity ->
        Intent(
            this@FinancialConnectionsSheetRedirectActivity,
            destinationActivity
        )
    }

    /**
     * Overrides app2app return url based on debug configuration (for testing purposes), or returns the original
     * [Uri], if no override set.
     */
    private fun Uri.overrideWithDebugConfiguration(): Uri =
        when (DebugConfiguration(application).overriddenNative) {
            true -> Uri.parse(toString().replace(HOST_LINK_ACCOUNTS, HOST_NATIVE_LINK_ACCOUNTS))
            false -> Uri.parse(toString().replace(HOST_NATIVE_LINK_ACCOUNTS, HOST_LINK_ACCOUNTS))
            null -> this
        }

    /**
     * When an integrity verdict fails, clients will switch to the web flow locally but backend will still
     * consider the flow native. This checks the local verdict state and overrides native deep links to web.
     */
    private fun Uri.overrideIfIntegrityFailed(): Uri =
        when (
            FinancialConnectionsSingletonSharedComponentHolder
                .getComponent(application)
                .integrityVerdictManager()
                .verdictFailed()
        ) {
            true -> Uri.parse(toString().replace(HOST_NATIVE_LINK_ACCOUNTS, HOST_LINK_ACCOUNTS))
            else -> this
        }

    private fun Uri.isFinancialConnectionsScheme(): Boolean {
        return (this.scheme == "stripe-auth" || this.scheme == "stripe")
    }

    private companion object {
        private const val HOST_NATIVE_LINK_ACCOUNTS = "link-native-accounts"
        private const val HOST_LINK_ACCOUNTS = "link-accounts"
        private const val HOST_NATIVE_REDIRECT = "native-redirect"
        private const val HOST_AUTH_REDIRECT = "auth-redirect"
    }
}
