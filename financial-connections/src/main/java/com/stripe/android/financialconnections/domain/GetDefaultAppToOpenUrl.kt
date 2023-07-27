package com.stripe.android.financialconnections.domain

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import javax.inject.Inject

/**
 * Check if a browser is available on the device.
 */
internal class GetDefaultAppToOpenUrl @Inject constructor(
    private val context: Application,
) {

    /**
     * @return the package name of the default app to open url, or null if no app is available.
     */
    operator fun invoke(
        urlToOpen: String = "https://"
    ): String? {
        return runCatching {
            val webAddress = Uri.parse(urlToOpen)
            val intentWeb = Intent(Intent.ACTION_VIEW, webAddress)
            val browserPackage: ComponentName? =  intentWeb.resolveActivity(context.packageManager)
            return browserPackage?.packageName
        }
            .onFailure {
                Log.e("EVENT", "error", it)
            }
            .getOrNull()
    }
}
