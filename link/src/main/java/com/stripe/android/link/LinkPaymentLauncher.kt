package com.stripe.android.link

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkActivityResult.Completed
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @Inject internal constructor(
    linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
    private val webLinkActivityContract: WebLinkActivityContract,
    private val nativeLinkActivityContract: NativeLinkActivityContract,
    private val linkStore: LinkStore,
) {
    private val analyticsHelper = linkAnalyticsComponentBuilder.build().linkAnalyticsHelper

    private var linkActivityResultLauncher:
        ActivityResultLauncher<WebLinkActivityContract.Args>? = null

    private var nativeLinkActivityResultLauncher:
        ActivityResultLauncher<NativeLinkActivityContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "LinkPaymentLauncher",
            webLinkActivityContract,
        ) { linkActivityResult ->
            analyticsHelper.onLinkResult(linkActivityResult)
            if (linkActivityResult is Completed) {
                linkStore.markLinkAsUsed()
            }
            callback(linkActivityResult)
        }

        nativeLinkActivityResultLauncher = activityResultRegistry.register(
            "NativeLinkPaymentLauncher",
            nativeLinkActivityContract,
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
            webLinkActivityContract
        ) { linkActivityResult ->
            analyticsHelper.onLinkResult(linkActivityResult)
            if (linkActivityResult is Completed) {
                linkStore.markLinkAsUsed()
            }
            callback(linkActivityResult)
        }

        nativeLinkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            nativeLinkActivityContract
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
        nativeLinkActivityResultLauncher?.unregister()
        linkActivityResultLauncher = null
        nativeLinkActivityResultLauncher = null
    }

    /**
     * Launch the Link UI to process a payment.
     *
     * @param configuration The payment and customer settings
     */
    fun present(
        configuration: LinkConfiguration,
    ) {
        val args = WebLinkActivityContract.Args(
            configuration,
        )
        linkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }

    /**
     * Launch the Link UI to process a payment.
     *
     * @param configuration The payment and customer settings
     */
    fun presentNative(
        configuration: LinkConfiguration,
    ) {
        val args = NativeLinkActivityContract.Args(
            configuration,
        )
        nativeLinkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }
}
