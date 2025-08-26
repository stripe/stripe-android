@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stripe.android.common.ui.InlineContentTemplateBuilder
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.EceLinkWhiteBackground
import com.stripe.android.link.theme.EceLinkWhiteTextPrimary
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.LinkThemeConfig.contentOnPrimaryButton
import com.stripe.android.link.theme.LinkThemeConfig.separatorOnPrimaryButton
import com.stripe.android.link.ui.wallet.BankIcon
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors

private val LinkButtonTheme.textColor: Color
    @Composable
    get() = when (this) {
        LinkButtonTheme.WHITE -> EceLinkWhiteTextPrimary
        LinkButtonTheme.DEFAULT -> LinkTheme.colors.contentOnPrimaryButton
    }

private val LinkButtonTheme.dividerColor: Color
    @Composable
    get() = when (this) {
        LinkButtonTheme.WHITE -> MaterialTheme.stripeColors.componentBorder
        LinkButtonTheme.DEFAULT -> LinkTheme.colors.separatorOnPrimaryButton
    }

private val LinkButtonTheme.borderColor: Color?
    @Composable
    get() = when (this) {
        LinkButtonTheme.WHITE -> MaterialTheme.stripeColors.componentBorder
        LinkButtonTheme.DEFAULT -> null
    }

private val LinkButtonTheme.logoRes: Int
    @Composable
    @DrawableRes
    get() = when (this) {
        LinkButtonTheme.WHITE -> R.drawable.stripe_link_logo_light
        LinkButtonTheme.DEFAULT -> com.stripe.android.uicore.R.drawable.stripe_link_logo_bw
    }

@Composable
private fun Modifier.themeBorder(theme: LinkButtonTheme): Modifier {
    return theme.borderColor?.let { borderColor ->
        this.border(1.dp, borderColor, LinkButtonShape)
    } ?: this
}

@Composable
private fun LinkButtonTheme.buttonColors(): ButtonColors = when (this) {
    LinkButtonTheme.WHITE -> ButtonDefaults.buttonColors(
        backgroundColor = EceLinkWhiteBackground,
        disabledBackgroundColor = EceLinkWhiteBackground,
    )
    LinkButtonTheme.DEFAULT -> ButtonDefaults.buttonColors(
        backgroundColor = LinkTheme.colors.buttonLinkBrand,
        contentColor = LinkTheme.colors.onButtonLinkBrand,
        disabledBackgroundColor = LinkTheme.colors.buttonLinkBrand,
        disabledContentColor = LinkTheme.colors.onButtonLinkBrand.copy(alpha = ContentAlpha.disabled)
    )
}

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

@Preview(name = "LinkButton States and Themes")
@Composable
private fun LinkButtonPreview(
    @PreviewParameter(LinkButtonPreviewParameterProvider::class)
    previewData: LinkButtonPreviewData
) {
    DefaultLinkTheme {
        LinkButton(
            state = previewData.state,
            enabled = previewData.enabled,
            theme = previewData.theme,
            onClick = {}
        )
    }
}

@Preview(name = "LinkButton - Localized (Russian)", locale = "ru", fontScale = 1.5f)
@Composable
private fun LinkButtonLocalizedPreview(
    @PreviewParameter(LinkButtonPreviewParameterProvider::class)
    previewData: LinkButtonPreviewData
) {
    DefaultLinkTheme {
        LinkButton(
            state = previewData.state,
            enabled = previewData.enabled,
            theme = previewData.theme,
            onClick = {}
        )
    }
}

@Composable
internal fun LinkButton(
    state: LinkButtonState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: LinkButtonTheme = LinkButtonTheme.DEFAULT,
) {
    val alpha = if (enabled) {
        1f
    } else {
        ContentAlpha.disabled
    }
    CompositionLocalProvider(
        LocalContentAlpha provides alpha
    ) {
        DefaultLinkTheme {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = PrimaryButtonTheme.shape.height)
                    .themeBorder(theme)
                    .testTag(LinkButtonTestTag),
                enabled = enabled,
                shape = LinkButtonShape,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                colors = theme.buttonColors(),
                contentPadding = PaddingValues(
                    start = LinkButtonHorizontalPadding,
                    top = LinkButtonVerticalPadding,
                    end = LinkButtonHorizontalPadding,
                    bottom = LinkButtonVerticalPadding
                )
            ) {
                when (state) {
                    is LinkButtonState.DefaultPayment -> PaymentDetailsButtonContent(
                        paymentUI = state.paymentUI,
                        theme = theme
                    )

                    is LinkButtonState.Email -> SignedInButtonContent(
                        email = state.email,
                        theme = theme
                    )
                    LinkButtonState.Default -> SignedOutButtonContent(theme = theme)
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailsButtonContent(
    paymentUI: DefaultPaymentUI,
    theme: LinkButtonTheme
) {
    val color = theme.textColor.copy(alpha = LocalContentAlpha.current)
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinkIconAndDivider(theme)

        PaymentDetailsDisplay(paymentUI = paymentUI)

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = paymentUI.last4,
            color = color,
            style = LinkTheme.typography.bodyEmphasized,
            fontSize = LINK_EMAIL_FONT_SIZE.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(LINK_EMAIL_TEXT_WEIGHT, fill = false),
            maxLines = 1
        )
    }
}

@Composable
private fun PaymentDetailsDisplay(
    paymentUI: DefaultPaymentUI
) {
    Box(modifier = Modifier.size(24.dp)) {
        when (paymentUI.paymentType) {
            is DefaultPaymentUI.PaymentType.Card -> Image(
                painter = painterResource(paymentUI.paymentType.iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            is DefaultPaymentUI.PaymentType.BankAccount -> BankIcon(
                bankIconCode = paymentUI.paymentType.bankIconCode
            )
        }
    }
}

@Composable
private fun SignedInButtonContent(
    email: String,
    theme: LinkButtonTheme
) {
    val annotatedEmail = remember(email) {
        buildAnnotatedString {
            append(email)
        }
    }

    val color = theme.textColor.copy(alpha = LocalContentAlpha.current)
    val payWithLinkText = resolvableString(R.string.stripe_pay_with_link).resolve(LocalContext.current)

    Row(
        modifier = Modifier.semantics(
            mergeDescendants = true
        ) {
            this.contentDescription = payWithLinkText
        }
    ) {
        LinkIconAndDivider(theme)
        Text(
            text = annotatedEmail,
            color = color,
            style = LinkTheme.typography.bodyEmphasized,
            fontSize = LINK_EMAIL_FONT_SIZE.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(LINK_EMAIL_TEXT_WEIGHT, fill = false),
            maxLines = 1
        )
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.SignedOutButtonContent(theme: LinkButtonTheme) {
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
            add(id = LINK_ICON_ID, width = 2.6.em, height = 0.9.em) { LinkButtonIcon(theme.logoRes) }
        }.build(),
        modifier = Modifier
            .padding(start = 6.dp)
            .fillMaxWidth()
            .semantics {
                this.contentDescription = text
            },
        color = theme.textColor.copy(alpha = LocalContentAlpha.current),
        style = LinkTheme.typography.bodyEmphasized,
        fontSize = LINK_PAY_WITH_FONT_SIZE.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LinkIconAndDivider(
    theme: LinkButtonTheme
) {
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
        style = LinkTheme.typography.bodyEmphasized,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        inlineContent = InlineContentTemplateBuilder().apply {
            add(id = LINK_ICON_ID, width = 3.em, height = 1.1.em) { LinkButtonIcon(theme.logoRes) }
            add(id = LINK_DIVIDER_ID, width = 0.1.em, height = 1.3.em) { LinkDivider(theme.dividerColor) }
            addSpacer(id = LINK_DIVIDER_SPACER_ID, width = 0.5.em)
        }.build(),
        modifier = Modifier.semantics { this.invisibleToUser() },
    )
}

@Composable
private fun LinkDivider(color: Color) {
    Divider(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(),
        color = color,
    )
}

@Composable
private fun LinkButtonIcon(
    @DrawableRes logoRes: Int
) {
    Icon(
        modifier = Modifier
            .aspectRatio(LINK_ICON_ASPECT_RATIO)
            .alpha(LocalContentAlpha.current),
        painter = painterResource(logoRes),
        contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
        tint = Color.Unspecified
    )
}
