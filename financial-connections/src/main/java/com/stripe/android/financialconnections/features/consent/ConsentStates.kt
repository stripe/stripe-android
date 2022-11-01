package com.stripe.android.financialconnections.features.consent

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.Image

internal class ConsentStates : PreviewParameterProvider<ConsentState> {
    override val values = sequenceOf(
        canonical()
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun canonical() = ConsentState(consent = Success(sampleConsent()))
        fun manualEntryPlusMicrodeposits() = canonical().copy(
            manualEntryEnabled = true,
            manualEntryShowBusinessDaysNotice = true
        )

        fun sampleConsent(): ConsentPane = ConsentPane(
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
            )
        )
    }
}
