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
import com.stripe.android.payments.bankaccount.navigation.toUSBankAccountResult
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
            return CollectBankAccountForACHLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it.toUSBankAccountResult())
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
            return CollectBankAccountForACHLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it.toUSBankAccountResult())
                }
            )
        }

        // TODO[BANKCON-10079] callback should return CollectBankAccountResult, instead of the internal result.
        // However, CollectBankAccountResult currently does not support nullable intents, a requirement
        // for deferred payment flows. Updating that implies a breaking change.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            callback: (CollectBankAccountResultInternal) -> Unit,
        ): CollectBankAccountLauncher {
            return CollectBankAccountForACHLauncher(
                activityResultRegistryOwner.activityResultRegistry.register(
                    LAUNCHER_KEY,
                    CollectBankAccountContract(),
                    callback,
                )
            )
        }
    }
}

sealed interface CollectBankAccountConfiguration : Parcelable {
    @Parcelize
    data class USBankAccount(
        val name: String,
        val email: String?
    ) : Parcelable, CollectBankAccountConfiguration

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class InstantDebits(
        val email: String?
    ) : Parcelable, CollectBankAccountConfiguration
}
