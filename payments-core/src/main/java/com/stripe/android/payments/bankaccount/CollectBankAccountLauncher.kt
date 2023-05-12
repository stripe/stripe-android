package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.navigation.toExposedResult
import kotlinx.parcelize.Parcelize

/**
 * API to collect bank account information for a given PaymentIntent or SetupIntent.
 *
 * use [CollectBankAccountLauncher.create] to instantiate this object.
 */
interface CollectBankAccountLauncher {

    fun presentWithPaymentIntent(
        publishableKey: String,
        stripeAccountId: String? = null,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    )

    fun presentWithSetupIntent(
        publishableKey: String,
        stripeAccountId: String? = null,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun presentWithDeferredPayment(
        publishableKey: String,
        stripeAccountId: String? = null,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
        amount: Int?,
        currency: String?
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun presentWithDeferredSetup(
        publishableKey: String,
        stripeAccountId: String? = null,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun unregister()

    companion object {

        private const val LAUNCHER_KEY = "CollectBankAccountLauncher"

        /**
         * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        fun create(
            activity: ComponentActivity,
            callback: (CollectBankAccountResult) -> Unit
        ): CollectBankAccountLauncher {
            return StripeCollectBankAccountLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it.toExposedResult())
                }
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment],  it needs
         * to be called before the [Fragment] is created.
         */
        fun create(
            fragment: Fragment,
            callback: (CollectBankAccountResult) -> Unit
        ): CollectBankAccountLauncher {
            return StripeCollectBankAccountLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it.toExposedResult())
                }
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createInternal(
            activity: ComponentActivity,
            callback: (CollectBankAccountResultInternal) -> Unit
        ): CollectBankAccountLauncher {
            return StripeCollectBankAccountLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it)
                }
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment],  it needs
         * to be called before the [Fragment] is created.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createInternal(
            fragment: Fragment,
            callback: (CollectBankAccountResultInternal) -> Unit
        ): CollectBankAccountLauncher {
            return StripeCollectBankAccountLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it)
                }
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            callback: (CollectBankAccountResultInternal) -> Unit,
        ): CollectBankAccountLauncher {
            return StripeCollectBankAccountLauncher(
                activityResultRegistryOwner.activityResultRegistry.register(
                    LAUNCHER_KEY,
                    CollectBankAccountContract(),
                    callback,
                )
            )
        }
    }
}

internal class StripeCollectBankAccountLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>
) : CollectBankAccountLauncher {

    override fun presentWithPaymentIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
                attachToIntent = true
            )
        )
    }

    override fun presentWithSetupIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
                attachToIntent = true
            )
        )
    }

    override fun presentWithDeferredPayment(
        publishableKey: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
        amount: Int?,
        currency: String?
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                configuration = configuration,
                customerId = customerId,
                onBehalfOf = onBehalfOf,
                amount = amount,
                currency = currency,
            )
        )
    }

    override fun presentWithDeferredSetup(
        publishableKey: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredSetupIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                configuration = configuration,
                customerId = customerId,
                onBehalfOf = onBehalfOf,
            )
        )
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }
}

sealed class CollectBankAccountConfiguration : Parcelable {
    @Parcelize
    data class USBankAccount(
        val name: String,
        val email: String?
    ) : Parcelable, CollectBankAccountConfiguration()
}
