package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.isLinkDs3

/** Gap between the two buttons in the Link DS 3.0 side-by-side layout. */
private val SideBySideSpacing = 8.dp

/**
 * One action in a [FooterButtons] pair. Not a state holder — construct it inline at the call site.
 */
internal class FooterButton(
    val onClick: () -> Unit,
    val enabled: Boolean,
    val loading: Boolean,
    val testTag: String,
    val content: @Composable RowScope.() -> Unit,
)

/**
 * A footer with a primary action and an optional secondary action.
 *
 * Link DS 3.0 lays the pair out side by side with equal widths — secondary on the left, primary on
 * the right — but only where [preferSideBySide] is set and a secondary action actually exists. Every
 * other case stacks them with the primary on top, which is the layout all other themes use.
 *
 * Side-by-side is opt-in per surface rather than automatic, matching iOS: the bottom sheets
 * (warmup, exit confirmation, and the partner-auth prepane when shown as a sheet) use it, while
 * full-screen panes keep the stacked layout.
 *
 * @param stackedSpacing gap between the buttons in the stacked layout. Varies by surface.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FooterButtons(
    primary: FooterButton,
    secondary: FooterButton?,
    preferSideBySide: Boolean,
    stackedSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    val sideBySide = preferSideBySide &&
        secondary != null &&
        FinancialConnectionsTheme.theme.isLinkDs3

    if (sideBySide && secondary != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(SideBySideSpacing),
        ) {
            // The outlined pill only exists in the side-by-side layout.
            FooterButton(
                button = secondary,
                type = Type.SecondaryOutlined,
                modifier = Modifier.weight(1f),
            )
            FooterButton(
                button = primary,
                type = Type.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(stackedSpacing),
        ) {
            FooterButton(
                button = primary,
                type = Type.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            secondary?.let {
                FooterButton(
                    button = it,
                    type = Type.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FooterButton(
    button: FooterButton,
    type: Type,
    modifier: Modifier,
) {
    FinancialConnectionsButton(
        onClick = button.onClick,
        type = type,
        enabled = button.enabled,
        loading = button.loading,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(button.testTag),
        content = button.content,
    )
}
