package com.stripe.android.financialconnections.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Poko
class GetFinancialConnectionsAccountsParams internal constructor(
    @SerialName("client_secret") private val clientSecret: String,
    @SerialName("starting_after") private val startingAfterAccountId: String?
) : Parcelable {
    fun toParamMap(): Map<String, Any> {
        return listOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_STARTING_AFTER to startingAfterAccountId
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }
    }

    private companion object {
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_STARTING_AFTER = "starting_after"
    }
}
