package com.stripe.android.financialconnections.ui

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConnectedAccessNotice
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.model.ServerLink
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.model.ConsumerSession

internal object MacrobenchmarkMocks {
    const val DEFAULT_PUBLISHABLE_KEY = "pk_test_vOo1umqsYxSrP5UXfOeL3ecm"
    const val DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET = "las_client_secret_asdf1234"

    const val HOSTED_AUTH_URL = "https://stripe.com/auth/flow/start"
    const val SUCCESS_URL = "stripe-auth://link-accounts/success"
    const val CANCEL_URL = "stripe-auth://link-accounts/cancel"

    fun nativeArgs() = FinancialConnectionsSheetNativeActivityArgs(
        FinancialConnectionsSheet.Configuration(
            financialConnectionsSessionClientSecret = DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            publishableKey = DEFAULT_PUBLISHABLE_KEY
        ),
        syncResponse()
    )

    fun syncResponse(
        manifest: FinancialConnectionsSessionManifest = sessionManifest(),
        visual: VisualUpdate = visual()
    ) = SynchronizeSessionResponse(
        manifest = manifest,
        text = TextUpdate(
            consent = sampleConsent()
        ),
        visual = visual
    )

    fun visual() = VisualUpdate(
        reducedBranding = false,
        reducedManualEntryProminenceInErrors = false,
        merchantLogos = emptyList()
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

    fun financialConnectionsSessionNoAccounts() = FinancialConnectionsSession(
        clientSecret = "las_1234567890",
        id = DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        accountsNew = FinancialConnectionsAccountList(
            data = emptyList(),
            hasMore = false,
            url = "url",
            count = 0
        ),
        livemode = true
    )

    fun sessionManifest() = FinancialConnectionsSessionManifest(
        allowManualEntry = true,
        consentRequired = true,
        customManualEntryHandling = true,
        disableLinkMoreAccounts = true,
        id = "1234",
        instantVerificationDisabled = true,
        institutionSearchDisabled = true,
        livemode = true,
        manualEntryUsesMicrodeposits = true,
        mobileHandoffEnabled = true,
        nextPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
        permissions = emptyList(),
        product = FinancialConnectionsSessionManifest.Product.STRIPE_CARD,
        singleAccount = true,
        useSingleSortSearch = true,
        manualEntryMode = ManualEntryMode.AUTOMATIC,
        successUrl = SUCCESS_URL,
        cancelUrl = CANCEL_URL,
        hostedAuthUrl = HOSTED_AUTH_URL
    )

    fun institution() = FinancialConnectionsInstitution(
        id = "id",
        name = "name",
        url = "url",
        featured = true,
        featuredOrder = null,
        mobileHandoffCapable = false
    )

    fun consumerSession() = ConsumerSession(
        clientSecret = "clientSecret",
        emailAddress = "test@test.com",
        redactedPhoneNumber = "+1***123",
        verificationSessions = emptyList(),
        authSessionClientSecret = null,
        publishableKey = null
    )
}
