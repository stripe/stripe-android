package com.stripe.android.paymentmethodmessaging.view.messagingelement

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
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
    private val _state: MutableStateFlow<State?> = MutableStateFlow(null)
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ): PaymentMethodMessagingElement.Result {
        val state = configuration.build()
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = null,
            amount = state.amount,
            currency = state.currency,
            locale = state.locale,
            country = state.countryCode,
            requestOptions = ApiRequest.Options(paymentConfiguration.publishableKey)
        )

        result.getOrNull()?.let {
            _state.value = State(
                message = it.paymentMethods
            )
        } ?: {
            _state.value = null
        }

        return PaymentMethodMessagingElement.Result.Succeeded()
    }

    @Composable
    fun Content(appearance: PaymentMethodMessagingElement.Appearance) {
        val state = _state.collectAsState().value

        if (state != null) {
            Text(state.message)
        }
    }

    @Composable
    private fun TextWithLogo(label: String) {
        val context = LocalContext.current
        val imageLoader = remember {
            StripeImageLoader(context.applicationContext)
        }
        val style = TextStyle(
            fontSize = 16.sp
        )
        Text(
            text = label.buildLogoAnnotatedString(),
            style = style,
            inlineContent = mapOf(
                "logo_here" to InlineTextContent(
                    placeholder = Placeholder(
                        width = style.fontSize,
                        height = style.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    StripeImage(
                        url = "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-klarna@3x-cbd108f6432733bea9ef16827d10f5c5.png",
                        imageLoader = imageLoader,
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        )
    }

    @Composable
    private fun String.buildLogoAnnotatedString(): AnnotatedString = buildAnnotatedString {
        val parts = split("logo_here")
        val preLogoString = parts.getOrNull(0)
        val postLogoString = parts.getOrNull(1)
        if (preLogoString == null || postLogoString == null) {
            // logo_here not found, just show label
            append(this@buildLogoAnnotatedString)
        } else {
            append(preLogoString)
            appendInlineContent(id = "logo_here")
            append(postLogoString)
        }
    }

    private data class State(
        val message: String
    )
}