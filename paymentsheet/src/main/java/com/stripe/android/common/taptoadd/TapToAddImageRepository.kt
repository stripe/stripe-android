package com.stripe.android.common.taptoadd

import android.graphics.Bitmap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface TapToAddImageRepository {
    fun get(cardBrand: CardBrand): CardArt?

    suspend fun load(cardBrand: CardBrand): Deferred<CardArt?>

    data class CardArt(
        val bitmap: Bitmap,
        val textColor: Color,
    )
}

internal class DefaultTapToAddImageRepository @Inject constructor(
    @IOContext private val coroutineContext: CoroutineContext,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val imageLoader: StripeImageLoader,
) : TapToAddImageRepository {
    private val images = mutableMapOf<CardBrand, ImageState?>()
    private val imageStorageMutex = Mutex()

    init {
        viewModelScope.launch(coroutineContext) {
            val imageOperations = DEFAULT_CARD_ART_URLS.map { cardArtEntry ->
                async {
                    loadImage(cardArtEntry.key, cardArtEntry.value)
                }
            }.toTypedArray()

            awaitAll(*imageOperations)
        }
    }

    override fun get(cardBrand: CardBrand): TapToAddImageRepository.CardArt? {
        val loadedState = images[cardBrand] as? ImageState.Loaded

        return loadedState?.cardArt
    }

    override suspend fun load(cardBrand: CardBrand) = withContext(coroutineContext) {
        val imageInfo = DEFAULT_CARD_ART_URLS[cardBrand]
            ?: return@withContext CompletableDeferred<TapToAddImageRepository.CardArt?>().apply {
                complete(null)
            }

        return@withContext async {
            loadImage(cardBrand, imageInfo)
        }
    }

    private suspend fun loadImage(
        cardBrand: CardBrand,
        cardArtInfo: CardArtInfo,
    ): TapToAddImageRepository.CardArt? = withContext(coroutineContext) {
        val loadedCardArt = images[cardBrand]?.asLoaded()

        if (loadedCardArt != null) {
            return@withContext loadedCardArt.cardArt
        }

        val currentLoadingOperation = imageStorageMutex.withLock {
            images[cardBrand]?.asLoading()?.operation
        }

        if (currentLoadingOperation != null) {
            return@withContext currentLoadingOperation.await()
        }

        val cardArtOperation = async {
            imageLoader.load(cardArtInfo.url).getOrNull()?.let {
                TapToAddImageRepository.CardArt(
                    bitmap = it,
                    textColor = cardArtInfo.textColor,
                )
            }
        }

        imageStorageMutex.withLock {
            images[cardBrand] = ImageState.Loading(cardArtOperation)
        }

        val cardArt = cardArtOperation.await()

        imageStorageMutex.withLock {
            images[cardBrand] = ImageState.Loaded(cardArt)
        }

        return@withContext cardArt
    }

    private fun ImageState?.asLoaded(): ImageState.Loaded? {
        return this as? ImageState.Loaded
    }

    private fun ImageState?.asLoading(): ImageState.Loading? {
        return this as? ImageState.Loading
    }

    sealed interface ImageState {
        class Loading(val operation: Deferred<TapToAddImageRepository.CardArt?>) : ImageState

        class Loaded(val cardArt: TapToAddImageRepository.CardArt?) : ImageState
    }

    private data class CardArtInfo(
        val url: String,
        val textColor: Color,
    )

    private companion object {
        val DEFAULT_CARD_ART_URLS = mapOf(
            CardBrand.Visa to CardArtInfo(
                url = "https://b.stripecdn.com/ocs-mobile/assets/visa.png",
                textColor = Color.White
            ),
            CardBrand.MasterCard to CardArtInfo(
                url = "https://b.stripecdn.com/ocs-mobile/assets/mastercard.png",
                textColor = Color.Black
            ),
            CardBrand.Discover to CardArtInfo(
                url = "https://b.stripecdn.com/ocs-mobile/assets/discover.webp",
                textColor = Color.White
            ),
            CardBrand.AmericanExpress to CardArtInfo(
                url = "https://b.stripecdn.com/ocs-mobile/assets/amex.webp",
                textColor = Color.White
            ),
            CardBrand.JCB to CardArtInfo(
                url = "https://b.stripecdn.com/ocs-mobile/assets/jcb.png",
                textColor = Color.White
            ),
        )
    }
}

internal val LocalTapToAddImageRepository = staticCompositionLocalOf<TapToAddImageRepository?> { null }
