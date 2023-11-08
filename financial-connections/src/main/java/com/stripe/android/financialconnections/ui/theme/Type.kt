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
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

@Immutable
@Deprecated("Use FinancialConnectionsV3Typography instead")
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
internal data class FinancialConnectionsV3Typography(
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
                style = v3Typography.headingXLarge
            )
            Text(
                text = "Heading Large",
                style = v3Typography.headingLarge
            )
            Text(
                text = "Heading Medium",
                style = v3Typography.headingMedium
            )
            Divider()
            Text(
                text = "Body Medium Emphasized",
                style = v3Typography.bodyMediumEmphasized
            )
            Text(
                text = "Body Medium",
                style = v3Typography.bodyMedium
            )
            Text(
                text = "Body Small",
                style = v3Typography.bodySmall
            )
            Divider()
            Text(
                text = "Label Large Emphasized",
                style = v3Typography.labelLargeEmphasized
            )
            Text(
                text = "Label Large",
                style = v3Typography.labelLarge
            )
            Text(
                text = "Label Medium Emphasized",
                style = v3Typography.labelMediumEmphasized
            )
            Text(
                text = "Label Medium",
                style = v3Typography.labelMedium
            )
            Text(
                text = "Label Small",
                style = v3Typography.labelSmall
            )
        }
    }
}
