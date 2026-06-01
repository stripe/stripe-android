package com.stripe.link.core.navigation.deeplink

import com.stripe.link.core.analytics.Analytics
import com.stripe.link.core.data.network.LinkApiClient
import com.stripe.link.core.data.storage.LocalFeatureFlag
import com.stripe.link.core.data.storage.isEnabled
import com.stripe.link.core.navigation.Coordinator
import com.stripe.link.feature.common.LinkDispatchers
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface DeepLinkRouter {
    /**
     * Parse a deep link URL and return the navigation actions to execute.
     *
     * @param url The deep link URL to parse
     * @return List of actions to execute, or null if the URL is not a recognized deep link
     */
    fun parseDeepLink(url: String): List<Coordinator.Action>?
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RealDeepLinkRouter(
    private val linkApiClient: LinkApiClient,
    dispatchers: LinkDispatchers,
) : DeepLinkRouter {

    // Class-level scope for fire-and-forget POST — lives as long as the singleton (AppScope)
    private val scope = CoroutineScope(dispatchers.IO + SupervisorJob())

    override fun parseDeepLink(url: String): List<Coordinator.Action>? {
        Analytics.logBreadcrumb("Deep link received: $url")

        val route = DeepLinkRoute.fromPath(url)
        Analytics.logBreadcrumb("Deep link route: ${route::class.simpleName}")

        // Fire-and-forget email click recording — behind local feature flag
        if (LocalFeatureFlag.EmailClickTracking.isEnabled) {
            runCatching { Url(url) }.getOrNull()?.let { parsed ->
                val ref = parsed.parameters["ref"]
                val eid = parsed.parameters["eid"]
                if (ref != null && eid != null) {
                    Analytics.logBreadcrumb("Email click detected: ref=$ref, eid=$eid")
                    scope.launch {
                        linkApiClient.recordEmailClick(ref = ref, eid = eid, link = url)
                    }
                }
            }
        }

        val actions = route.actions
        return actions.takeIf { it.isNotEmpty() }
    }
}
