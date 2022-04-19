package com.stripe.android.financialconnections

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

internal class ConnectionsSheetContract :
    ActivityResultContract<ConnectionsSheetContract.Args, ConnectionsSheetResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, ConnectionsSheetActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): ConnectionsSheetResult {
        val connectionsResult =
            intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.connectionsSheetResult
        return connectionsResult ?: ConnectionsSheetResult.Failed(
            IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
        )
    }

    @Parcelize
    data class Args constructor(
        val configuration: ConnectionsSheet.Configuration,
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
        val connectionsSheetResult: ConnectionsSheetResult
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
