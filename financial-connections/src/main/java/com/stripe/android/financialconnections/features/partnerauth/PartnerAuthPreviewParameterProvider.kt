@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.AuthStatus
import com.stripe.android.financialconnections.model.Body
import com.stripe.android.financialconnections.model.Cta
import com.stripe.android.financialconnections.model.Display
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession.Flow
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
        browserLoading(),
        completing()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = SharedPartnerAuthState(
        payload = Success(
            SharedPartnerAuthState.Payload(
                institution = institution(),
                authSession = session(),
                isStripeDirect = false
            )
        ),
        authenticationStatus = AuthStatus.Uninitialized,
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun browserLoading() = SharedPartnerAuthState(
        payload = Success(
            SharedPartnerAuthState.Payload(
                institution = institution(),
                authSession = session(),
                isStripeDirect = false
            )
        ),
        // While browser is showing, show the "pending prepane".
        authenticationStatus = AuthStatus.Pending,
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun completing() = SharedPartnerAuthState(
        payload = Success(
            SharedPartnerAuthState.Payload(
                institution = institution(),
                authSession = session(),
                isStripeDirect = false
            )
        ),
        // While authentication is completing, show a loading indicator.
        authenticationStatus = AuthStatus.Completing,
        viewEffect = null,
        activeAuthSession = null,
        pane = Pane.PARTNER_AUTH
    )

    private fun institution() = FinancialConnectionsInstitution(
        id = "id",
        name = "name",
        url = "url",
        featured = true,
        icon = null,
        logo = null,
        featuredOrder = null,
        mobileHandoffCapable = false
    )

    private fun session() = FinancialConnectionsAuthorizationSession(
        flow = Flow.FINICITY_CONNECT_V2_OAUTH.name,
        showPartnerDisclosure = true,
        _isOAuth = true,
        nextPane = Pane.PARTNER_AUTH,
        id = "1234",
        display = Display(
            TextUpdate(
                oauthPrepane = oauthPrepane(),
                oauthPrepanePending = oauthPrepanePending(),
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

    private fun oauthPrepanePending() = OauthPrepane(
        title = "Continue linking your Test OAuth Institution account",
        body = Body(
            listOf(
                Entry.Text(
                    "You haven’t finished linking your account. Click Continue to finish the process."
                ),
            )
        ),
        cta = Cta(
            icon = null,
            text = "Continue"
        ),
        institutionIcon = null,
    )
}
