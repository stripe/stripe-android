package com.stripe.link.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Controls whether a [ListItem] is visually and semantically enabled or disabled.
 */
enum class ListItemEnablement {
    /** Fully enabled; click targets are active. */
    Enabled,

    /** Visually dimmed and not clickable. */
    Disabled,

    /**
     * Not clickable but retains its normal appearance. Use for rows that contain
     * their own interactive controls (e.g. [LinkSwitch]) where the row itself
     * should not be tappable.
     */
    DisabledWithoutAppearance,
}

/**
 * Holds per-item configuration that can be shared across a group of [ListItem]s.
 */
data class ListItemConfig(
    val useInsetStyle: Boolean = false,
)

/**
 * A standard list row with an optional leading icon, label, sublabel, and trailing indicator.
 *
 * @param label Primary text for the row.
 * @param modifier [Modifier] applied to the outermost container.
 * @param sublabel Secondary text displayed below [label].
 * @param accessibleSublabel Accessibility-only description for the sublabel region.
 * @param detailsContent Slot rendered to the right of the label column (e.g. a switch).
 * @param leadingIcon Slot rendered before the label column.
 * @param trailingIndicator Slot rendered at the trailing edge (e.g. a chevron).
 * @param useInsetStyle When `true`, adds leading inset padding to align with inset-style rows.
 * @param config Additional configuration.
 * @param enabledState Controls click-ability and visual state of the row.
 * @param onClick Invoked when the row is tapped.
 * @param onLongClick Invoked when the row is long-pressed (optional).
 */
@Composable
fun ListItem(
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    accessibleSublabel: String? = null,
    detailsContent: (@Composable RowScope.() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIndicator: (@Composable () -> Unit)? = null,
    useInsetStyle: Boolean = false,
    config: ListItemConfig = ListItemConfig(),
    enabledState: ListItemEnablement = ListItemEnablement.Enabled,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val isEnabled = enabledState == ListItemEnablement.Enabled
    val alpha = if (enabledState == ListItemEnablement.Disabled) ContentAlpha.disabled else 1f

    val clickableModifier = when (enabledState) {
        ListItemEnablement.Enabled -> if (onLongClick != null) {
            modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
            modifier.clickable(role = Role.Button, onClick = onClick)
        }
        ListItemEnablement.Disabled,
        ListItemEnablement.DisabledWithoutAppearance -> modifier
    }

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(
                start = if (useInsetStyle || config.useInsetStyle) ListItemTokens.insetStart else ListItemTokens.start,
                end = ListItemTokens.end,
                top = ListItemTokens.vertical,
                bottom = ListItemTokens.vertical,
            )
            .then(
                if (accessibleSublabel != null) {
                    Modifier.semantics { contentDescription = accessibleSublabel }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(ListItemTokens.leadingIconSpacing))
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            ListItemLabel(label)
            if (sublabel != null) {
                ListItemSublabel(sublabel)
            }
        }

        if (detailsContent != null) {
            detailsContent()
        }

        if (trailingIndicator != null) {
            Spacer(modifier = Modifier.width(ListItemTokens.trailingSpacing))
            trailingIndicator()
        }
    }
}

/**
 * A [ListItem] overload with **no** `onClick` parameter. The row itself is not
 * tappable — use this when the row contains its own interactive control such as
 * a [LinkSwitch].
 *
 * Delegates to the [enabledState] overload with
 * [ListItemEnablement.DisabledWithoutAppearance] so that the row looks fully
 * enabled but does not consume tap events at the row level.
 */
@Composable
fun ListItem(
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    accessibleSublabel: String? = null,
    detailsContent: (@Composable RowScope.() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIndicator: (@Composable () -> Unit)? = null,
    useInsetStyle: Boolean = false,
    config: ListItemConfig = ListItemConfig(),
) {
    ListItem(
        label = label,
        modifier = modifier,
        sublabel = sublabel,
        accessibleSublabel = accessibleSublabel,
        detailsContent = detailsContent,
        leadingIcon = leadingIcon,
        trailingIndicator = trailingIndicator,
        useInsetStyle = useInsetStyle,
        config = config,
        enabledState = ListItemEnablement.DisabledWithoutAppearance,
        onClick = {},
    )
}

// ---------------------------------------------------------------------------
// Internal label composables
// ---------------------------------------------------------------------------

@Composable
private fun ListItemLabel(text: String) {
    androidx.compose.material.Text(
        text = text,
        style = androidx.compose.material.MaterialTheme.typography.body1,
    )
}

@Composable
private fun ListItemSublabel(text: String) {
    androidx.compose.material.Text(
        text = text,
        style = androidx.compose.material.MaterialTheme.typography.body2,
        color = androidx.compose.material.MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
    )
}

// ---------------------------------------------------------------------------
// Design tokens
// ---------------------------------------------------------------------------

private object ListItemTokens {
    val start = androidx.compose.ui.unit.dp * 16
    val insetStart = androidx.compose.ui.unit.dp * 56
    val end = androidx.compose.ui.unit.dp * 16
    val vertical = androidx.compose.ui.unit.dp * 12
    val leadingIconSpacing = androidx.compose.ui.unit.dp * 12
    val trailingSpacing = androidx.compose.ui.unit.dp * 8
}
