@file:Suppress("MatchingDeclarationName", "ktlint:filename")

package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
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
    val captionCodeEmphasized: TextStyle
)

@Immutable
internal data class FinancialConnectionsAuthFlowV3Typography(
    val headingXLarge: TextStyle,
    val headingLarge: TextStyle,
    val headingMedium: TextStyle,
    val bodyMediumEmphasized: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLargeEmphasized: TextStyle,
    val labelLarge: TextStyle,
    val labelMediumEmphasized: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

@Preview(group = "Components", name = "Type")
@Composable
internal fun TypePreview() {
    FinancialConnectionsPreview {
        Column {
            Text(
                text = "Heading XLarge",
                style = FinancialConnectionsTheme.authFlowV3Typography.headingXLarge
            )
            Text(
                text = "Heading Large",
                style = FinancialConnectionsTheme.authFlowV3Typography.headingLarge
            )
            Text(
                text = "Heading Medium",
                style = FinancialConnectionsTheme.authFlowV3Typography.headingMedium
            )
            Divider()
            Text(
                text = "Body Medium Emphasized",
                style = FinancialConnectionsTheme.authFlowV3Typography.bodyMediumEmphasized
            )
            Text(
                text = "Body Medium",
                style = FinancialConnectionsTheme.authFlowV3Typography.bodyMedium
            )
            Text(
                text = "Body Small",
                style = FinancialConnectionsTheme.authFlowV3Typography.bodySmall
            )
            Divider()
            Text(
                text = "Label Large Emphasized",
                style = FinancialConnectionsTheme.authFlowV3Typography.labelLargeEmphasized
            )
            Text(
                text = "Label Large",
                style = FinancialConnectionsTheme.authFlowV3Typography.labelLarge
            )
            Text(
                text = "Label Medium Emphasized",
                style = FinancialConnectionsTheme.authFlowV3Typography.labelMediumEmphasized
            )
            Text(
                text = "Label Medium",
                style = FinancialConnectionsTheme.authFlowV3Typography.labelMedium
            )
            Text(
                text = "Label Small",
                style = FinancialConnectionsTheme.authFlowV3Typography.labelSmall
            )
        }
    }
}
