package com.stripe.android.financialconnections.domain

import android.app.Application
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

/**
 * Check if a browser is available on the device.
 */
internal class IsBrowserAvailable @Inject constructor(
    private val context: Application,
) {

    operator fun invoke(): Boolean {
        val url = "https://"
        val webAddress = Uri.parse(url)
        val intentWeb = Intent(Intent.ACTION_VIEW, webAddress)
        return intentWeb.resolveActivity(context.packageManager) != null
    }
}
