package com.stripe.android.customersheet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.parcelize.Parcelize

internal class CustomerSheetContract :
    ActivityResultContract<CustomerSheetContract.Args, InternalCustomerSheetResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CustomerSheetActivity::class.java)
            .putExtra("args", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalCustomerSheetResult {
        return InternalCustomerSheetResult.fromIntent(intent) ?: InternalCustomerSheetResult.Error(
            IllegalArgumentException("Failed to retrieve a CustomerSheetResult")
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    @Parcelize
    internal data class Args(
        val configuration: CustomerSheet.Configuration,
        val statusBarColor: Int?,
    ) : Parcelable
}
