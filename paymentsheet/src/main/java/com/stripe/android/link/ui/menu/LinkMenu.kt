package com.stripe.android.link.ui.menu

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.MinimumTouchTargetSize
import com.stripe.android.link.theme.linkColors

/**
 * Displays a generic bottom sheet with the provided [items].
 *
 * @param items The list of items that implement [LinkMenuItem]
 * @param onItemPress Called when an item in the list is pressed
 */
@Composable
internal fun LinkMenu(
    modifier: Modifier = Modifier,
    items: List<LinkMenuItem>,
    onItemPress: (LinkMenuItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        for (item in items) {
            LinkBottomSheetRow(
                item = item,
                modifier = Modifier
                    .testTag(item.testTag)
                    .clickable {
                        onItemPress(item)
                    }
            )
        }
    }
}

@Composable
private fun LinkBottomSheetRow(
    item: LinkMenuItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(MinimumTouchTargetSize)
            .fillMaxWidth()
    ) {
        Text(
            text = item.text.resolve(context),
            color = if (item.isDestructive) {
                MaterialTheme.linkColors.errorText
            } else {
                Color.Unspecified
            },
            modifier = Modifier.padding(horizontal = HorizontalPadding)
        )
    }
}
