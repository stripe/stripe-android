@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.utils.InlineContentTemplateBuilder
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme

private val LinkButtonVerticalPadding = 10.dp
private val LinkButtonHorizontalPadding = 25.dp
private val LinkButtonShape: RoundedCornerShape
    get() = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )

private const val LINK_BRAND_NAME = "Link"
private const val LINK_ICON_ID = "LinkIcon"
private const val LINK_DIVIDER_SPACER_ID = "LinkDividerSpacer"
private const val LINK_DIVIDER_ID = "LinkDivider"

private const val LINK_EMAIL_TEXT_WEIGHT = 0.5f
private const val LINK_PAY_WITH_FONT_SIZE = 21
private const val LINK_EMAIL_FONT_SIZE = 16

private const val LINK_ICON_ASPECT_RATIO = 72f / 26f

internal const val LinkButtonTestTag = "LinkButtonTestTag"

@Preview
@Composable
private fun LinkEmailButton() {
    LinkButton(
        enabled = false,
        email = "theop@email.com",
        onClick = {}
    )
}

@Preview(locale = "ru", fontScale = 1.5f)
@Composable
private fun LinkNoEmailButton() {
    LinkButton(
        enabled = true,
        email = null,
        onClick = {}
    )
}

@Composable
internal fun LinkButton(
    email: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) {
        1f
    } else {
        ContentAlpha.disabled
    }
    CompositionLocalProvider(
        LocalContentAlpha provides alpha
    ) {
        DefaultLinkTheme(contentShape = LinkButtonShape) {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag(LinkButtonTestTag),
                enabled = enabled,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
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
private fun SignedInButtonContent(email: String) {
    val annotatedEmail = remember(email) {
        buildAnnotatedString {
            append(email)
        }
    }

    val color = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current)
    val payWithLinkText = resolvableString(R.string.stripe_pay_with_link).resolve(LocalContext.current)

    Row(
        modifier = Modifier.semantics(
            mergeDescendants = true
        ) {
            this.contentDescription = payWithLinkText
        }
    ) {
        LinkIconAndDivider()
        Text(
            text = annotatedEmail,
            color = color,
            fontSize = LINK_EMAIL_FONT_SIZE.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(LINK_EMAIL_TEXT_WEIGHT, fill = false),
            maxLines = 1
        )
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.SignedOutButtonContent() {
    val text = stringResource(id = R.string.stripe_pay_with_link)

    val iconizedText = buildAnnotatedString {
        append(text.substringBefore(LINK_BRAND_NAME))
        appendInlineContent(
            id = LINK_ICON_ID,
            alternateText = "[icon]"
        )
        append(text.substringAfter(LINK_BRAND_NAME))
    }

    Text(
        text = iconizedText,
        textAlign = TextAlign.Center,
        inlineContent = InlineContentTemplateBuilder().apply {
            add(id = LINK_ICON_ID, width = 2.6.em, height = 0.9.em) { LinkButtonIcon() }
        }.build(),
        modifier = Modifier
            .padding(start = 6.dp)
            .fillMaxWidth()
            .semantics {
                this.contentDescription = text
            },
        color = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current),
        fontSize = LINK_PAY_WITH_FONT_SIZE.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@OptIn(ExperimentalComposeUiApi::class)
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
        inlineContent = InlineContentTemplateBuilder().apply {
            add(id = LINK_ICON_ID, width = 3.em, height = 1.1.em) { LinkButtonIcon() }
            add(id = LINK_DIVIDER_ID, width = 0.1.em, height = 1.3.em) { LinkDivider() }
            addSpacer(id = LINK_DIVIDER_SPACER_ID, width = 0.5.em)
        }.build(),
        modifier = Modifier.semantics { this.invisibleToUser() },
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
private fun LinkButtonIcon() {
    LinkIcon(
        modifier = Modifier
            .aspectRatio(LINK_ICON_ASPECT_RATIO)
            .alpha(LocalContentAlpha.current)
    )
}
