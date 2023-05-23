package com.stripe.android.customersheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.ExperimentalCustomerSheetApi
import java.io.Serializable

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSheetContract :
    ActivityResultContract<CustomerSheetContract.Args, CustomerSheetResult?>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CustomerSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CustomerSheetResult? {
        return CustomerSheetResult.fromIntent(intent)
    }

    internal data class Args(
        val data: String,
    ) : Serializable {
        internal companion object {
            private const val serialVersionUID = 1L
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getSerializableExtra(EXTRA_ARGS) as? Args
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
