package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography

@Immutable
internal data class FinancialConnectionsTypography(
    val headingXLarge: TextStyle,
    val headingXLargeSubdued: TextStyle,
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
        Column(
            modifier = Modifier.background(colors.background)
        ) {
            Text(
                text = "Heading XLarge",
                style = typography.headingXLarge
            )
            Text(
                text = "Heading XLarge Subdued",
                style = typography.headingXLargeSubdued
            )
            Text(
                text = "Heading Large",
                style = typography.headingLarge
            )
            Text(
                text = "Heading Medium",
                style = typography.headingMedium
            )
            Divider()
            Text(
                text = "Body Medium Emphasized",
                style = typography.bodyMediumEmphasized
            )
            Text(
                text = "Body Medium",
                style = typography.bodyMedium
            )
            Text(
                text = "Body Small",
                style = typography.bodySmall
            )
            Divider()
            Text(
                text = "Label Large Emphasized",
                style = typography.labelLargeEmphasized
            )
            Text(
                text = "Label Large",
                style = typography.labelLarge
            )
            Text(
                text = "Label Medium Emphasized",
                style = typography.labelMediumEmphasized
            )
            Text(
                text = "Label Medium",
                style = typography.labelMedium
            )
            Text(
                text = "Label Small",
                style = typography.labelSmall
            )
        }
    }
}
