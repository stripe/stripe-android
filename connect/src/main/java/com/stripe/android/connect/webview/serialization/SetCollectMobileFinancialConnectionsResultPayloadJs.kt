package com.stripe.android.connect.webview.serialization

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.serializers.StripeErrorSerializer
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
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
            result: FinancialConnectionsSheetForTokenResult?
        ): SetCollectMobileFinancialConnectionsResultPayloadJs {
            return when (result) {
                null,
                is FinancialConnectionsSheetForTokenResult.Canceled -> {
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = FinancialConnectionsSessionJs(accounts = emptyList()),
                        token = null,
                        error = null,
                    )
                }
                is FinancialConnectionsSheetForTokenResult.Failed -> {
                    val error = (result.error as? StripeException)?.stripeError
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = null,
                        token = null,
                        error = error
                    )
                }
                is FinancialConnectionsSheetForTokenResult.Completed -> {
                    SetCollectMobileFinancialConnectionsResultPayloadJs(
                        id = id,
                        financialConnectionsSession = FinancialConnectionsSessionJs(
                            accounts = result.financialConnectionsSession.accounts.data,
                        ),
                        token = result.token,
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
