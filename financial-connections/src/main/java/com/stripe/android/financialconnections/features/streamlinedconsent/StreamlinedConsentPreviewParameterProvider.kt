package com.stripe.android.financialconnections.features.streamlinedconsent

import Alignment
import FinancialConnectionsGenericInfoScreen
import FinancialConnectionsGenericInfoScreen.Body
import FinancialConnectionsGenericInfoScreen.Body.Entry.Bullets.GenericBulletPoint
import FinancialConnectionsGenericInfoScreen.Footer
import FinancialConnectionsGenericInfoScreen.Header
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.StreamlinedConsentPane
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success

internal class StreamlinedConsentPreviewParameterProvider : PreviewParameterProvider<StreamlinedConsentState> {

    override val values = sequenceOf(
        canonical(),
        acceptingConsent(),
    )

    private fun canonical() = StreamlinedConsentState(
        payload = Success(
            StreamlinedConsentState.Payload(
                streamlinedConsentPane = streamlinedConsentPane(),
                dataAccessNotice = null,
            )
        ),
    )

    private fun acceptingConsent() = canonical().copy(
        acceptConsent = Loading(),
    )

    private fun streamlinedConsentPane() = StreamlinedConsentPane(
        screen = FinancialConnectionsGenericInfoScreen(
            id = "streamlined_consent",
            header = Header(
                title = "Log in to Test Institution",
                icon = Image(
                    // Real institution logos aren't BrandIcon assets, so the header must not rely on the URL.
                    default = "https://example.com/institution-logo.png"
                ),
                alignment = Alignment.Center,
            ),
            body = Body(
                entries = listOf(
                    Body.Entry.Bullets(
                        id = "streamlined_consent_body_text",
                        bullets = listOf(
                            GenericBulletPoint(
                                id = "ease_of_use",
                                icon = Image(
                                    default = "$ASSETS_BASE_URL/SailIcon--link-primary-3x.png"
                                ),
                                content = "Connect your account in seconds",
                            ),
                            GenericBulletPoint(
                                id = "data_protection",
                                icon = Image(
                                    default = "$ASSETS_BASE_URL/SailIcon--lock-primary-3x.png"
                                ),
                                content = "Your data is encrypted. Sample Merchant may retrieve " +
                                    "<a href=\"stripe://data-access-notice\">account data</a>.",
                            ),
                        ),
                    ),
                ),
            ),
            footer = Footer(
                disclaimer = "By continuing, you agree to the " +
                    "<a href=\"stripe://legal-details-notice\">Terms and Privacy Policy</a>",
                primaryCta = Footer.GenericInfoAction(
                    id = "continue",
                    label = "Agree and continue",
                ),
                belowCta = "<a href=\"stripe://manual-entry\">Manually verify instead</a> " +
                    "(may take 1-2 business days)",
            ),
        ),
        dataAccessNotice = null,
        legalDetailsNotice = LegalDetailsNotice(
            title = "Terms and Privacy Policy",
            body = LegalDetailsBody(links = emptyList()),
            cta = "OK",
        ),
        moreInfoNotice = null,
    )
}

private const val ASSETS_BASE_URL = "https://b.stripecdn.com/connections-statics-srv/assets"
