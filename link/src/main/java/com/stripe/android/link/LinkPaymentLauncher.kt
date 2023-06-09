package com.stripe.android.link

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @Inject internal constructor() {
    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            LinkActivityContract(),
            callback,
        )
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
        configuration: Configuration,
        prefilledNewCardParams: PaymentMethodCreateParams? = null,
    ) {
        val args = LinkActivityContract.Args(
            configuration,
            prefilledNewCardParams,
        )
        linkActivityResultLauncher?.launch(args)
    }

    /**
     * Arguments for launching [LinkActivity] to confirm a payment with Link.
     *
     * @param stripeIntent The Stripe Intent that is being processed
     * @param merchantName The customer-facing business name.
     * @param customerName Name of the customer, used to pre-fill the form.
     * @param customerEmail Email of the customer, used to pre-fill the form.
     * @param customerPhone Phone number of the customer, used to pre-fill the form.
     * @param shippingValues The initial shipping values for [FormController].
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Configuration(
        val stripeIntent: StripeIntent,
        val merchantName: String,
        val customerName: String?,
        val customerEmail: String?,
        val customerPhone: String?,
        val customerBillingCountryCode: String?,
        val shippingValues: Map<IdentifierSpec, String?>?
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        val supportedFundingSources = SupportedPaymentMethod.allTypes
    }
}
