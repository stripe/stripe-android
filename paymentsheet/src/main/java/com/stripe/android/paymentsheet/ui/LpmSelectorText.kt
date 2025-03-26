package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import com.stripe.android.common.ui.InlineContentTemplateBuilder

@Composable
internal fun LpmSelectorText(
    @DrawableRes icon: Int? = null,
    text: String,
    textColor: Color,
    modifier: Modifier,
    isEnabled: Boolean
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconizedText = buildAnnotatedString {
            icon?.let {
                appendInlineContent(
                    id = "lpm_selector_text_icon",
                    alternateText = "[icon]"
                )
                appendInlineContent(
                    id = "lpm_selector_spacing",
                    alternateText = "[spacer]"
                )
            }
            append(text)
        }

        Text(
            text = iconizedText,
            style = MaterialTheme.typography.caption,
            inlineContent = InlineContentTemplateBuilder().apply {
                icon?.let {
                    add(
                        id = "lpm_selector_text_icon",
                        width = 1.em,
                        height = 1.em
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize(),
                            painter = painterResource(it),
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.6f)
                        )
                    }
                    addSpacer(
                        id = "lpm_selector_spacing",
                        width = 0.4.em,
                    )
                }
            }.build(),
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
