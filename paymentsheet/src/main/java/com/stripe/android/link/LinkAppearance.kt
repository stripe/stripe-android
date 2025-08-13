package com.stripe.android.link

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * @param borderSelected The color used for the border of selected views.
     */
    @Poko
    @Parcelize
    @TypeParceler<Color, ColorParceler>()
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Colors(
        val primary: Color,
        val borderSelected: Color
    ) : Parcelable {
        internal companion object {
            fun default(isDark: Boolean) = Colors(
                primary = LinkThemeConfig.colors(isDark = isDark).buttonPrimary,
                borderSelected = LinkThemeConfig.colors(isDark = isDark).borderSelected,
            )
        }
    }

    /**
     * The light/dark mode style of the appearance..
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
