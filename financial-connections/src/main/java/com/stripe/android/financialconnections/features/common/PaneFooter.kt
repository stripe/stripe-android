package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

/**
 * A Composable function that displays a Pane footer with a given elevation and content.
 *
 * @param elevation The elevation of the Surface that wraps the Column content.
 * @param content of the footer.
 */
@Composable
internal fun PaneFooter(
    elevation: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = FinancialConnectionsTheme.colors.backgroundSurface,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 24.dp
            ),
            content = content
        )
    }
}
