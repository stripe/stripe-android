package com.stripe.android.link

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.link.LinkActivityResult.Completed
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
internal class LinkPaymentLauncher @Inject internal constructor(
    linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
    private val linkActivityContract: LinkActivityContract,
    private val linkStore: LinkStore,
) {
    private val analyticsHelper = linkAnalyticsComponentBuilder.build().linkAnalyticsHelper

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "LinkPaymentLauncher",
            linkActivityContract,
        ) { linkActivityResult ->
            analyticsHelper.onLinkResult(linkActivityResult)
            if (linkActivityResult is Completed) {
                linkStore.markLinkAsUsed()
            }
            callback(linkActivityResult)
        }
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            linkActivityContract
        ) { linkActivityResult ->
            analyticsHelper.onLinkResult(linkActivityResult)
            if (linkActivityResult is Completed) {
                linkStore.markLinkAsUsed()
            }
            callback(linkActivityResult)
        }
    }

    fun unregister() {
        linkActivityResultLauncher?.unregister()
        linkActivityResultLauncher = null
    }

    /**
     * Launch the Link UI to process a payment.
     *
     * @param configuration The payment and customer settings
     */
    fun present(
        configuration: LinkConfiguration,
    ) {
        val args = LinkActivityContract.Args(
            configuration,
        )
        linkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }
}
