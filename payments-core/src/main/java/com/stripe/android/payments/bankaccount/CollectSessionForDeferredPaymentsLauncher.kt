package com.stripe.android.payments.bankaccount

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsResult

/**
 * API to create and collect a Financial Connections Session for deferred payments.
 *
 * use [CollectSessionForDeferredPaymentsLauncher.create] to instantiate this object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CollectSessionForDeferredPaymentsLauncher {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun presentForDeferredPayment(
        publishableKey: String,
        stripeAccountId: String? = null,
        elementsSessionId: String,
        customer: String?,
        amount: Int?,
        currency: String?
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun presentForDeferredSetup(
        publishableKey: String,
        stripeAccountId: String? = null,
        elementsSessionId: String,
        customer: String?,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Create a [CollectSessionForDeferredPaymentsLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(
            activity: ComponentActivity,
            callback: (CollectSessionForDeferredPaymentsResult) -> Unit
        ): CollectSessionForDeferredPaymentsLauncher {
            return StripeCollectSessionForDeferredPaymentsLauncher(
                activity.registerForActivityResult(CollectSessionForDeferredPaymentsContract()) {
                    callback(it)
                }
            )
        }

        /**
         * Create a [CollectSessionForDeferredPaymentsLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment],  it needs
         * to be called before the [Fragment] is created.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(
            fragment: Fragment,
            callback: (CollectSessionForDeferredPaymentsResult) -> Unit
        ): CollectSessionForDeferredPaymentsLauncher {
            return StripeCollectSessionForDeferredPaymentsLauncher(
                fragment.registerForActivityResult(CollectSessionForDeferredPaymentsContract()) {
                    callback(it)
                }
            )
        }
    }
}

internal class StripeCollectSessionForDeferredPaymentsLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectSessionForDeferredPaymentsContract.Args>
) : CollectSessionForDeferredPaymentsLauncher {

    override fun presentForDeferredPayment(
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionId: String,
        customer: String?,
        amount: Int?,
        currency: String?
    ) {
        hostActivityLauncher.launch(
            CollectSessionForDeferredPaymentsContract.Args(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                customer = customer,
                amount = amount,
                currency = currency
            )
        )
    }

    override fun presentForDeferredSetup(
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionId: String,
        customer: String?
    ) {
        hostActivityLauncher.launch(
            CollectSessionForDeferredPaymentsContract.Args(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                customer = customer,
                amount = null,
                currency = null
            )
        )
    }
}
