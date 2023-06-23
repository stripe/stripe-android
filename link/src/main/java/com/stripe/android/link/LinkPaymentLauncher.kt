package com.stripe.android.link

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import com.stripe.android.link.injection.LinkLauncherSubcomponent
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @Inject internal constructor(
    linkLauncherSubcomponentBuilder: LinkLauncherSubcomponent.Builder,
    private val linkActivityContract: LinkActivityContract,
) {
    private val analyticsHelper = linkLauncherSubcomponentBuilder.build().linkAnalyticsHelper

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "LinkPaymentLauncher",
            LinkActivityContract(),
            callback,
        )
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            linkActivityContract
        ) { linkActivityResult ->
            analyticsHelper.onLinkResult(linkActivityResult)
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
     * @param prefilledNewCardParams The card information prefilled by the user. If non null, Link
     *  will launch into adding a new card, with the card information pre-filled.
     */
    fun present(
        configuration: LinkConfiguration,
        prefilledNewCardParams: PaymentMethodCreateParams? = null,
    ) {
        val args = LinkActivityContract.Args(
            configuration,
            prefilledNewCardParams,
        )
        linkActivityResultLauncher?.launch(args)
        analyticsHelper.onLinkLaunched()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        val supportedFundingSources = SupportedPaymentMethod.allTypes
    }
}
