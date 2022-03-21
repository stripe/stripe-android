package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.core.model.StripeModel
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import kotlinx.parcelize.Parcelize

/**
 * API to collect bank account information for a given PaymentIntent or SetupIntent.
 *
 * use [CollectBankAccountLauncher.create] to instantiate this object.
 */
interface CollectBankAccountLauncher {

    fun presentWithPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountConfiguration
    )

    fun presentWithSetupIntent(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountConfiguration
    )

    companion object {
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
                    callback(it)
                },
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
                    callback(it)
                },
            )
        }
    }
}

internal class StripeCollectBankAccountLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
) : CollectBankAccountLauncher {

    override fun presentWithPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret,
                params = params
            )
        )
    }

    override fun presentWithSetupIntent(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret,
                params = params
            )
        )
    }
}

sealed class CollectBankAccountConfiguration : Parcelable {
    @Parcelize
    data class USBankAccount(
        val name: String,
        val email: String?
    ) : Parcelable, CollectBankAccountConfiguration()
}

@Parcelize
data class CollectBankAccountResponse(
    val clientSecret: String
) : StripeModel
