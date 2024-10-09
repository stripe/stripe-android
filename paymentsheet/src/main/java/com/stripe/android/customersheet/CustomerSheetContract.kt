package com.stripe.android.customersheet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.parcelize.Parcelize

private const val ArgsKey = "args"

internal class CustomerSheetContract :
    ActivityResultContract<CustomerSheetContract.Args, InternalCustomerSheetResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CustomerSheetActivity::class.java)
            .putExtra(ArgsKey, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalCustomerSheetResult {
        return InternalCustomerSheetResult.fromIntent(intent) ?: InternalCustomerSheetResult.Error(
            IllegalArgumentException("Failed to retrieve a CustomerSheetResult")
        )
    }

    @Parcelize
    internal data class Args(
        val integrationType: CustomerSheetIntegration.Type,
        val configuration: CustomerSheet.Configuration,
        val statusBarColor: Int?,
    ) : Parcelable {

        companion object {

            fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(ArgsKey)
            }
        }
    }
}
