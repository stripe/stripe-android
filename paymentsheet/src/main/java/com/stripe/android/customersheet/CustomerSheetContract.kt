package com.stripe.android.customersheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class CustomerSheetContract :
    ActivityResultContract<CustomerSheetContract.Args, InternalCustomerSheetResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CustomerSheetActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalCustomerSheetResult {
        return InternalCustomerSheetResult.fromIntent(intent) ?: InternalCustomerSheetResult.Error(
            IllegalArgumentException("Failed to retrieve a CustomerSheetResult")
        )
    }

    internal object Args
}
