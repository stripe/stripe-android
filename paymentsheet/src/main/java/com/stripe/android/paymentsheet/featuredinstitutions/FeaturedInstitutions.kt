package com.stripe.android.paymentsheet.featuredinstitutions

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

internal data class FeaturedInstitutionIcon(
    val id: String,
    val bitmap: ImageBitmap,
)

/**
 * The featured bank institution icons shown on the instant debits "Bank" row, for a single load.
 *
 * [icons] starts empty and is only ever empty or has at least [MINIMUM_ICON_COUNT] icons.
 */
internal class FeaturedInstitutions(
    private val onShown: () -> Unit,
) {
    private val _icons = MutableStateFlow<List<FeaturedInstitutionIcon>>(emptyList())
    val icons: StateFlow<List<FeaturedInstitutionIcon>> = _icons.asStateFlow()

    /**
     * The institutions whose icons are currently published, so another screen can show the same icons.
     */
    @Volatile
    var publishedInstitutions: List<FeaturedInstitution>? = null
        private set

    private val hasReportedShown = AtomicBoolean(false)

    fun reportShown() {
        if (hasReportedShown.compareAndSet(false, true)) {
            onShown()
        }
    }

    /**
     * Downloads the icons of [institutions] in parallel, keeping their order and skipping failures. The first
     * [MINIMUM_ICON_COUNT] icons are published as soon as they're ready, and the full list once all downloads finish.
     */
    suspend fun loadIcons(
        institutions: List<FeaturedInstitution>,
        imageLoader: StripeImageLoader,
        onInsufficientIcons: (count: Int) -> Unit,
    ) = coroutineScope {
        val downloads = institutions.mapNotNull { institution ->
            val iconUrl = institution.iconUrl?.takeIf { it.isValidIconUrl() } ?: return@mapNotNull null
            institution to async {
                imageLoader.load(iconUrl).getOrNull()?.asImageBitmap()
            }
        }

        val icons = mutableListOf<FeaturedInstitutionIcon>()
        val iconInstitutions = mutableListOf<FeaturedInstitution>()

        // Awaiting in featured order means the leading icons are published once they have all finished, without
        // waiting for the ones only needed by the rotating slot.
        downloads.forEach { (institution, download) ->
            download.await()?.let { bitmap ->
                icons += FeaturedInstitutionIcon(id = institution.id, bitmap = bitmap)
                iconInstitutions += institution
            }

            if (icons.size == MINIMUM_ICON_COUNT && _icons.value.isEmpty()) {
                publish(icons.toList(), iconInstitutions.toList())
            }
        }

        if (icons.size < MINIMUM_ICON_COUNT) {
            onInsufficientIcons(icons.size)
        } else {
            publish(icons.toList(), iconInstitutions.toList())
        }
    }

    private fun publish(
        icons: List<FeaturedInstitutionIcon>,
        institutions: List<FeaturedInstitution>?,
    ) {
        if (icons.size >= MINIMUM_ICON_COUNT) {
            publishedInstitutions = institutions
            _icons.value = icons
        }
    }

    companion object {
        const val MINIMUM_ICON_COUNT = 4

        fun preloaded(
            icons: List<FeaturedInstitutionIcon>,
            onShown: () -> Unit = {},
        ): FeaturedInstitutions {
            return FeaturedInstitutions(onShown).apply {
                publish(icons, institutions = null)
            }
        }
    }
}

private fun String.isValidIconUrl(): Boolean {
    val uri = runCatching { URI(this) }.getOrNull() ?: return false
    return (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrEmpty()
}
