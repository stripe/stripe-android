@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.Body
import com.stripe.android.financialconnections.model.Cta
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.Flow
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.OauthPrepane
import com.stripe.android.financialconnections.model.PartnerNotice

internal class PartnerAuthPreviewParameterProvider :
    PreviewParameterProvider<PartnerAuthState> {
    override val values = sequenceOf(
        canonical(),
        browserLoading()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = PartnerAuthState(
        payload = Success(
            PartnerAuthState.Payload(
                institution = FinancialConnectionsInstitution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = true,
                    icon = null,
                    logo = null,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                ),
                authSessionId = "1234",
                authSessionUrl = "https://stripe.com",
                flow = Flow.FINICITY_CONNECT_V2_OAUTH.name,
                oauthPrepane = oauthPrepane(),
                isOAuth = true,
                isStripeDirect = false,
            )
        ),
        authenticationStatus = Uninitialized,
        viewEffect = null
    )

    private fun browserLoading() = PartnerAuthState(
        payload = Success(
            PartnerAuthState.Payload(
                institution = FinancialConnectionsInstitution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = true,
                    icon = null,
                    logo = null,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                ),
                authSessionId = "1234",
                authSessionUrl = "https://stripe.com",
                flow = Flow.FINICITY_CONNECT_V2_OAUTH.name,
                oauthPrepane = oauthPrepane(),
                isOAuth = true,
                isStripeDirect = false
            )
        ),
        // While browser is showing, this Async is loading.
        authenticationStatus = Loading(),
        viewEffect = null
    )

    private fun oauthPrepane(): OauthPrepane {
        val sampleImage =
            "https://b.stripecdn.com/connections-statics-srv/assets/PrepaneAsset--account_numbers-capitalone-2x.gif"
        return OauthPrepane(
            title = "Sign in with Sample bank",
            body = Body(
                listOf(
                    Entry.Text(
                        "Some very large text will most likely go here!" +
                            "Some very large text will most likely go here!"
                    ),
                    Entry.Image(
                        Image(sampleImage)
                    ),
                    Entry.Text(
                        "Some very large text will most likely go here!"
                    ),
                    Entry.Text(
                        "Some very large text will most likely go here!"
                    ),
                    Entry.Text(
                        "Some very large text will most likely go here!"
                    )
                )
            ),
            cta = Cta(
                icon = null,
                text = "Continue!"
            ),
            institutionIcon = null,
            partnerNotice = PartnerNotice(
                partnerIcon = Image(sampleImage),
                text = "Stripe works with partners like MX to reliably" +
                    " offer access to thousands of financial institutions." +
                    " Learn more"
            )
        )
    }
}
