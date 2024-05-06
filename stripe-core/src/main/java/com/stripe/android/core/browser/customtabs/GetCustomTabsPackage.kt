package com.stripe.android.core.browser.customtabs

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import com.stripe.android.core.Logger
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetCustomTabsPackage @Inject constructor(
    val logger: Logger,
    val application: Application
) {

    /**
     * Goes through all apps that handle VIEW intents and have a warmup service. Picks
     * the one chosen by the user if there is one, otherwise makes a best effort to return a
     * valid package name.
     *
     * @param context to use for accessing [PackageManager].
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    operator fun invoke(url: Uri): String? = runCatching {
        val pm = application.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, url)

        // Get default VIEW intent handler.
        val defaultPackage = pm
            .resolveActivity(activityIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

        // Get all apps that can handle VIEW intents.
        val packagesSupportingHttp = queryIntentActivities(pm, activityIntent)
        val packagesSupportingCustomTabs: List<String> = packagesSupportingHttp.filter { info ->
            val serviceIntent = Intent()
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION)
            serviceIntent.setPackage(info.activityInfo.packageName)
            pm.resolveService(serviceIntent, 0) != null
        }.map { it.activityInfo.packageName }

        return when {
            // We cannot resolve the default package. Although there might be a custom tabs browser installed,
            // this can be an app2app link (we cannot resolve the exact package without registering it on the manifest).
            // in this cases, let's not force using custom tabs.
            defaultPackage.isNullOrEmpty() -> null
            // Device does not have a browser that supports Custom Tabs.
            packagesSupportingCustomTabs.isEmpty() -> null
            // Prefer the default browser if it supports Custom Tabs.
            defaultPackage.isNotEmpty() && packagesSupportingCustomTabs.contains(defaultPackage) -> defaultPackage
            // pick the next favorite Custom Tabs provider.
            else -> packagesSupportingCustomTabs[0]
        }
    }.getOrElse {
        logger.error("Exception while retrieving Custom Tabs package", it)
        null
    }

    private fun queryIntentActivities(
        pm: PackageManager,
        activityIntent: Intent
    ): MutableList<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)
    } else {
        pm.queryIntentActivities(activityIntent, 0)
    }
}
