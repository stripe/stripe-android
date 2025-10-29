@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.ui.core.R as UiCoreR


internal sealed class PaymentMethodMessagingContent {

    @Composable
    abstract fun Content(appearance: PaymentMethodMessagingElement.Appearance.State)

    class SinglePartner(
        private val message: PaymentMethodMessageSinglePartner
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            SinglePartner(message, appearance)
        }
    }

    class MultiPartner(
        private val message: PaymentMethodMessageMultiPartner
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            MultiPartner(message, appearance)
        }
    }

    object NoContent : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            // NO-OP
        }
    }

    companion object {
        fun get(message: PaymentMethodMessage): PaymentMethodMessagingContent {
            val singlePartnerMessage = message.singlePartner
            val multiPartnerMessage = message.multiPartner
            return if (singlePartnerMessage != null) {
                SinglePartner(singlePartnerMessage)
            } else if (multiPartnerMessage != null) {
                MultiPartner(multiPartnerMessage)
            } else {
                NoContent
            }
        }
    }
}

@Composable
private fun SinglePartner(
    message: PaymentMethodMessageSinglePartner,
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    val image = when (appearance.theme) {
        PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImage
        PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImage
        PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImage
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextWithLogo(
            label = message.inlinePartnerPromotion,
            image = image,
            appearance = appearance,
        )
    }
}

@Composable
private fun MultiPartner(
    message: PaymentMethodMessageMultiPartner,
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    val style = appearance.font?.toTextStyle(appearance)
        ?: MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Normal)
    Column {
        Images(getImages(message, appearance.theme), appearance)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message.promotion.buildAnnotatedStringWithInfoIcon(),
                style = style,
                inlineContent = mapOf(
                    INLINE_ICON_KEY to InlineTextContent(
                        placeholder = Placeholder(
                            width = style.fontSize * INLINE_INFO_ICON_SCALE_FACTOR,
                            height = style.fontSize * INLINE_INFO_ICON_SCALE_FACTOR,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        InfoIcon(appearance)
                    }
                )
            )
        }
    }
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
    val fontSize = appearance.font?.fontSizeSp ?: DEFAULT_TEXT_SIZE
    val scaleFactor = fontSize / DEFAULT_TEXT_SIZE
    val iconHeight = DEFAULT_ICON_SIZE * scaleFactor

    Row {
        imageList.forEachIndexed { index, messagingImage ->
            StripeImage(
                url = messagingImage.url,
                imageLoader = imageLoader,
                contentDescription = messagingImage.text,
                contentScale = ContentScale.Fit,
                loadingContent = { FallbackIconFromResource(messagingImage, appearance) },
                errorContent = { FallbackIconFromResource(messagingImage, appearance) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(getIconSize(appearance.font?.fontSizeSp).dp)
            )
            if (index != imageList.lastIndex) Spacer(Modifier.width(8.dp))
        }
    }
}

private fun getIconSize(fontSize: Float?): Float {
    val fontSizeSp = fontSize ?: DEFAULT_TEXT_SIZE
    val scaleFactor = fontSizeSp / DEFAULT_TEXT_SIZE
    val iconHeight = DEFAULT_ICON_SIZE * scaleFactor
    return iconHeight
}

private fun getImages(
    message: PaymentMethodMessageMultiPartner,
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
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }
    val style = appearance.font?.toTextStyle(appearance)
        ?: MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Normal)

    Text(

        text = label.buildInlineLogoAnnotatedStringWithInfoIcon(),
        style = style,
        inlineContent = mapOf(
            INLINE_IMAGE_KEY to InlineTextContent(
                placeholder = Placeholder(
                    width = style.fontSize * INLINE_LOGO_SCALE_FACTOR,
                    height = style.fontSize * INLINE_LOGO_SCALE_FACTOR,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                StripeImage(
                    url = image.url,
                    imageLoader = imageLoader,
                    contentDescription = image.text,
                    loadingContent = { FallbackIconFromResource(image, appearance) },
                    errorContent = { FallbackIconFromResource(image, appearance) },
                    modifier = Modifier.fillMaxSize()
                )
            },
            INLINE_ICON_KEY to InlineTextContent(
                placeholder = Placeholder(
                    width = style.fontSize * INLINE_INFO_ICON_SCALE_FACTOR,
                    height = style.fontSize * INLINE_INFO_ICON_SCALE_FACTOR,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                InfoIcon(appearance)
            }
        )
    )
}

@Composable
private fun String.buildInlineLogoAnnotatedStringWithInfoIcon(): AnnotatedString = buildAnnotatedString {
    val parts = split(INLINE_IMAGE_KEY)
    val preLogoString = parts.getOrNull(0)
    val postLogoString = parts.getOrNull(1)
    if (preLogoString == null || postLogoString == null) {
        // {partner} not found, just show label
        append(this@buildInlineLogoAnnotatedStringWithInfoIcon)
    } else {
        append(preLogoString)
        appendInlineContent(id = INLINE_IMAGE_KEY)
        append(postLogoString)
        appendInlineContent(id = INLINE_ICON_KEY)
    }
}

@Composable
private fun String.buildAnnotatedStringWithInfoIcon(): AnnotatedString = buildAnnotatedString {
    append(this@buildAnnotatedStringWithInfoIcon)
    appendInlineContent(id = INLINE_ICON_KEY)
}

private fun PaymentMethodMessagingElement.Appearance.Font.State.toTextStyle(
    appearance: PaymentMethodMessagingElement.Appearance.State
): TextStyle {
    return TextStyle(
        fontSize = fontSizeSp?.sp ?: DEFAULT_TEXT_SIZE.sp,
        fontWeight = fontWeight?.let { FontWeight(it) },
        fontFamily = fontFamily?.let { FontFamily(Font(it)) },
        letterSpacing = letterSpacingSp?.sp ?: TextUnit.Unspecified,
        color = Color(appearance.colors.textColor)
    )
}

@Composable
private fun InfoIcon(appearance: PaymentMethodMessagingElement.Appearance.State) {
    Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = Color(appearance.colors.infoIconColor),
        modifier = Modifier.fillMaxSize().padding(start = 4.dp)
    )
}

@Composable
private fun FallbackIconFromResource(
    image: PaymentMethodMessageImage,
    appearance: PaymentMethodMessagingElement.Appearance.State
) {
    val iconRes = when (PaymentMethod.Type.fromCode(image.paymentMethodType)) {
        PaymentMethod.Type.Klarna -> UiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna
        PaymentMethod.Type.Affirm -> UiCoreR.drawable.stripe_ic_paymentsheet_pm_affirm
        PaymentMethod.Type.AfterpayClearpay -> UiCoreR.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay
        else -> null
    }
    iconRes?.let {
        Image(
            painter = painterResource(it),
            contentDescription = image.text,
            modifier = Modifier.size(getIconSize(appearance.font?.fontSizeSp).dp)
        )
    }
}

private const val DEFAULT_TEXT_SIZE = 16F
private const val DEFAULT_ICON_SIZE = 20
private const val INLINE_IMAGE_KEY = "{partner}"
private const val INLINE_ICON_KEY = "{icon}"
private const val INLINE_LOGO_SCALE_FACTOR = 2.5
private const val INLINE_INFO_ICON_SCALE_FACTOR = 1.4
