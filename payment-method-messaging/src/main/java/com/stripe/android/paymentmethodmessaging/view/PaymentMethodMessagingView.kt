package com.stripe.android.paymentmethodmessaging.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stripe.android.paymentmethodmessaging.view.theme.Color as PaymentMethodMessageColor

/**
 * A view that displays promotional text and images for payment methods like Afterpay and Klarna.
 * For example, "As low as 4 interest-free-payments of $9.75". When tapped, this view presents a
 * full-screen Custom Chrome Tab to the customer with additional information ont he payment methods
 * being displayed.
 *
 * You can embed this into your checkout or product screens to promote payment method options to
 * your customer.
 *
 * **Note**: You must initialize this view with [PaymentMethodMessagingView.load]. For example:
 *
 * PaymentMethodMessagingView.load(
 *     config = config,
 *     onSuccess = {
 *         // Show view
 *     },
 *     onFailure = {
 *         // Show error
 *     }
 * )
 */
internal class PaymentMethodMessagingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private val data = MutableStateFlow<PaymentMethodMessagingData?>(null)

    @Composable
    override fun Content() {
        data.collectAsState().value?.let { data ->
            PaymentMethodMessaging(
                data = data
            )
        }
    }

    fun load(
        config: Configuration,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            val viewModel: PaymentMethodMessagingViewModel = ViewModelProvider(
                context as ViewModelStoreOwner,
                PaymentMethodMessagingViewModel.Factory(config)
            )[PaymentMethodMessagingViewModel::class.java]

            viewModel.loadMessage()

            viewModel.messageFlow.collect { message ->
                message?.fold(
                    onSuccess = {
                        withContext(Dispatchers.Main) {
                            data.value = it
                            onSuccess()
                        }
                    },
                    onFailure = {
                        withContext(Dispatchers.Main) {
                            onFailure(it)
                        }
                    }
                )
            }
        }
    }

    class Configuration @JvmOverloads constructor(
        /**
         * The publishable key used to make requests to Stripe.
         */
        internal val publishableKey: String,

        /**
         * The payment methods to display messaging for.
         */
        internal val paymentMethods: Set<PaymentMethod>,

        /**
         * The currency, as a three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html).
         */
        internal val currency: String,

        /**
         * The purchase amount in the smallest currency unit, e.g. 100 for $1 USD.
         */
        internal val amount: Int,

        /**
         * The current locale of the device.
         */
        internal val locale: Locale = Locale.current,

        /**
         * The customer's country as a two-letter string. Defaults to their device's country.
         */
        internal val countryCode: String = Locale.current.region,

        /**
         * The font of text displayed in the view. Defaults to the system font.
         */
        @FontRes internal val fontFamily: Int? = null,

        /**
         * The color of text displayed in the view.
         */
        @ColorInt internal val textColor: Int? = null,

        /**
         * The color of the images displayed in the view.
         */
        internal val imageColor: ImageColor? = null
    ) {
        /**
         * Payment methods that can be displayed by `PaymentMethodMessagingView`
         */
        enum class PaymentMethod(internal val value: String) {
            Klarna("klarna"),
            AfterpayClearpay("afterpay_clearpay")
        }

        /**
         * The colors of the payment method images
         */
        enum class ImageColor(internal val value: String) {
            Light("white"),
            Dark("black"),
            Color("color")
        }
    }
}

/**
 * Returns a stateful [PaymentMethodMessagingState] used for displaying a [PaymentMethodMessaging]
 *
 * @param config The [PaymentMethodMessagingView.Configuration] for the view
 */
@Composable
internal fun rememberMessagingState(
    config: PaymentMethodMessagingView.Configuration
): State<PaymentMethodMessagingState> {
    val composeState = remember {
        mutableStateOf<PaymentMethodMessagingState>(PaymentMethodMessagingState.Loading)
    }

    val viewModel: PaymentMethodMessagingViewModel = viewModel(
        factory = PaymentMethodMessagingViewModel.Factory(config)
    )

    LaunchedEffect(config) {
        viewModel.loadMessage()
        viewModel.messageFlow.collect { result ->
            result?.fold(
                onSuccess = { data ->
                    composeState.value = PaymentMethodMessagingState.Success(
                        data = data
                    )
                },
                onFailure = {
                    composeState.value = PaymentMethodMessagingState.Failure(it)
                }
            )
        }
    }

    return composeState
}

/**
 * A Composable that displays promotional text and images for payment methods like Afterpay and Klarna.
 * For example, "As low as 4 interest-free-payments of $9.75". When tapped, this view presents a
 * full-screen Chrome Custom Tab to the customer with additional information about the payment methods
 * being displayed.
 *
 * You can embed this into your checkout or product screens to promote payment method options to
 * your customer. The color of the images displayed changes based on the [textColor].
 *
 * Note: You must initialize this Composable with [rememberMessagingState]. For example:
 *
 * setContent {
 *     val state = rememberMessagingState(config)
 *     if (state is Success) {
 *         PaymentMethodMessage(
 *             data = state.data
 *         )
 *     }
 * }
 *
 * @param modifier The [Modifier] for this Composable view
 * @param data The [PaymentMethodMessagingData] required to render this Composable view
 */
@Composable
internal fun PaymentMethodMessaging(
    modifier: Modifier = Modifier,
    data: PaymentMethodMessagingData
) {
    val context = LocalContext.current

    val clickable = {
        CustomTabsIntent.Builder().build()
            .launchUrl(context, Uri.parse(data.message.learnMoreUrl))
    }

    PaymentMethodMessagingTheme {
        Box(
            modifier = modifier
                .border(
                    border = BorderStroke(width = 1.dp, PaymentMethodMessageColor.ComponentDivider),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { clickable() }
                    .padding(16.dp)
            ) {
                Html(
                    html = data.message.displayHtml,
                    style = TextStyle.Default.copy(
                        fontFamily = if (data.config.fontFamily != null) {
                            FontFamily(Font(data.config.fontFamily))
                        } else {
                            FontFamily.Default
                        }
                    ),
                    imageLoader = data.images
                        .map { Pair(it.key, EmbeddableImage.Bitmap(it.value)) }
                        .toMap(),
                    color = if (data.config.textColor != null) {
                        Color(data.config.textColor)
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                    onClick = {
                        clickable()
                    }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodMessagingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) darkColors() else lightColors(),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colors.onSurface
            ) {
                content()
            }
        }
    )
}
