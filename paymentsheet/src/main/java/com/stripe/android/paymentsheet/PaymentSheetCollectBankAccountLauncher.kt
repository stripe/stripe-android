package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult

internal class PaymentSheetCollectBankAccountLauncher(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
) : CollectBankAccountLauncher {

    override fun presentWithPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret,
                configuration = configuration,
                attachToIntent = false
            )
        )
    }

    override fun presentWithSetupIntent(
        publishableKey: String,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = publishableKey,
                clientSecret = clientSecret,
                configuration = configuration,
                attachToIntent = false
            )
        )
    }

    companion object {
        fun create(
            activity: ComponentActivity,
            callback: (CollectBankAccountResult) -> Unit
        ): PaymentSheetCollectBankAccountLauncher {
            return PaymentSheetCollectBankAccountLauncher(
                activity.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it)
                }
            )
        }

        fun create(
            fragment: Fragment,
            callback: (CollectBankAccountResult) -> Unit
        ): PaymentSheetCollectBankAccountLauncher {
            return PaymentSheetCollectBankAccountLauncher(
                fragment.registerForActivityResult(CollectBankAccountContract()) {
                    callback(it)
                }
            )
        }
    }
}
