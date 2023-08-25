package com.stripe.android.core.browser.customtabs

import android.content.Context
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
    val logger: Logger
) {

    /**
     * Goes through all apps that handle VIEW intents and have a warmup service. Picks
     * the one chosen by the user if there is one, otherwise makes a best effort to return a
     * valid package name.
     *
     * @param context to use for accessing [PackageManager].
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    operator fun invoke(context: Context): String? = runCatching {
        val pm = context.packageManager
        // Get default VIEW intent handler.
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
        val defaultBrowserPackage = defaultViewHandlerInfo?.activityInfo?.packageName

        // Get all apps that can handle VIEW intents.
        val packagesSupportingHttp = queryIntentActivities(pm, activityIntent)
        val packagesSupportingCustomTabs: List<String> = packagesSupportingHttp.filter { info ->
            val serviceIntent = Intent()
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION)
            serviceIntent.setPackage(info.activityInfo.packageName)
            pm.resolveService(serviceIntent, 0) != null
        }.map { it.activityInfo.packageName }

        return when {
            // If it is empty, device does not have a browser that supports Custom Tabs.
            packagesSupportingCustomTabs.isEmpty() -> null
            // Prefer the default browser if it supports Custom Tabs.
            defaultBrowserPackage?.isNotEmpty() == true &&
                !hasSpecializedHandlerIntents(context, activityIntent) &&
                packagesSupportingCustomTabs.contains(defaultBrowserPackage) -> defaultBrowserPackage
            // pick the next favorite Custom Tabs provider.
            else -> packagesSupportingCustomTabs[0]
        }
    }.getOrElse {
        logger.error("Exception while retrieving Custom Tabs package", it)
        null
    }

    /**
     * Used to check whether there is a specialized handler for a given intent.
     * @param intent The intent to check with.
     * @return Whether there is a specialized handler for the given intent.
     */
    private fun hasSpecializedHandlerIntents(
        context: Context,
        intent: Intent
    ): Boolean =
        runCatching {
            val handlers = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.GET_RESOLVED_FILTER
            )
            return handlers.any { resolveInfo ->
                val filter = resolveInfo.filter
                filter != null &&
                    filter.countDataAuthorities() != 0 &&
                    filter.countDataPaths() != 0 &&
                    resolveInfo.activityInfo != null
            }
        }.getOrElse {
            logger.error("Runtime exception while getting specialized handlers", it)
            false
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
