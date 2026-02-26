@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLegalDisclosure
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.navigation.rememberKeyboardController
import kotlinx.coroutines.launch

internal sealed class PaymentMethodMessagingContent {

    @Composable
    abstract fun Content(appearance: PaymentMethodMessagingElement.Appearance.State)

    class SinglePartner(
        private val message: PaymentMethodMessage.SinglePartner,
        private val analyticsOnClick: () -> Unit
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            SinglePartner(message, appearance, analyticsOnClick)
        }
    }

    class MultiPartner(
        private val message: PaymentMethodMessage.MultiPartner,
        private val analyticsOnClick: () -> Unit
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            MultiPartner(message, appearance, analyticsOnClick)
        }
    }

    object NoContent : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            // NO-OP
        }
    }

    companion object {
        fun get(message: PaymentMethodMessage, analyticsOnClick: () -> Unit): PaymentMethodMessagingContent {
            return when (message) {
                is PaymentMethodMessage.MultiPartner -> MultiPartner(message, analyticsOnClick)
                is PaymentMethodMessage.SinglePartner -> SinglePartner(message, analyticsOnClick)
                is PaymentMethodMessage.NoContent,
                is PaymentMethodMessage.UnexpectedError -> NoContent
            }
        }
    }
}

@Composable
private fun SinglePartner(
    message: PaymentMethodMessage.SinglePartner,
    appearance: PaymentMethodMessagingElement.Appearance.State,
    analyticsOnClick: () -> Unit
) {
    val image = when (appearance.theme) {
        PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImage
        PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImage
        PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImage
    }
    val context = LocalContext.current
    val keyboardController = rememberKeyboardController()
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            scope.launch { keyboardController.dismiss() }
            analyticsOnClick()
            launchLearnMore(context, message.learnMore.url, appearance.theme)
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextWithLogo(
                label = message.inlinePartnerPromotion,
                image = image,
                appearance = appearance,
                learnMoreMessage = message.learnMore.message
            )
            message.legalDisclosure?.let {
                LegalDisclosure(
                    legalDisclosure = it,
                    theme = appearance.theme
                )
            }
        }
    }
}

@Composable
private fun MultiPartner(
    message: PaymentMethodMessage.MultiPartner,
    appearance: PaymentMethodMessagingElement.Appearance.State,
    analyticsOnClick: () -> Unit
) {
    val style = appearance.font?.toTextStyle()
        ?: MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)

    val context = LocalContext.current
    val keyboardController = rememberKeyboardController()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.clickable {
            scope.launch { keyboardController.dismiss() }
            analyticsOnClick()
            launchLearnMore(context, message.learnMore.url, appearance.theme)
        },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Images(
            imageList = getImages(message, appearance.theme),
            appearance = appearance
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message.promotion.maybeAddPeriod().buildAnnotatedStringWithLearnMoreText(
                    message.learnMore.message,
                    appearance
                ),
                style = style,
                color = Color(getTextColor(appearance))
            )
        }
        message.legalDisclosure?.let {
            LegalDisclosure(
                legalDisclosure = it,
                theme = appearance.theme
            )
        }
    }
}

private fun launchLearnMore(
    context: Context,
    learnMoreUrl: String,
    theme: PaymentMethodMessagingElement.Appearance.Theme
) {
    val args = LearnMoreActivityArgs.argsFromUrlAndTheme(learnMoreUrl, theme)
    context.startActivity(LearnMoreActivityArgs.createIntent(context, args))
}

@Composable
private fun Images(
    imageList: List<PaymentMethodMessageImage>,
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }
    if (imageList.isNotEmpty()) {
        Row(Modifier.height(getIconHeight(appearance).dp)) {
            imageList.forEachIndexed { index, messagingImage ->
                StripeImage(
                    url = messagingImage.url,
                    imageLoader = imageLoader,
                    contentDescription = messagingImage.text,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                if (index != imageList.lastIndex) Spacer(Modifier.width(8.dp))
            }
        }
    }
}

private fun getIconHeight(appearance: PaymentMethodMessagingElement.Appearance.State): Float {
    val fontSize = appearance.font?.fontSizeSp ?: DEFAULT_TEXT_SIZE
    val scaleFactor = fontSize / DEFAULT_TEXT_SIZE
    return DEFAULT_ICON_SIZE * scaleFactor
}

private fun getImages(
    message: PaymentMethodMessage.MultiPartner,
    theme: PaymentMethodMessagingElement.Appearance.Theme
): List<PaymentMethodMessageImage> {
    return when (theme) {
        PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImages
        PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImages
        PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImages
    }
}

@Composable
private fun TextWithLogo(
    label: String,
    image: PaymentMethodMessageImage,
    appearance: PaymentMethodMessagingElement.Appearance.State,
    learnMoreMessage: String
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }
    val style = appearance.font?.toTextStyle()
        ?: MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)

    Text(
        text = label.maybeAddPeriod().buildInlineLogoAnnotatedString(
            learnMoreMessage,
            appearance
        ),
        style = style,
        color = Color(getTextColor(appearance)),
        inlineContent = mapOf(
            INLINE_IMAGE_KEY to InlineTextContent(
                placeholder = Placeholder(
                    width = style.fontSize * INLINE_LOGO_SCALE_FACTOR,
                    height = style.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                StripeImage(
                    url = image.url,
                    imageLoader = imageLoader,
                    contentDescription = image.text,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    )
}

@Composable
private fun String.buildInlineLogoAnnotatedString(
    learnMoreString: String,
    appearance: PaymentMethodMessagingElement.Appearance.State
): AnnotatedString = buildAnnotatedString {
    val parts = split(INLINE_IMAGE_KEY)
    val preLogoString = parts.getOrNull(0)
    val postLogoString = parts.getOrNull(1)
    if (preLogoString == null || postLogoString == null) {
        // {partner} not found, just show label
        append(this@buildInlineLogoAnnotatedString)
    } else {
        append(preLogoString)
        appendInlineContent(id = INLINE_IMAGE_KEY)
        append(postLogoString)
    }
    addLearnMoreMessage(learnMoreString, appearance)
}

@Composable
private fun String.buildAnnotatedStringWithLearnMoreText(
    learnMoreString: String,
    appearance: PaymentMethodMessagingElement.Appearance.State
): AnnotatedString = buildAnnotatedString {
    append(this@buildAnnotatedStringWithLearnMoreText)
    addLearnMoreMessage(learnMoreString, appearance)
}

private fun AnnotatedString.Builder.addLearnMoreMessage(
    linkText: String,
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    withStyle(SpanStyle(color = Color(getLinkTextColor(appearance)))) {
        append(" $linkText")
    }
}

private fun String.maybeAddPeriod(): String {
    // '}' indicates where the partner icon will be place, if the promotional message ends with the image we do
    // not need to append a period.
    return if (endsWith('.') || endsWith('}')) this else "$this."
}

private fun PaymentMethodMessagingElement.Appearance.Font.State.toTextStyle(): TextStyle {
    return TextStyle(
        fontSize = fontSizeSp?.sp ?: DEFAULT_TEXT_SIZE.sp,
        fontWeight = fontWeight?.let { FontWeight(it) },
        fontFamily = fontFamily?.let { FontFamily(Font(it)) },
        letterSpacing = letterSpacingSp?.sp ?: TextUnit.Unspecified,
    )
}

@Composable
private fun LegalDisclosure(
    legalDisclosure: PaymentMethodMessageLegalDisclosure,
    theme: PaymentMethodMessagingElement.Appearance.Theme
) {
    Text(
        text = legalDisclosure.message,
        color = StripeThemeDefaults.colors(theme == PaymentMethodMessagingElement.Appearance.Theme.DARK).subtitle,
        style = MaterialTheme.typography.bodySmall
    )
}

private fun getTextColor(appearance: PaymentMethodMessagingElement.Appearance.State): Int {
    return appearance.colors.textColor ?: StripeTheme.getColors(
        appearance.theme == PaymentMethodMessagingElement.Appearance.Theme.DARK
    ).onComponent.toArgb()
}

private fun getLinkTextColor(appearance: PaymentMethodMessagingElement.Appearance.State): Int {
    return appearance.colors.linkTextColor ?: StripeTheme.getColors(
        appearance.theme == PaymentMethodMessagingElement.Appearance.Theme.DARK
    ).materialColorScheme.primary.toArgb()
}

private const val DEFAULT_TEXT_SIZE = 16F
private const val DEFAULT_ICON_SIZE = 20
private const val INLINE_IMAGE_KEY = "{partner}"
private const val INLINE_LOGO_SCALE_FACTOR = 2.5
