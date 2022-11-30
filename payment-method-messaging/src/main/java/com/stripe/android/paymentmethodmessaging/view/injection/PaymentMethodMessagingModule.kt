package com.stripe.android.paymentmethodmessaging.view.injection

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.text.style.ImageSpan
import androidx.core.text.HtmlCompat
import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingData
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingView
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Named

@Module
internal object PaymentMethodMessagingModule {
    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        configuration: PaymentMethodMessagingView.Configuration
    ): () -> String = { configuration.publishableKey }

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    fun providesStripeImageLoader(
        application: Application
    ): StripeImageLoader = StripeImageLoader(application)

    @Provides
    fun providesMapper(
        scope: CoroutineScope,
        stripeImageLoader: StripeImageLoader,
        configuration: PaymentMethodMessagingView.Configuration
    ): (PaymentMethodMessage) -> Deferred<PaymentMethodMessagingData> {
        return { message ->
            scope.async {
                PaymentMethodMessagingData(
                    message = message,
                    images = message.displayHtml.getBitmapsAsync(scope, stripeImageLoader),
                    config = configuration
                )
            }
        }
    }

    private suspend fun String.getBitmapsAsync(
        scope: CoroutineScope,
        imageLoader: StripeImageLoader
    ): Map<String, Bitmap> = withContext(scope.coroutineContext) {
        val spanned = HtmlCompat.fromHtml(this@getBitmapsAsync, HtmlCompat.FROM_HTML_MODE_LEGACY)
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
}
