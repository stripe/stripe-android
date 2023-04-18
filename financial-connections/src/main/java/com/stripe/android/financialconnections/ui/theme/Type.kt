@file:Suppress("MatchingDeclarationName", "ktlint:filename")

package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview

@Immutable
internal data class FinancialConnectionsTypography(
    val subtitle: TextStyle,
    val subtitleEmphasized: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val kicker: TextStyle,
    val body: TextStyle,
    val bodyEmphasized: TextStyle,
    val detail: TextStyle,
    val detailEmphasized: TextStyle,
    val caption: TextStyle,
    val captionEmphasized: TextStyle,
    val captionTight: TextStyle,
    val captionTightEmphasized: TextStyle,
    val bodyCode: TextStyle,
    val bodyCodeEmphasized: TextStyle,
    val captionCode: TextStyle,
    val captionCodeEmphasized: TextStyle,

)

@Preview(group = "Components", name = "Type")
@Composable
internal fun TypePreview() {
    FinancialConnectionsPreview {
        Column {
            Text(
                text = "subtitle",
                style = FinancialConnectionsTheme.typography.subtitle
            )
            Text(
                text = "subtitleEmphasized",
                style = FinancialConnectionsTheme.typography.subtitleEmphasized
            )
            Text(
                text = "heading",
                style = FinancialConnectionsTheme.typography.heading
            )
            Text(
                text = "subheading",
                style = FinancialConnectionsTheme.typography.subheading
            )
            Text(
                text = "KICKER",
                style = FinancialConnectionsTheme.typography.kicker
            )
            Text(
                text = "body",
                style = FinancialConnectionsTheme.typography.body
            )
            Text(
                text = "bodyEmphasized",
                style = FinancialConnectionsTheme.typography.bodyEmphasized
            )
            Text(
                text = "detail",
                style = FinancialConnectionsTheme.typography.detail
            )
            Text(
                text = "detailEmphasized",
                style = FinancialConnectionsTheme.typography.detailEmphasized
            )
            Text(
                text = "caption",
                style = FinancialConnectionsTheme.typography.caption
            )
            Text(
                text = "captionEmphasized",
                style = FinancialConnectionsTheme.typography.captionEmphasized
            )
            Text(
                text = "captionTight",
                style = FinancialConnectionsTheme.typography.captionTight
            )
            Text(
                text = "captionTightEmphasized",
                style = FinancialConnectionsTheme.typography.captionTightEmphasized
            )
        }
    }
}
