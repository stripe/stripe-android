package com.stripe.android.connect.appearance.fonts

import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/**
 * The font family corresponding custom fonts embedded in your app binary.
 * Each of the fonts in this family must be either a .ttf or .otf file file contained in your assets folder.
 * A custom font can be used in the EmbeddedComponent SDK by specifying the font family
 * String in the [Typography] object.
 */
@PrivateBetaConnectSDK
@Serializable
@Poko
class CustomFontSource(
    /**
     * The font family loaded from your assets folder.
     */
    internal val assetsFilePath: String,

    /**
     * The name of this font family.
     */
    internal val name: String,

    /**
     * The weight of this font by default (e.g., bold, normal) between 0-1000. If null the default will be used.
     */
    internal val weight: Int? = null,
) {
    init {
        require(weight == null || weight in 0..1000) {
            "Weight cannot be smaller than 0 or larger than 1000"
        }
    }
}
