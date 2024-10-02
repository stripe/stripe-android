package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.navigation.toUSBankAccountResult
import dev.drewhamilton.poko.Poko
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val HOSTED_SURFACE_PAYMENT_ELEMENT = "payment_element"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val HOSTED_SURFACE_CUSTOMER_SHEET = "customer_sheet"

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
                // L1 (public standalone) integration is not hosted by any Stripe surface.
                hostedSurface = null,
                hostActivityLauncher = activity.registerForActivityResult(CollectBankAccountContract()) {
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
                // L1 (public standalone) integration is not hosted by any Stripe surface.
                hostedSurface = null,
                hostActivityLauncher = fragment.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it.toUSBankAccountResult())
                }
            )
        }

        // TODO[BANKCON-10079] callback should return CollectBankAccountResult, instead of the internal result.
        // However, CollectBankAccountResult currently does not support nullable intents, a requirement
        // for deferred payment flows. Updating that implies a breaking change.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createForPaymentSheet(
            hostedSurface: String,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            callback: (CollectBankAccountResultInternal) -> Unit,
        ): CollectBankAccountLauncher {
            return CollectBankAccountForACHLauncher(
                hostedSurface = hostedSurface,
                hostActivityLauncher = activityResultRegistryOwner.activityResultRegistry.register(
                    LAUNCHER_KEY,
                    CollectBankAccountContract(),
                    callback,
                )
            )
        }
    }
}

sealed interface CollectBankAccountConfiguration : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class USBankAccount(
        val name: String,
        val email: String?,
        val elementsSessionContext: ElementsSessionContext?,
    ) : Parcelable, CollectBankAccountConfiguration

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class InstantDebits(
        val email: String?,
        val elementsSessionContext: ElementsSessionContext?,
    ) : Parcelable, CollectBankAccountConfiguration

    companion object {

        @JvmStatic
        fun usBankAccount(
            name: String,
            email: String?,
        ): CollectBankAccountConfiguration {
            return USBankAccount(name, email, elementsSessionContext = null)
        }
    }
}
