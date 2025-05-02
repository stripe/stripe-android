package com.stripe.android.link.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.strings.resolve

/**
 * Payload for the [LinkMenuV2] component.
 */
internal data class MenuPayload(
    val title: ResolvableString,
    val items: List<MenuItem>
) {
    data class MenuItem(
        val title: ResolvableString,
        val icon: Int,
        val testTag: String?,
        val onClick: () -> Unit
    )
}

/**
 * Displays a generic bottom sheet with the provided payload.
 *
 * @param payload The payload containing the title and items to be displayed in the menu
 * @param onClose Called when the close button is pressed
 */
@Composable
internal fun LinkMenuV2(
    payload: MenuPayload,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        MenuTitle(
            title = payload.title,
            onClose = onClose
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = LinkTheme.colors.componentBorder,
                    shape = LinkTheme.shapes.large
                )
                .clip(LinkTheme.shapes.large)
        ) {
            payload.items.forEach {
                MenuItemRow(it)
                Divider(
                    color = LinkTheme.colors.componentBorder
                )
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MenuTitle(title: ResolvableString, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(32.dp))

        Text(
            text = title.resolve(),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, true)
        )

        IconButton(
            onClick = onClose,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuPayload.MenuItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { item.onClick() }
            .padding(16.dp)
            .background(LinkTheme.colors.componentBackground)
            .also { if (item.testTag != null) it.testTag(item.testTag) }
    ) {
        IconWithBackground(
            itemIcon = item.icon,
        )
        Spacer(
            modifier = Modifier.size(16.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f),
            text = item.title.resolve(),
            style = MaterialTheme.typography.body1
        )
        Icon(
            painter = painterResource(R.drawable.stripe_ic_chevron_right),
            contentDescription = null,

        )
    }
}

@Composable
private fun IconWithBackground(itemIcon: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = LinkTheme.colors.textSecondary,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center),
            painter = painterResource(id = itemIcon),
            contentDescription = null,
        )
    }
}
