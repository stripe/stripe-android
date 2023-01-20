package com.stripe.android.financialconnections.features

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
            subtitle = pane.dataAccessNotice.subtitle?.let { toHtml(it) },
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
