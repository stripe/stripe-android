package com.stripe.android.link

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.link.LinkActivityResult.PaymentMethodObtained
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.DefaultLinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
internal class LinkPaymentLauncher @Inject internal constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    linkEventsReporter: LinkEventsReporter,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val linkActivityContract: LinkActivityContract,
    private val linkStore: LinkStore
) : LinkPaymentPresenter {
    // A restored result can arrive during registration before a configuration is presented.
    private var analyticsHelper: LinkAnalyticsHelper = DefaultLinkAnalyticsHelper(linkEventsReporter)

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

    fun register(
        key: String = "LinkPaymentLauncher",
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "${paymentElementCallbackIdentifier}_$key",
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
    override fun present(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        launchMode: LinkLaunchMode,
        linkExpressMode: LinkExpressMode,
        statusBarColor: Int?,
    ) {
        val args = LinkActivityContract.Args(
            configuration = configuration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkExpressMode = linkExpressMode,
            linkAccountInfo = linkAccountInfo,
            launchMode = launchMode,
            statusBarColor = statusBarColor,
        )
        val analyticsHelper = linkConfigurationCoordinator.getComponent(configuration).linkAnalyticsHelper
        this.analyticsHelper = analyticsHelper
        linkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }
}

internal interface LinkPaymentPresenter {
    fun present(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        launchMode: LinkLaunchMode,
        linkExpressMode: LinkExpressMode,
        statusBarColor: Int?,
    )
}
