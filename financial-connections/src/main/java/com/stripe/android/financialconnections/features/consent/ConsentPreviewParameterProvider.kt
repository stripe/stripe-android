package com.stripe.android.financialconnections.features.consent

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.core.Result
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConnectedAccessNotice
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.ServerLink

@OptIn(ExperimentalMaterialApi::class)
internal class ConsentPreviewParameterProvider :
    PreviewParameterProvider<Pair<ModalBottomSheetValue, ConsentState>> {
    override val values = sequenceOf(
        ModalBottomSheetValue.Hidden to withPlatformLogos(),
        ModalBottomSheetValue.Hidden to withConnectedAccountLogos(),
        ModalBottomSheetValue.Hidden to manualEntryPlusMicrodeposits(),
        ModalBottomSheetValue.Expanded to withDataBottomSheet(),
        ModalBottomSheetValue.Expanded to withLegalDetailsBottomSheet(),
        ModalBottomSheetValue.Expanded to withDataBottomSheetAndConnectedAccount()
    )

    override val count: Int
        get() = super.count

    private fun withPlatformLogos() =
        ConsentState(
            consent = Result.Success(
                ConsentState.Payload(
                    consent = sampleConsent().copy(belowCta = null),
                    merchantLogos = listOf(
                        "www.logo1.com",
                        "www.logo2.com"
                    ),
                    shouldShowMerchantLogos = true
                )
            )
        )

    private fun withConnectedAccountLogos() =
        ConsentState(
            consent = Result.Success(
                ConsentState.Payload(
                    consent = sampleConsent().copy(belowCta = null),
                    merchantLogos = listOf(
                        "www.logo1.com",
                        "www.logo2.com",
                        "www.logo3.com",
                    ),
                    shouldShowMerchantLogos = true
                )
            )
        )

    private fun manualEntryPlusMicrodeposits() = ConsentState(
        consent = Result.Success(
            ConsentState.Payload(
                consent = sampleConsent(),
                merchantLogos = listOf(
                    "www.logo1.com",
                    "www.logo2.com"
                ),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun withDataBottomSheet() = ConsentState(
        currentBottomSheet = ConsentState.BottomSheetContent.DATA,
        consent = Result.Success(
            ConsentState.Payload(
                consent = sampleConsent().copy(
                    dataAccessNotice = sampleConsent().dataAccessNotice.copy(
                        connectedAccountNotice = null
                    )
                ),
                merchantLogos = listOf(
                    "www.logo1.com",
                    "www.logo2.com"
                ),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun withDataBottomSheetAndConnectedAccount() = ConsentState(
        currentBottomSheet = ConsentState.BottomSheetContent.DATA,
        consent = Result.Success(
            ConsentState.Payload(
                consent = sampleConsent(),
                merchantLogos = listOf(
                    "www.logo1.com",
                    "www.logo2.com"
                ),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun withLegalDetailsBottomSheet() = ConsentState(
        currentBottomSheet = ConsentState.BottomSheetContent.LEGAL,
        consent = Result.Success(
            ConsentState.Payload(
                consent = sampleConsent().copy(belowCta = null),
                merchantLogos = listOf(
                    "www.logo1.com",
                    "www.logo2.com"
                ),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun sampleConsent(): ConsentPane = ConsentPane(
        title = "Goldilocks uses Stripe to link your accounts",
        body = ConsentPaneBody(
            bullets = listOf(
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow Goldilocks to access only the data requested",
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
            )
        ),
        aboveCta = "Manually verify instead (takes 1-2 business days)",
        belowCta = "Stripe will allow Goldilocks to access only the data requested." +
            " We never share your login details with them.",
        cta = "Agree",
        dataAccessNotice = DataAccessNotice(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Goldilocks uses Stripe to link your accounts",
            subtitle = "Goldilocks will use your account and routing number, balances and transactions:",
            body = DataAccessNoticeBody(
                bullets = bullets()
            ),
            disclaimer = "Learn more about data access",
            connectedAccountNotice = ConnectedAccessNotice(
                subtitle = "Connected account placeholder",
                body = DataAccessNoticeBody(
                    bullets = bullets()
                )
            ),
            cta = "OK"
        ),
        legalDetailsNotice = LegalDetailsNotice(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Terms and privacy policy",
            subtitle = "Stripe only uses your data and credentials as described in the Terms, " +
                "such as to improve its services, manage loss, and mitigate fraud.",
            body = LegalDetailsBody(
                links = listOf(
                    ServerLink(
                        title = "Terms",
                    ),
                    ServerLink(
                        title = "Privacy Policy",
                    ),
                )
            ),
            disclaimer = "Learn more",
            cta = "OK"
        ),
    )

    private fun bullets() = listOf(
        Bullet(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Account details",
            content = null
        ),
        Bullet(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Balances",
            content = null
        ),
        Bullet(
            icon = Image("https://www.cdn.stripe.com/12321312321.png"),
            title = "Transactions",
            content = null
        ),
    )
}
