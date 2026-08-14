package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors

/** Corner radius of the Link DS 3.0 grouped card. */
internal val GroupedCardCornerRadius = 12.dp

/**
 * Where a row sits inside a Link DS 3.0 grouped card. A null [GroupPosition] means the row is
 * standalone and should keep whatever treatment its theme gives it.
 */
internal data class GroupPosition(
    val isFirst: Boolean,
    val isLast: Boolean,
)

/**
 * Leading inset of the separator between grouped card rows. Lines up with where row text starts:
 * 16.dp of row padding + a 56.dp [IconSize.Medium] icon + a 20.dp gap.
 */
internal val GroupedCardSeparatorInset = 92.dp

/**
 * The Link DS 3.0 loading state for a grouped list: [rowCount] shimmer rows sitting directly against
 * each other inside a single rounded card. The rows have no corner radius of their own — the card
 * clips them — and no spacing between them.
 *
 * Other themes show spaced rows with individual corners instead, so callers should only reach for
 * this in the Link DS 3.0 theme.
 */
@Composable
internal fun GroupedShimmerCard(
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    rowCount: Int = 2,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupedCardCornerRadius))
            .background(colors.iconBackground)
    ) {
        repeat(rowCount) {
            LoadingShimmerEffect { shimmer ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .background(shimmer)
                )
            }
        }
    }
}

/**
 * Applies one row of the Link DS 3.0 grouped card: a run of adjacent rows sharing a single grey
 * surface, with only the outer corners rounded and hairline separators in between.
 *
 * iOS does this with a floating background view whose frame is recomputed from the table's row rects
 * on every scroll event. Applying the surface per row instead produces the same pixels — the rows are
 * adjacent, so their backgrounds read as continuous — while needing no measurement, so the card grows
 * and shrinks with its content for free.
 *
 * @param isFirst whether this is the top row, which owns the top corners.
 * @param isLast whether this is the bottom row, which owns the bottom corners. Where a footer row
 *        ("Search for more banks", "Can't find your bank?") is present, that footer is the last row.
 * @param separatorInset leading inset of the separator drawn along this row's top edge. Pass
 *        [Dp.Unspecified] to suppress it.
 */
@Composable
internal fun Modifier.groupedCardSurface(
    isFirst: Boolean,
    isLast: Boolean,
    separatorInset: Dp = GroupedCardSeparatorInset,
): Modifier {
    val separatorColor = colors.borderNeutral
    // Only the outer corners are rounded, so adjacent rows butt together seamlessly.
    val shape: Shape = RoundedCornerShape(
        topStart = if (isFirst) GroupedCardCornerRadius else 0.dp,
        topEnd = if (isFirst) GroupedCardCornerRadius else 0.dp,
        bottomStart = if (isLast) GroupedCardCornerRadius else 0.dp,
        bottomEnd = if (isLast) GroupedCardCornerRadius else 0.dp,
    )
    // The separator sits on the row's top edge, so the first row never draws one.
    val drawSeparator = isFirst.not() && separatorInset != Dp.Unspecified
    return this
        // Clip before the background so anything drawn further down the modifier chain — notably the
        // press ripple from the row's clickable — is bounded by the card's rounded corners too.
        .clip(shape)
        .background(color = colors.iconBackground)
        .then(
            if (drawSeparator) {
                Modifier.drawWithContent {
                    drawContent()
                    drawLine(
                        color = separatorColor,
                        start = Offset(x = separatorInset.toPx(), y = 0f),
                        end = Offset(x = size.width, y = 0f),
                        // A single physical pixel, matching iOS's `1 / nativeScale` hairline.
                        strokeWidth = 1f,
                    )
                }
            } else {
                Modifier
            }
        )
}
