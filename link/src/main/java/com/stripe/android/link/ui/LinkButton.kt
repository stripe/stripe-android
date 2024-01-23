@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.utils.InlineContentTemplateBuilder
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.R as StripeR

private val LinkButtonVerticalPadding = 10.dp
private val LinkButtonHorizontalPadding = 25.dp
private val LinkButtonShape: RoundedCornerShape
    get() = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )

private const val LINK_ICON_ID = "LinkIcon"
private const val LINK_DIVIDER_SPACER_ID = "LinkDividerSpacer"
private const val LINK_SPACER_ID = "LinkSpacer"
private const val LINK_DIVIDER_ID = "LinkDivider"
private const val LINK_ARROW_ID = "LinkArrow"

private const val LINK_EMAIL_TEXT_WEIGHT = 0.5f
private const val LINK_EMAIL_FONT_SIZE = 15

private const val LINK_ICON_ASPECT_RATIO = 33f / 13f
private const val LINK_ARROW_ICON_ASPECT_RATIO = 18f / 12f

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val LinkButtonTestTag = "LinkButtonTestTag"

@Preview
@Composable
private fun LinkEmailButton() {
    LinkButton(
        enabled = false,
        email = "theop@email.com",
        onClick = {}
    )
}

@Preview
@Composable
private fun LinkNoEmailButton() {
    LinkButton(
        enabled = true,
        email = null,
        onClick = {}
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun LinkButton(
    email: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        DefaultLinkTheme {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(LinkButtonShape)
                    .testTag(LinkButtonTestTag),
                enabled = enabled,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                shape = LinkButtonShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    disabledBackgroundColor = MaterialTheme.colors.primary
                ),
                contentPadding = PaddingValues(
                    start = LinkButtonHorizontalPadding,
                    top = LinkButtonVerticalPadding,
                    end = LinkButtonHorizontalPadding,
                    bottom = LinkButtonVerticalPadding
                )
            ) {
                if (email == null) {
                    SignedOutButtonContent()
                } else {
                    SignedInButtonContent(email = email)
                }
            }
        }
    }
}

@Composable
private fun RowScope.SignedInButtonContent(email: String) {
    val annotatedEmail = remember(email) {
        buildAnnotatedString {
            append(email)
        }
    }

    val annotatedArrow = remember {
        buildAnnotatedString {
            appendInlineContent(
                id = LINK_SPACER_ID,
                alternateText = "[spacer]"
            )
            appendInlineContent(
                id = LINK_ARROW_ID,
                alternateText = "[arrow]"
            )
        }
    }

    val color = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current)

    LinkIconAndDivider()
    Text(
        text = annotatedEmail,
        color = color,
        fontSize = LINK_EMAIL_FONT_SIZE.sp,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(LINK_EMAIL_TEXT_WEIGHT, fill = false),
        maxLines = 1
    )
    Text(
        text = annotatedArrow,
        color = color,
        fontSize = LINK_EMAIL_FONT_SIZE.sp,
        maxLines = 1,
        inlineContent = InlineContentTemplateBuilder()
            .addSpacer(id = LINK_SPACER_ID, width = 0.4.em)
            .add(id = LINK_ARROW_ID, width = 1.2.em, height = 0.8.em) { LinkArrow() }
            .build()
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.SignedOutButtonContent() {
    val iconizedText = buildAnnotatedString {
        append("Pay with") // TODO(jaynewstrom) Link: Add localization
        append(" ")
        appendInlineContent(
            id = LINK_ICON_ID,
            alternateText = "[icon]"
        )
        appendInlineContent(
            id = LINK_SPACER_ID,
            alternateText = "[spacer]"
        )
        appendInlineContent(
            id = LINK_ARROW_ID,
            alternateText = "[arrow]"
        )
    }

    Text(
        text = iconizedText,
        inlineContent = InlineContentTemplateBuilder()
            .add(id = LINK_ICON_ID, width = 2.2.em, height = 0.93.em) { LinkIcon() }
            .addSpacer(id = LINK_SPACER_ID, width = 0.2.em)
            .add(id = LINK_ARROW_ID, width = 1.05.em, height = 0.7.em) { LinkArrow() }
            .build(),
        modifier = Modifier.padding(start = 6.dp),
        color = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current),
        fontSize = 18.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Composable
private fun LinkIconAndDivider() {
    val annotatedLinkAndDivider = remember {
        buildAnnotatedString {
            appendInlineContent(
                id = LINK_ICON_ID,
                alternateText = "[icon]"
            )
            appendInlineContent(
                id = LINK_DIVIDER_SPACER_ID,
                alternateText = "[divider_spacer]"
            )
            appendInlineContent(
                id = LINK_DIVIDER_ID,
                alternateText = "[divider]"
            )
            appendInlineContent(
                id = LINK_DIVIDER_SPACER_ID,
                alternateText = "[divider_spacer]"
            )
        }
    }

    Text(
        text = annotatedLinkAndDivider,
        fontSize = LINK_EMAIL_FONT_SIZE.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        inlineContent = InlineContentTemplateBuilder()
            .add(id = LINK_ICON_ID, width = 2.4.em, height = 1.em) { LinkIcon() }
            .add(id = LINK_DIVIDER_ID, width = 0.1.em, height = 1.5.em) { LinkDivider() }
            .addSpacer(id = LINK_DIVIDER_SPACER_ID, width = 0.5.em)
            .build()
    )
}

@Composable
private fun LinkIcon() {
    Icon(
        painter = painterResource(R.drawable.stripe_link_logo),
        contentDescription = stringResource(StripeR.string.stripe_link),
        modifier = Modifier.aspectRatio(LINK_ICON_ASPECT_RATIO),
        tint = MaterialTheme.linkColors.buttonLabel
            .copy(alpha = LocalContentAlpha.current)
    )
}

@Composable
private fun LinkDivider() {
    Divider(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(),
        color = MaterialTheme.linkColors.actionLabelLight,
    )
}

@Composable
private fun LinkArrow() {
    Icon(
        painter = painterResource(R.drawable.stripe_link_arrow),
        contentDescription = null,
        modifier = Modifier.aspectRatio(LINK_ARROW_ICON_ASPECT_RATIO),
        tint = MaterialTheme.linkColors.buttonLabel
            .copy(alpha = LocalContentAlpha.current)
    )
}
