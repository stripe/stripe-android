package com.stripe.android.financialconnections.features

import com.stripe.android.financialconnections.domain.prepane.Body
import com.stripe.android.financialconnections.domain.prepane.Cta
import com.stripe.android.financialconnections.domain.prepane.Display
import com.stripe.android.financialconnections.domain.prepane.OauthPrepane
import com.stripe.android.financialconnections.domain.prepane.PartnerNotice
import com.stripe.android.financialconnections.domain.prepane.Text
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.LegalDetailsBody
import com.stripe.android.financialconnections.model.LegalDetailsNotice

internal object MarkdownParser {
    private val markDownToHtmlRegex = listOf(
        // bold, italics rules
        "\\*\\*\\s?([^\\n]+)\\*\\*".toRegex() to "<b>$1</b>",
        "__([^_]+)__".toRegex() to "<b>$1</b>",

        // links
        "\\[([^]]+)]\\(([^)]+)\\)".toRegex() to "<a href=\"$2\">$1</a>"
    )

    internal fun toHtml(string: String): String {
        var newText = string
        markDownToHtmlRegex.forEach { (regex, replacement) ->
            newText = newText.replace(regex, replacement)
        }
        return newText
    }

    internal fun toHtml(display: Display) = Display(
        Text(
            oauthPrepane = toHtml(display.text.oauthPrepane),
        )
    )

    private fun toHtml(prepane: OauthPrepane) = OauthPrepane(
        body = prepane.body.map { item ->
            when (item) {
                is Body.Image -> Body.Image(
                    type = item.type,
                    content = item.content
                )
                is Body.Text -> Body.Text(
                    type = item.type,
                    content = toHtml(item.content)
                )
            }
        },
        cta = Cta(
            text = prepane.cta.text,
            icon = prepane.cta.icon
        ),
        institutionIcon = prepane.institutionIcon,
        title = toHtml(prepane.title),
        partnerNotice = PartnerNotice(
            text = toHtml(prepane.partnerNotice.text),
            partnerIcon = prepane.partnerNotice.partnerIcon
        )
    )

    internal fun toHtml(pane: ConsentPane): ConsentPane = ConsentPane(
        title = toHtml(pane.title),
        body = ConsentPaneBody(
            bullets = pane.body.bullets.map { bullet ->
                Bullet(
                    icon = bullet.icon,
                    content = bullet.content?.let { toHtml(it) },
                    title = bullet.title?.let { toHtml(it) }
                )
            }
        ),
        belowCta = pane.belowCta?.let { toHtml(it) },
        aboveCta = toHtml(pane.aboveCta),
        cta = toHtml(pane.cta),
        dataAccessNotice = DataAccessNotice(
            title = toHtml(pane.dataAccessNotice.title),
            body = DataAccessNoticeBody(
                bullets = pane.dataAccessNotice.body.bullets.map { bullet ->
                    Bullet(
                        icon = bullet.icon,
                        content = bullet.content?.let { toHtml(it) },
                        title = bullet.title?.let { toHtml(it) }
                    )
                }
            ),
            learnMore = toHtml(pane.dataAccessNotice.learnMore),
            cta = toHtml(pane.dataAccessNotice.cta),
            connectedAccountNotice = pane.dataAccessNotice.connectedAccountNotice?.let { toHtml(it) }
        ),
        legalDetailsNotice = LegalDetailsNotice(
            title = toHtml(pane.legalDetailsNotice.title),
            body = LegalDetailsBody(
                bullets = pane.legalDetailsNotice.body.bullets.map { bullet ->
                    Bullet(
                        icon = bullet.icon,
                        content = bullet.content?.let { toHtml(it) },
                        title = bullet.title?.let { toHtml(it) }
                    )
                }
            ),
            cta = toHtml(pane.legalDetailsNotice.cta),
            learnMore = toHtml(pane.legalDetailsNotice.learnMore)
        )
    )
}
