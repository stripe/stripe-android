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
import java.lang.Exception

interface CollectBankAccountLauncher {

    fun launch(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountParams
    )

    /**
     * API to collect bank account information for [PaymentIntent].
     *
     * use [CollectBankAccountLauncher.ForPaymentIntent.create] to instantiate this object.
     */
    interface ForPaymentIntent : CollectBankAccountLauncher {

        companion object {
            /**
             * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
             *
             * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
             * to be called before the [ComponentActivity] is created.
             */
            fun create(
                activity: ComponentActivity,
                callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
            ): ForPaymentIntent {
                return StripeCollectBankAccountForPaymentIntentLauncher(
                    activity.registerForActivityResult(CollectBankAccountContract()) {
                        callback.trigger(
                            it
                        )
                    },
                    callback = callback
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
                callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
            ): ForPaymentIntent {
                return StripeCollectBankAccountForPaymentIntentLauncher(
                    fragment.registerForActivityResult(CollectBankAccountContract()) {
                        callback.trigger(
                            it
                        )
                    },
                    callback = callback
                )
            }

            private fun ApiResultCallback<CollectBankAccountForPaymentResponse>.trigger(
                result: CollectBankAccountResult
            ) = when (result) {
                is CollectBankAccountResult.Completed -> onSuccess(
                    CollectBankAccountForPaymentResponse(result.intent as PaymentIntent)
                )
                is CollectBankAccountResult.Failed -> onError(Exception(result.error))
            }
        }
    }

    /**
     * API to collect bank account information for [SetupIntent].
     *
     * use [CollectBankAccountLauncher.ForSetupIntent.create] to instantiate this object.
     */
    interface ForSetupIntent : CollectBankAccountLauncher {

        companion object {

            /**
             * Create a [CollectBankAccountLauncher] instance with [ComponentActivity].
             *
             * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
             * to be called before the [ComponentActivity] is created.
             */
            fun create(
                activity: ComponentActivity,
                callback: ApiResultCallback<CollectBankAccountForSetupResponse>
            ): ForSetupIntent {
                return StripeCollectBankAccountForSetupIntentLauncher(
                    activity.registerForActivityResult(CollectBankAccountContract()) { result ->
                        callback.trigger(result)
                    },
                    callback = callback
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
                callback: ApiResultCallback<CollectBankAccountForSetupResponse>
            ): ForSetupIntent {
                return StripeCollectBankAccountForSetupIntentLauncher(
                    fragment.registerForActivityResult(CollectBankAccountContract()) { result ->
                        callback.trigger(result)
                    },
                    callback = callback
                )
            }

            private fun ApiResultCallback<CollectBankAccountForSetupResponse>.trigger(
                result: CollectBankAccountResult
            ) = when (result) {
                is CollectBankAccountResult.Completed -> onSuccess(
                    CollectBankAccountForSetupResponse(result.intent as SetupIntent)
                )
                is CollectBankAccountResult.Failed -> onError(Exception(result.error))
            }
        }
    }
}

internal class StripeCollectBankAccountForPaymentIntentLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    val callback: ApiResultCallback<CollectBankAccountForPaymentResponse>
) : CollectBankAccountLauncher.ForPaymentIntent {

    override fun launch(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountParams
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

internal class StripeCollectBankAccountForSetupIntentLauncher constructor(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    val callback: ApiResultCallback<CollectBankAccountForSetupResponse>
) : CollectBankAccountLauncher.ForSetupIntent {

    override fun launch(
        publishableKey: String,
        clientSecret: String,
        params: CollectBankAccountParams
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

sealed class CollectBankAccountParams : Parcelable {
    @Parcelize
    data class USBankAccount(
        val name: String,
        val email: String?
    ) : Parcelable, CollectBankAccountParams()
}

@Parcelize
data class CollectBankAccountForPaymentResponse(
    val paymentIntent: PaymentIntent
) : StripeModel

@Parcelize
data class CollectBankAccountForSetupResponse(
    val setupIntent: SetupIntent
) : StripeModel

