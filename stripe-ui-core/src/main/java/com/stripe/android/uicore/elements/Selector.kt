package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Selector(
    currentItem: TextFieldIcon.Selector.Item,
    items: List<TextFieldIcon.Selector.Item>,
    onItemSelected: (item: TextFieldIcon.Selector.Item?) -> Unit,
    hasFocus: Boolean,
    popupMessage: ResolvableString?,
    hasMadeSelection: Boolean
) {
    var showPopup by remember { mutableStateOf(!hasMadeSelection) }

    Box(
        Modifier
            .border(
                width = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                color = MaterialTheme.stripeColors.componentBorder,
                shape = MaterialTheme.stripeShapes.roundedCornerShape
            )
            .clip(MaterialTheme.stripeShapes.roundedCornerShape)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                SelectorItem(
                    item = item,
                    isSelected = item == currentItem,
                    onItemSelected = {
                        showPopup = false
                        onItemSelected(it)
                    }
                )
                if (index != items.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(1f),
                        color = MaterialTheme.stripeColors.componentBorder
                    )
                }
            }
        }
    }

    if (showPopup && hasFocus && popupMessage != null) {
        SelectorPopup(popupMessage)
    }
}

@Composable
private fun SelectorItem(
    item: TextFieldIcon.Selector.Item,
    isSelected: Boolean,
    onItemSelected: (item: TextFieldIcon.Selector.Item?) -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.stripeColors.componentBorder.copy()
    } else {
        MaterialTheme.stripeColors.component.copy()
    }
    Box(
        Modifier
            .clickable(
                enabled = item.enabled,
                onClick = {
                    if (isSelected) {
                        onItemSelected(null)
                    } else {
                        onItemSelected(item)
                    }
                }
            )
            .background(color = backgroundColor)
            .padding(6.dp)
            .animateContentSize()
            .testTag("${SELECTOR_ITEM_TEST_TAG}_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_checkmark),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .height(16.dp)
                )
            }
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = item.label.resolve(),
                modifier = Modifier
                    .alpha(if (item.enabled) 1f else ContentAlpha.disabled)
            )
        }
    }
}

@Composable
private fun SelectorPopup(
    popupMessage: ResolvableString
) {
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset(
                x = anchorBounds.right - popupContentSize.width,
                y = anchorBounds.bottom + 10
            )
        }
    ) {
        Surface(
            elevation = 4.dp,
            shape = MaterialTheme.stripeShapes.roundedCornerShape,
            color = MaterialTheme.stripeColors.component
        ) {
            Text(
                text = popupMessage.resolve(),
                color = MaterialTheme.stripeColors.onComponent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val SELECTOR_ITEM_TEST_TAG = "selector_item"
