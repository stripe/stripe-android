package com.stripe.android.ui.core.elements.messaging

import android.content.Context
import android.graphics.Bitmap
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.stripe.android.core.browser.BrowserCapabilitiesSupplier
import com.stripe.android.core.browser.BrowserLauncher
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.isSystemDarkTheme
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A view that displays promotional text and images for payment methods like Afterpay and Klarna.
 * For example, "As low as 4 interest-free-payments of $9.75". When tapped, this view presents a
 * full-screen Custom Chrome Tab to the customer with additional information ont he payment methods
 * being displayed.
 *
 * You can embed this into your checkout or product screens to promote payment method options to
 * your customer. The color of the images displayed changes based on the [textColor].
 *
 * Note: You must initialize this view with [PaymentMethodMessagingView.load]. For example:
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
class PaymentMethodMessagingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val data = MutableStateFlow<PaymentMethodMessageData?>(null)
    private var job: Job? = null
    private val stripeImageLoader = StripeImageLoader(
        context = context,
        memoryCache = null,
        diskCache = null
    )

    @Composable
    override fun Content() {
        data.collectAsState().value?.let { data ->
            PaymentMethodMessage(
                data = data
            )
        }
    }

    fun load(config: Configuration, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        job = CoroutineScope(Dispatchers.IO).launch {
            val loader = PaymentMethodLoader(context, config)
            val message = loader.loadMessage()
            message.fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        data.value = PaymentMethodMessageData(
                            message = it,
                            images = it.displayLHtml.getBitmaps(this, stripeImageLoader),
                            config = config
                        )
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

    override fun onDetachedFromWindow() {
        job?.cancel()
        super.onDetachedFromWindow()
    }

    data class Configuration(
        val context: Context,
        /**
         * The publishable key used to make requests to Stripe.
         */
        val publishableKey: String,

        /**
         * The Stripe Connect account making requests to Stripe.
         */
        val stripeAccountId: String? = null,

        /**
         * The payment methods to display messaging for.
         */
        val paymentMethods: List<PaymentMethod>,

        /**
         * The currency, as a three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html).
         */
        val currency: String,

        /**
         * The customer's locale. Defaults to the device locale.
         */
        val amount: Int,

        /**
         * The purchase amount, in the smallest currency unit. e.g. 100 for $1 USD.
         */
        val locale: Locale = Locale.current,

        /**
         * The customer's country as a two-letter string. Defaults to their device's country.
         */
        val countryCode: String = Locale.current.region,

        /**
         * The font of text displayed in the view. Defaults to the system font.
         */
        val font: FontFamily = FontFamily.Default,

        /**
         * The color of text displayed in the view. Defaults to onComponent.
         */
        val textColor: Color = if (context.isSystemDarkTheme()) {
            PaymentsThemeDefaults.colorsDark.onComponent
        } else {
            PaymentsThemeDefaults.colorsLight.onComponent
        },

        /**
         * The color of the images displayed in the view as a tuple specifying the color to use in light and dark mode.
         */
        val imageColor: ImageColor = if (context.isSystemDarkTheme()) {
            ImageColor.Light
        } else {
            ImageColor.Dark
        }
    ) {
        /**
         * Payment methods that can be displayed by `PaymentMethodMessagingView`
         */
        enum class PaymentMethod(internal val value: String) {
            Klarna("klarna"),
            AfterpayClearpay("afterpay_clearpay")
        }

        /**
         * The colors of the image
         */
        enum class ImageColor(internal val value: String) {
            Light("white"),
            Dark("black"),
            Color("color")
        }
    }
}

/**
 * Returns a stateful [PaymentMethodMessageResult] used for displaying a [PaymentMethodMessage]
 *
 * @param config The [PaymentMethodMessagingView.Configuration] for the view
 */
@Composable
fun rememberMessagingState(
    config: PaymentMethodMessagingView.Configuration
): State<PaymentMethodMessageResult> {
    val composeState = remember {
        MutableStateFlow<PaymentMethodMessageResult>(
            PaymentMethodMessageResult.Loading
        )
    }
    val context = LocalContext.current
    val loader = remember(config) { PaymentMethodLoader(context, config) }
    val imageLoader = remember {
        StripeImageLoader(
            context = context,
            memoryCache = null,
            diskCache = null
        )
    }

    LaunchedEffect(config) {
        loader.loadMessage().fold(
            onSuccess = {
                composeState.value = PaymentMethodMessageResult.Success(
                    data = PaymentMethodMessageData(
                        message = it,
                        images = it.displayLHtml.getBitmaps(this, imageLoader),
                        config = config
                    )
                )
            },
            onFailure = {
                composeState.value = PaymentMethodMessageResult.Failure(it)
            }
        )
    }

    return composeState.collectAsState()
}

/**
 * A Composable that displays promotional text and images for payment methods like Afterpay and Klarna.
 * For example, "As low as 4 interest-free-payments of $9.75". When tapped, this view presents a
 * full-screen Custom Chrome Tab to the customer with additional information ont he payment methods
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
 * @param data The [PaymentMethodMessageData] required to render this Composable view
 */
@Composable
fun PaymentMethodMessage(
    modifier: Modifier = Modifier,
    data: PaymentMethodMessageData
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .border(
                border = BorderStroke(width = 1.dp, Color(0x33787880)),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    val browserLauncher = BrowserLauncher(
                        capabilities = BrowserCapabilitiesSupplier(context).get(),
                        statusBarColor = null,
                        url = data.message.learnMoreModalUrl,
                        title = "Learn More"
                    )
                    val intent = browserLauncher.createLaunchIntent()
                    context.startActivity(intent)
                }
                .padding(16.dp)
        ) {
            Html(
                html = data.message.displayLHtml,
                style = TextStyle.Default.copy(
                    fontFamily = data.config.font
                ),
                imageLoader = data.images
                    .map { Pair(it.key, EmbeddableImage.Bitmap(it.value)) }
                    .toMap(),
                color = data.config.textColor
            )
        }
    }
}

private suspend fun String.getBitmaps(
    scope: CoroutineScope,
    imageLoader: StripeImageLoader
): Map<String, Bitmap> = withContext(scope.coroutineContext) {
    val spanned = HtmlCompat.fromHtml(this@getBitmaps, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val images = spanned
        .getSpans(0, spanned.length, Any::class.java)
        .filterIsInstance<ImageSpan>()
        .map { it.source!! }

    val deferred = images.map { url ->
        async {
            Pair(url, imageLoader.load(url).getOrNull())
        }
    }

    val bitmaps = deferred.awaitAll()

    bitmaps.mapNotNull { pair ->
        pair.second?.let { bitmap ->
            Pair(pair.first, bitmap)
        }
    }.toMap()
}
