package com.stripe.android.common.taptoadd

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import com.stripe.android.common.ui.InlineContentTemplateBuilder
import com.stripe.android.paymentsheet.R

internal const val TAP_TO_BUTTON_UI_TEST_TAG = "tap_to_button_ui"
private const val NFC_ICON_INLINE_CONTENT_ID = "nfc_icon"

@Composable
internal fun TapButtonUI(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.primary.copy(alpha = ContentAlpha.disabled)
    }

    Text(
        text = buildAnnotatedString {
            appendInlineContent(
                id = NFC_ICON_INLINE_CONTENT_ID,
                alternateText = " ",
            )
            append(" ")
            append(label)
        },
        modifier = Modifier
            .testTag(TAP_TO_BUTTON_UI_TEST_TAG)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        color = contentColor,
        style = MaterialTheme.typography.h6,
        inlineContent = InlineContentTemplateBuilder()
            .add(
                id = NFC_ICON_INLINE_CONTENT_ID,
                width = 1.4.em,
                height = 1.4.em,
                align = PlaceholderVerticalAlign.TextCenter,
            ) {
                Image(
                    painter = painterResource(R.drawable.stripe_ic_nfc_tap),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            .build()
    )
}
