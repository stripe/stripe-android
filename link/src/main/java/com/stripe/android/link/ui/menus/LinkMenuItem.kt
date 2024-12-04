package com.stripe.android.link.ui.menus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors

/**
 * An item to be displayed in a [LinkMenu].
 *
 * @property textResId The resource ID of the text of the item
 * @property isDestructive Whether this item should be rendered with the error text color
 */
internal interface LinkMenuItem {
    val textResId: Int
    val isDestructive: Boolean
}

/**
 * Displays a generic bottom sheet with the provided [items].
 *
 * @param items The list of items that implement [LinkMenuItem]
 * @param onItemPress Called when an item in the list is pressed
 */
@Composable
internal fun <T : LinkMenuItem> LinkMenu(
    modifier: Modifier = Modifier,
    items: List<T>,
    onItemPress: (T) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        for (item in items) {
            LinkBottomSheetRow(
                item = item,
                modifier = Modifier.clickable {
                    onItemPress(item)
                }
            )
        }
    }
}

@Composable
private fun <T : LinkMenuItem> LinkBottomSheetRow(
    item: T,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(MinimumTouchTargetSize)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(item.textResId),
            color = if (item.isDestructive) {
                MaterialTheme.linkColors.errorText
            } else {
                Color.Unspecified
            },
            modifier = Modifier.padding(horizontal = HorizontalPadding)
        )
    }
}
