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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.MessagingImage
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

internal class MessagingContentHelper @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
){
    private val _state: MutableStateFlow<PaymentMethodMessage?> = MutableStateFlow(null)
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ): PaymentMethodMessagingElement.Result {
        val state = configuration.build()
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = state.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = state.amount,
            currency = state.currency,
            locale = state.locale,
            country = state.countryCode,
            requestOptions = ApiRequest.Options(paymentConfiguration.publishableKey)
        )

        result.getOrNull()?.let {
            println("YEET result: $it")
            _state.value = it
        } ?: {
            _state.value = null
        }

        return PaymentMethodMessagingElement.Result.Succeeded
    }

    @Composable
    fun Content(appearance: PaymentMethodMessagingElement.Appearance) {
        val state = _state.collectAsState().value  ?: return
        val app = appearance.build()

        if (state.inlinePartnerPromotion != null) {
            SinglePartner(state, app)
        } else {
            MultiPartner(state, app)
        }
    }


    @Composable
    private fun SinglePartner(state: PaymentMethodMessage, appearance: PaymentMethodMessagingElement.Appearance.State) {
        val image = when (appearance.theme) {
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> state.lightImages[0]
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> state.darkImages[0]
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> state.flatImages[0]
        }
        Row {
            TextWithLogo(
                label = state.inlinePartnerPromotion ?: "",
                image = image,
                appearance = appearance,
            )
        }
    }

    @Composable
    private fun MultiPartner(state: PaymentMethodMessage, appearance: PaymentMethodMessagingElement.Appearance.State) {
        if (state.promotion == null) return
        val style = appearance.font?.toTextStyle()
            ?: MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Normal)
        Column {
            Images(getImages(state, appearance.theme))
            Row {
                Text(
                    text = state.promotion ?: "",
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

    private fun getImages(message: PaymentMethodMessage, theme: PaymentMethodMessagingElement.Appearance.Theme): List<MessagingImage> {
        return when (theme) {
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> message.lightImages
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> message.darkImages
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> message.flatImages
        }
    }
}

//    var skipHalfExpanded by remember { mutableStateOf(false) }
//    val state = rememberModalBottomSheetState(
//        initialValue = ModalBottomSheetValue.Hidden,
//        skipHalfExpanded = skipHalfExpanded
//    )
//    val scope = rememberCoroutineScope()
//    ModalBottomSheetLayout(
//    sheetState = state,
//    sheetContent = {
//        Text("yolo")
//    }
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize().padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Row(
//                Modifier.toggleable(
//                    value = skipHalfExpanded,
//                    role = Role.Checkbox,
//                    onValueChange = { checked -> skipHalfExpanded = checked }
//                )
//            ) {
//                Checkbox(checked = skipHalfExpanded, onCheckedChange = null)
//                Spacer(Modifier.width(16.dp))
//                Text("Skip Half Expanded State")
//            }
//            Spacer(Modifier.height(20.dp))
//            Button(onClick = { scope.launch { state.show() } }) {
//                Text("Click to show sheet")
//            }
//        }
//    }
//}