@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.Body
import com.stripe.android.financialconnections.model.Cta
import com.stripe.android.financialconnections.model.Display
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.OauthPrepane
import com.stripe.android.financialconnections.model.PartnerNotice
import com.stripe.android.financialconnections.model.TextUpdate

internal class PartnerAuthPreviewParameterProvider :
    PreviewParameterProvider<SharedPartnerAuthState> {
    override val values = sequenceOf(
        canonical(),
        prepaneLoading(),
        browserLoading()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = SharedPartnerAuthState(
        payload = Success(
            SharedPartnerAuthState.Payload(
                isStripeDirect = false,
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
                showInModal = false
            )
        ),
        authenticationStatus = Uninitialized,
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun prepaneLoading() = SharedPartnerAuthState(
        payload = Loading(),
        authenticationStatus = Uninitialized,
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun browserLoading() = SharedPartnerAuthState(
        payload = Success(
            SharedPartnerAuthState.Payload(
                isStripeDirect = false,
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
                showInModal = false
            )
        ),
        // While browser is showing, this Async is loading.
        authenticationStatus = Loading(),
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun session() =
        FinancialConnectionsAuthorizationSession(
            flow = FinancialConnectionsAuthorizationSession.Flow.FINICITY_CONNECT_V2_OAUTH.name,
            showPartnerDisclosure = true,
            _isOAuth = true,
            nextPane = Pane.PARTNER_AUTH,
            id = "1234",
            display = Display(
                TextUpdate(
                    oauthPrepane = oauthPrepane()
                )
            )
        )

    private fun oauthPrepane(): OauthPrepane {
        val sampleImage =
            "https://b.stripecdn.com/connections-statics-srv/assets/PrepaneAsset--account_numbers-capitalone-2x.gif"
        return OauthPrepane(
            title = "Sign in with Sample bank",
            subtitle = "Next, you'll be prompted to log in and connect your accounts.",
            body = Body(
                listOf(
                    Entry.Image(
                        Image(sampleImage)
                    ),
                    Entry.Text(
                        "Dynamic content placeholder that will show below image."
                    )
                )
            ),
            cta = Cta(
                icon = null,
                text = "Continue!"
            ),
            institutionIcon = Image("www.image.url"),
            partnerNotice = PartnerNotice(
                partnerIcon = Image(sampleImage),
                text = "Stripe works with partners like MX to reliably" +
                    " offer access to thousands of financial institutions." +
                    " Learn more"
            )
        )
    }
}
