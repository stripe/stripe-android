package com.stripe.android.paymentmethodmessaging.view.messagingelement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.stripe.android.model.MessagingImage
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader

internal class MessagingContent(
    private val message: Message,
) {
    @Composable
    fun Content(appearance: PaymentMethodMessagingElement.Appearance) {
        val appearanceState = appearance.build()
        when (message) {
            is Message.SinglePartner -> SinglePartner(message, appearanceState)
            is Message.MultiPartner -> MultiPartner(message, appearanceState)
            is Message.Empty -> {
                // NO-OP
            }
        }
    }


    @Composable
    private fun SinglePartner(
        message: Message.SinglePartner,
        appearance: PaymentMethodMessagingElement.Appearance.State
    ) {
        val image = when (appearance.theme) {
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImage
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImage
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImage
        }
        Row {
            TextWithLogo(
                label = message.message,
                image = image,
                appearance = appearance,
            )
        }
    }

    @Composable
    private fun MultiPartner(
        message: Message.MultiPartner,
        appearance: PaymentMethodMessagingElement.Appearance.State
    ) {
        val style = appearance.font?.toTextStyle()
            ?: MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Normal)
        Column {
            Images(getImages(message, appearance.theme))
            Row {
                Text(
                    text = message.message,
                    style = style
                )
            }
        }
    }

    @Composable
    private fun TextWithLogo(
        label: String,
        image: MessagingImage,
        appearance: PaymentMethodMessagingElement.Appearance.State
    ) {
        val context = LocalContext.current
        val imageLoader = remember {
            StripeImageLoader(context.applicationContext)
        }
        val style = appearance.font?.toTextStyle()
            ?: MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Normal)
        Text(
            text = label.buildLogoAnnotatedString(),
            style = style,
            color = Color(appearance.colors.textColor),
            inlineContent = mapOf(
                "{partner}" to InlineTextContent(
                    placeholder = Placeholder(
                        width = style.fontSize * 2.5,
                        height = style.fontSize * 2.5,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    StripeImage(
                        url = image.url,
                        imageLoader = imageLoader,
                        contentDescription = image.text,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                "{icon}" to InlineTextContent(
                    placeholder = Placeholder(
                        width = style.fontSize,
                        height = style.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(appearance.colors.infoIconColor),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        )
    }

    @Composable
    private fun String.buildLogoAnnotatedString(): AnnotatedString = buildAnnotatedString {
        val parts = split("{partner}")
        val preLogoString = parts.getOrNull(0)
        val postLogoString = parts.getOrNull(1)
        if (preLogoString == null || postLogoString == null) {
            // {partner} not found, just show label
            append(this@buildLogoAnnotatedString)
        } else {
            append(preLogoString)
            appendInlineContent(id = "{partner}")
            append(postLogoString)
            appendInlineContent("{icon}")
        }
    }

    @Composable
    private fun Images(imageList: List<MessagingImage>) {
        val context = LocalContext.current
        val imageLoader = remember {
            StripeImageLoader(context.applicationContext)
        }
        Row {
            imageList.forEachIndexed { index, messagingImage ->
                StripeImage(
                    url = messagingImage.url,
                    imageLoader = imageLoader,
                    contentDescription = messagingImage.text,
                    contentScale = ContentScale.Fit,
                    disableAnimations = true,
                    modifier = Modifier.align(Alignment.CenterVertically).height(24.dp)
                )
                if (index != imageList.lastIndex) Spacer(Modifier.width(8.dp))
            }
        }
    }

    private fun PaymentMethodMessagingElement.Appearance.Font.State.toTextStyle(): TextStyle {
        return TextStyle(
            fontSize = fontSizeSp?.sp ?: TextUnit.Unspecified,
            fontWeight = fontWeight?.let { FontWeight(it) },
            fontFamily = fontFamily?.let { FontFamily(Font(it)) },
            letterSpacing = letterSpacingSp?.sp ?: TextUnit.Unspecified,
        )
    }

    private fun getImages(
        message: Message.MultiPartner,
        theme: PaymentMethodMessagingElement.Appearance.Theme
    ): List<MessagingImage> {
        return when (theme) {
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImages
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImages
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImages
        }
    }
}