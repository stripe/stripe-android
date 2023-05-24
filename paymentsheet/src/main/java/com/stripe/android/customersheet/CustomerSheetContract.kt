package com.stripe.android.customersheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class CustomerSheetContract :
    ActivityResultContract<CustomerSheetContract.Args, InternalCustomerSheetResult?>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CustomerSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalCustomerSheetResult? {
        return InternalCustomerSheetResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val data: String,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
