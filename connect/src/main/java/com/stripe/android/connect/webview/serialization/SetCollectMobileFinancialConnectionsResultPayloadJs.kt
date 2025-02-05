package com.stripe.android.connect.webview.serialization

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.serializers.StripeErrorSerializer
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Token
import com.stripe.android.model.parsers.TokenSerializer
import kotlinx.serialization.Serializable

@Serializable
internal data class SetCollectMobileFinancialConnectionsResultPayloadJs(
    val id: String,
    val financialConnectionsSession: FinancialConnectionsSessionJs?,
    @Serializable(TokenSerializer::class) val token: Token?,
    @Serializable(StripeErrorSerializer::class) val error: StripeError?,
) {
    companion object {
        fun from(
            id: String,
            result: FinancialConnectionsSheetResult?
        ): SetCollectMobileFinancialConnectionsResultPayloadJs {
            return when (result) {
                null,
                is FinancialConnectionsSheetResult.Canceled -> {
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = FinancialConnectionsSessionJs(accounts = emptyList()),
                        token = null,
                        error = null,
                    )
                }
                is FinancialConnectionsSheetResult.Failed -> {
                    val error = (result.error as? StripeException)?.stripeError
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = null,
                        token = null,
                        error = error
                    )
                }
                is FinancialConnectionsSheetResult.Completed -> {
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = FinancialConnectionsSessionJs(
                            accounts = result.financialConnectionsSession.accounts.data,
                        ),
                        token = result.financialConnectionsSession.parsedToken,
                        error = null,
                    )
                }
            }
        }
    }
}

@Serializable
internal data class FinancialConnectionsSessionJs(
    val accounts: List<FinancialConnectionsAccount>
)
