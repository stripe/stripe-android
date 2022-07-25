package com.stripe.android.financialconnections.features.consent

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource

internal class ConsentStates : PreviewParameterProvider<ConsentState> {
    override val values = sequenceOf(
        canonical(),
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun canonical() = ConsentState(
            title = TextResource.StringId(
                R.string.stripe_consent_requested_data_title_no_businessname
            ),
            bullets = listOf(
                R.drawable.stripe_ic_safe to TextResource.StringId(R.string.stripe_consent_pane_body1_no_businessname),
                R.drawable.stripe_ic_shield to TextResource.StringId(R.string.stripe_consent_pane_body2),
                R.drawable.stripe_ic_lock to TextResource.StringId(R.string.stripe_consent_pane_body3)
            ),
            requestedDataBullets = listOf(
                Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_balances_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_balances_desc)
                ),
                Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_ownership_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_ownership_desc)
                ),
                Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_desc)
                ),
                Pair(
                    TextResource.StringId(R.string.stripe_consent_requested_data_transactions_title),
                    TextResource.StringId(R.string.stripe_consent_requested_data_transactions_desc)
                )
            ),
            requestedDataTitle = TextResource.StringId(
                R.string.stripe_consent_requested_data_title_no_businessname
            )
        )
    }
}
