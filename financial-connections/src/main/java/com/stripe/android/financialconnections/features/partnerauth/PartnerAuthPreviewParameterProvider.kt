@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.domain.Body
import com.stripe.android.financialconnections.domain.Cta
import com.stripe.android.financialconnections.domain.Display
import com.stripe.android.financialconnections.domain.Entry
import com.stripe.android.financialconnections.domain.OauthPrepane
import com.stripe.android.financialconnections.domain.PartnerNotice
import com.stripe.android.financialconnections.domain.Text
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.Image

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
                authSession = session(),
                isStripeDirect = false
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
                authSession = session(),
                isStripeDirect = false
            )
        ),
        // While browser is showing, this Async is loading.
        authenticationStatus = Loading(),
        viewEffect = null
    )

    private fun session() =
        FinancialConnectionsAuthorizationSession(
            flow = FinancialConnectionsAuthorizationSession.Flow.FINICITY_CONNECT_V2_OAUTH,
            showPartnerDisclosure = true,
            _isOAuth = true,
            nextPane = FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH,
            id = "1234",
            display = Display(
                Text(
                    oauthPrepane = oauthPrepane()
                )
            )
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
