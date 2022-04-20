package com.stripe.android.financialconnections

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

internal class FinancialConnectionsSheetContract :
    ActivityResultContract<FinancialConnectionsSheetContract.Args, FinancialConnectionsSheetResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, FinancialConnectionsSheetActivity::class.java).putExtra(
            EXTRA_ARGS,
            input
        )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetResult {
        val connectionsResult =
            intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.financialConnectionsSheetResult
        return connectionsResult ?: FinancialConnectionsSheetResult.Failed(
            IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
        )
    }

    @Parcelize
    data class Args constructor(
        val configuration: FinancialConnectionsSheet.Configuration,
    ) : Parcelable {

        fun validate() {
            if (configuration.linkAccountSessionClientSecret.isBlank()) {
                throw InvalidParameterException(
                    "The link account session client secret cannot be an empty string."
                )
            }
            if (configuration.publishableKey.isBlank()) {
                throw InvalidParameterException(
                    "The publishable key cannot be an empty string."
                )
            }
        }

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    data class Result(
        val financialConnectionsSheetResult: FinancialConnectionsSheetResult
    ) : Parcelable {
        fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    companion object {
        const val EXTRA_ARGS =
            "com.stripe.android.connections.ConnectionsSheetContract.extra_args"
        private const val EXTRA_RESULT =
            "com.stripe.android.connections.ConnectionsSheetContract.extra_result"
    }
}
