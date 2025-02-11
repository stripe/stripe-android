@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.strings.resolve

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <TDropdownChoice : SingleChoiceDropdownItem> SingleChoiceDropdown(
    expanded: Boolean,
    title: ResolvableString,
    currentChoice: TDropdownChoice?,
    choices: List<TDropdownChoice>,
    onChoiceSelected: (TDropdownChoice) -> Unit,
    headerTextColor: Color,
    optionTextColor: Color,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = title.resolve(),
            color = headerTextColor,
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 13.dp),
        )

        choices.forEach { choice ->
            Choice(
                label = choice.label.resolve(),
                icon = choice.icon,
                isSelected = choice == currentChoice,
                currentTextColor = optionTextColor,
                enabled = choice.enabled,
                onClick = {
                    onChoiceSelected(choice)
                }
            )
        }
    }
}

@Composable
private fun Choice(
    label: String,
    icon: Int?,
    isSelected: Boolean,
    currentTextColor: Color,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .requiredSizeIn(minHeight = 48.dp)
            .clickable(enabled = enabled) { onClick() }
            .testTag("${TEST_TAG_DROP_DOWN_CHOICE}_$label")
            .alpha(if (enabled) 1f else ContentAlpha.disabled)
    ) {
        icon?.let { icon ->
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.padding(start = 13.dp),
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            color = if (isSelected) {
                MaterialTheme.colors.primary
            } else {
                currentTextColor
            },
            fontWeight = if (isSelected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier
                .height(20.dp)
                .padding(start = 8.dp, end = 16.dp)
                .alpha(if (isSelected) 1f else 0f),
            tint = MaterialTheme.colors.primary,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_DROP_DOWN_CHOICE = "TEST_TAG_DROP_DOWN_CHOICE"
