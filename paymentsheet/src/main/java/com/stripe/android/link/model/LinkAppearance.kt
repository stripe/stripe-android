package com.stripe.android.link.model

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.link.theme.LinkThemeConfig
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Configuration for customizing the visual appearance of Link components.
 *
 * @param lightColors Colors to use when the system is in light mode.
 * @param darkColors Colors to use when the system is in dark mode.
 * @param primaryButton Configuration for primary button styling (corner radius, height).
 *
 */
@Poko
@Parcelize
class LinkAppearance(
    val lightColors: Colors = Colors.default(isDark = false),
    val darkColors: Colors = Colors.default(isDark = true),
    val style: Style,
    val primaryButton: PrimaryButton = PrimaryButton()
) : Parcelable {

    /**
     * Color configuration for Link components.
     *
     * @param primary The primary color used for buttons, highlights, and interactive elements.
     */
    @Poko
    @Parcelize
    @TypeParceler<Color, ColorParceler>()
    class Colors(
        val primary: Color
    ) : Parcelable {
        internal companion object {
            fun default(isDark: Boolean) = Colors(
                primary = LinkThemeConfig.colors(isDark = isDark).buttonPrimary
            )
        }
    }

    /**
     * The light/dark mode style of the appearance..
     */
    @Parcelize
    enum class Style : Parcelable {
        /**
         * Automatic based on system preference
         */
        AUTOMATIC,

        /**
         * Always light mode
         */
        ALWAYS_LIGHT,

        /**
         * Always dark mode
         */
        ALWAYS_DARK
    }

    /**
     * Configuration for primary button styling.
     *
     * @param cornerRadiusDp Corner radius for primary buttons in density-independent pixels.
     *                       If null, uses the default Link theme corner radius.
     * @param heightDp Height for primary buttons in density-independent pixels.
     *                 If null, uses the default Link theme button height.
     */
    @Poko
    @Parcelize
    class PrimaryButton(
        val cornerRadiusDp: Float? = null,
        val heightDp: Float? = null
    ) : Parcelable

    private object ColorParceler : Parceler<Color> {
        override fun create(parcel: Parcel): Color =
            Color(parcel.readInt())
        override fun Color.write(parcel: Parcel, flags: Int) =
            parcel.writeInt(this.toArgb())
    }
}
