package com.stripe.android.financialconnections.features.notice

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConnectedAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.ServerLink

internal class NoticeSheetPreviewParameterProvider :
    PreviewParameterProvider<NoticeSheetState.NoticeSheetContent> {

    override val values = sequenceOf(
        legal(),
        dataAccess(),
        dataAccessWithConnectedAccounts(),
    )

    private fun legal() = NoticeSheetState.NoticeSheetContent.Legal(
        legalDetails = legalDetails(),
    )

    private fun dataAccess() = NoticeSheetState.NoticeSheetContent.DataAccess(
        dataAccess = dataAccessNotice().copy(
            connectedAccountNotice = null,
        ),
    )

    private fun dataAccessWithConnectedAccounts() = NoticeSheetState.NoticeSheetContent.DataAccess(
        dataAccess = dataAccessNotice(),
    )

    private fun legalDetails(): LegalDetailsNotice {
        return LegalDetailsNotice(
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
        )
    }

    private fun dataAccessNotice(): DataAccessNotice {
        return DataAccessNotice(
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
        )
    }

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
