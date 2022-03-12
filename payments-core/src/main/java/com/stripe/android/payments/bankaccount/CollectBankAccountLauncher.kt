package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.ApiResultCallback
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

/**
 * API to confirm and handle next actions for [PaymentIntent] and [SetupIntent].
 *
 * use [CollectBankAccountLauncher.createForPayment], [CollectBankAccountLauncher.createForSetup]
 * to instantiate this object.
 */
interface CollectBankAccountLauncher<T : CollectBankAccountParams> {

    fun launch(
        clientSecret: String,
        params: T
    )

    companion object {
        /**
         * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        fun createForPayment(
            activity: ComponentActivity,
            publishableKey: String,
            callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
        ): CollectBankAccountLauncher<CollectBankAccountForPaymentParams> {
            return StripeCollectBankAccountForPaymentLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    // convert result to CollectBankAccountForPaymentResponse
                },
                publishableKey = publishableKey,
                callback = callback
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment],  it needs
         * to be called before the [Fragment] is created.
         */
        fun createForPayment(
            fragment: Fragment,
            publishableKey: String,
            callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
        ): CollectBankAccountLauncher<CollectBankAccountForPaymentParams> {
            return StripeCollectBankAccountForPaymentLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    // convert result to CollectBankAccountForPaymentResponse
                },
                publishableKey = publishableKey,
                callback = callback
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        fun createForSetup(
            activity: ComponentActivity,
            publishableKey: String,
            callback: ApiResultCallback<CollectBankAccountForSetupResponse>
        ): CollectBankAccountLauncher<CollectBankAccountForSetupParams> {
            return StripeCollectBankAccountForSetupLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    // convert result to CollectBankAccountForSetupResponse

                },
                publishableKey = publishableKey,
                callback = callback
            )
        }

        /**
         * Create a [CollectBankAccountLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment],  it needs
         * to be called before the [Fragment] is created.
         */
        fun createForSetup(
            fragment: Fragment,
            publishableKey: String,
            callback: ApiResultCallback<CollectBankAccountForSetupResponse>
        ): CollectBankAccountLauncher<CollectBankAccountForSetupParams> {
            return StripeCollectBankAccountForSetupLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    // convert result to CollectBankAccountForSetupResponse
                },
                publishableKey = publishableKey,
                callback = callback
            )
        }
    }
}

internal class StripeCollectBankAccountForPaymentLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    val publishableKey: String,
    val callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
) : CollectBankAccountLauncher<CollectBankAccountForPaymentParams> {

    override fun launch(
        clientSecret: String,
        params: CollectBankAccountForPaymentParams
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret,
                params = params
            )
        )
    }
}

internal class StripeCollectBankAccountForSetupLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    val publishableKey: String,
    val callback: ApiResultCallback<CollectBankAccountForSetupResponse>
) : CollectBankAccountLauncher<CollectBankAccountForSetupParams> {

    override fun launch(
        clientSecret: String,
        params: CollectBankAccountForSetupParams
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

interface CollectBankAccountParams

@Parcelize
data class CollectBankAccountForPaymentParams(
    val paymentMethodType: String,
    val billingDetails: BillingDetails
) : Parcelable, CollectBankAccountParams

@Parcelize
data class CollectBankAccountForSetupParams(
    val paymentMethodType: String,
    val billingDetails: BillingDetails
) : Parcelable, CollectBankAccountParams

@Parcelize
data class CollectBankAccountForPaymentResponse(
    val paymentIntent: PaymentIntent
) : StripeModel

@Parcelize
data class CollectBankAccountForSetupResponse(
    val setupIntent: SetupIntent
) : StripeModel



@Parcelize
data class BillingDetails(
    val name: String,
    val email: String?
) : Parcelable
