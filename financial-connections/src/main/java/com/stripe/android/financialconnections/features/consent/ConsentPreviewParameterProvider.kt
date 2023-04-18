package com.stripe.android.financialconnections.features.consent

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice

@OptIn(ExperimentalMaterialApi::class)
internal class ConsentPreviewParameterProvider :
    PreviewParameterProvider<Pair<ModalBottomSheetValue, ConsentState>> {
    override val values = sequenceOf(
        ModalBottomSheetValue.Hidden to canonical(),
        ModalBottomSheetValue.Hidden to withNoLogos(),
        ModalBottomSheetValue.Hidden to withPlatformLogos(),
        ModalBottomSheetValue.Hidden to withConnectedAccountLogos(),
        ModalBottomSheetValue.Hidden to manualEntryPlusMicrodeposits(),
        ModalBottomSheetValue.Expanded to withDataBottomSheet(),
        ModalBottomSheetValue.Expanded to withLegalDetailsBottomSheet()
    )

    override val count: Int
        get() = super.count

    private fun canonical() =
        ConsentState(
            consent = Success(
                ConsentState.Payload(
                    consent = sampleConsent().copy(belowCta = null),
                    merchantLogos = emptyList(),
                    shouldShowMerchantLogos = false
                )
            )
        )

    private fun withNoLogos() =
        ConsentState(
            consent = Success(
                ConsentState.Payload(
                    consent = sampleConsent().copy(belowCta = null),
                    merchantLogos = emptyList(),
                    shouldShowMerchantLogos = true
                )
            )
        )

    private fun withPlatformLogos() =
        ConsentState(
            consent = Success(
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
            consent = Success(
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
        consent = Success(
            ConsentState.Payload(
                consent = sampleConsent(),
                merchantLogos = emptyList(),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun withDataBottomSheet() = ConsentState(
        currentBottomSheet = ConsentState.BottomSheetContent.DATA,
        consent = Success(
            ConsentState.Payload(
                consent = sampleConsent(),
                merchantLogos = emptyList(),
                shouldShowMerchantLogos = false
            )
        )
    )

    private fun withLegalDetailsBottomSheet() = ConsentState(
        currentBottomSheet = ConsentState.BottomSheetContent.LEGAL,
        consent = Success(
            ConsentState.Payload(
                consent = sampleConsent().copy(belowCta = null),
                merchantLogos = emptyList(),
                shouldShowMerchantLogos = false
            )
        )
    )

    @Suppress("LongMethod")
    private fun sampleConsent(): ConsentPane = ConsentPane(
        title = "Goldilocks works with Stripe to link your accounts",
        body = ConsentPaneBody(
            bullets = listOf(
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow Goldilocks to access only the data requested",
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow Goldilocks to access only the data requested",
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
                Bullet(
                    icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow Goldilocks to access only the data requested",
                    title = "Stripe will allow Goldilocks to access only the data requested"
                ),
            )
        ),
        aboveCta = "Manually verify instead (takes 1-2 business days)",
        belowCta = "Stripe will allow Goldilocks to access only the data requested." +
            " We never share your login details with them.",
        cta = "Agree",
        dataAccessNotice = DataAccessNotice(
            title = "Goldilocks works with Stripe to link your accounts",
            subtitle = "Goldilocks will use your account and routing number, balances and transactions:",
            body = DataAccessNoticeBody(
                bullets = listOf(
                    Bullet(
                        icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                        title = "Account details",
                        content = "Account number, routing number, account type, account nickname."
                    ),
                    Bullet(
                        icon = Image("https://www.cdn.stripe.com/12321312321.png"),
                        title = "Account details",
                        content = "Account number, routing number, account type, account nickname."
                    ),
                )
            ),
            learnMore = "Learn more about data access",
            connectedAccountNotice = "Connected account placeholder",
            cta = "OK"
        ),
        legalDetailsNotice = LegalDetailsNotice(
            title = "Stripe uses your account data as described in the Terms, including:",
            body = LegalDetailsBody(
                bullets = listOf(
                    Bullet(
                        content = "To improve our services"
                    ),
                    Bullet(
                        content = "To manage fraud and loss risk of transactions"
                    ),
                )
            ),
            learnMore = "Learn more",
            cta = "OK"
        ),
    )
}
