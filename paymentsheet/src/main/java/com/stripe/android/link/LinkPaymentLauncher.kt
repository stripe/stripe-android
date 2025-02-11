package com.stripe.android.link

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.link.LinkActivityResult.PaymentMethodObtained
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.LinkAccount
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
            handleActivityResult(linkActivityResult, callback)
        }
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            linkActivityContract
        ) { linkActivityResult ->
            handleActivityResult(linkActivityResult, callback)
        }
    }

    private fun handleActivityResult(
        linkActivityResult: LinkActivityResult,
        nextStep: (LinkActivityResult) -> Unit
    ) {
        analyticsHelper.onLinkResult(linkActivityResult)
        when (linkActivityResult) {
            is PaymentMethodObtained, is LinkActivityResult.Completed -> {
                linkStore.markLinkAsUsed()
            }
            is LinkActivityResult.Canceled, is LinkActivityResult.Failed -> Unit
        }
        nextStep(linkActivityResult)
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
        linkAccount: LinkAccount?,
        useLinkExpress: Boolean
    ) {
        val args = LinkActivityContract.Args(
            configuration = configuration,
            linkAccount = linkAccount,
            startWithVerificationDialog = useLinkExpress
        )
        linkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }
}
