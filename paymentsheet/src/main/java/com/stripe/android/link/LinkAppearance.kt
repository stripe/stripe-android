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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkAppearance {

    private var lightColors: Colors? = null
    private var darkColors: Colors? = null
    private var style: Style = Style.AUTOMATIC
    private var primaryButton: PrimaryButton? = null

    fun lightColors(lightColors: Colors) = apply {
        this.lightColors = lightColors
    }

    fun darkColors(darkColors: Colors) = apply {
        this.darkColors = darkColors
    }

    fun style(style: Style) = apply {
        this.style = style
    }

    fun primaryButton(primaryButton: PrimaryButton) = apply {
        this.primaryButton = primaryButton
    }

    @Poko
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State(
        val lightColors: Colors.State,
        val darkColors: Colors.State,
        val style: Style,
        val primaryButton: PrimaryButton.State,
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun build(): State {
        return State(
            lightColors = (lightColors ?: Colors()).build(isDark = false),
            darkColors = (darkColors ?: Colors()).build(isDark = true),
            style = style,
            primaryButton = (primaryButton ?: PrimaryButton()).build(),
        )
    }

    /**
     * Color configuration for Link components.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Colors {
        private var primary: Color? = null
        private var contentOnPrimary: Color? = null
        private var borderSelected: Color? = null

        fun primary(primary: Color) = apply {
            this.primary = primary
        }

        fun contentOnPrimary(contentOnPrimary: Color) = apply {
            this.contentOnPrimary = contentOnPrimary
        }

        fun borderSelected(borderSelected: Color) = apply {
            this.borderSelected = borderSelected
        }

        @Poko
        @Parcelize
        @TypeParceler<Color, ColorParceler>()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class State(
            val primary: Color,
            val contentOnPrimary: Color,
            val borderSelected: Color,
        ) : Parcelable

        internal fun build(isDark: Boolean): State {
            val defaults = LinkThemeConfig.colors(isDark = isDark)
            return State(
                primary = primary ?: defaults.buttonPrimary,
                contentOnPrimary = contentOnPrimary ?: defaults.onButtonPrimary,
                borderSelected = borderSelected ?: defaults.borderSelected,
            )
        }
    }

    /**
     * The light/dark mode style of the appearance.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Style : Parcelable {
        AUTOMATIC,
        ALWAYS_LIGHT,
        ALWAYS_DARK
    }

    /**
     * Configuration for primary button styling.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PrimaryButton {
        private var cornerRadiusDp: Float? = null
        private var heightDp: Float? = null

        fun cornerRadiusDp(cornerRadiusDp: Float?) = apply {
            this.cornerRadiusDp = cornerRadiusDp
        }

        fun heightDp(heightDp: Float?) = apply {
            this.heightDp = heightDp
        }

        @Poko
        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class State(
            val cornerRadiusDp: Float? = null,
            val heightDp: Float? = null
        ) : Parcelable

        internal fun build(): State = State(
            cornerRadiusDp = cornerRadiusDp,
            heightDp = heightDp,
        )
    }

    private object ColorParceler : Parceler<Color> {
        override fun create(parcel: Parcel): Color =
            Color(parcel.readInt())

        override fun Color.write(parcel: Parcel, flags: Int) =
            parcel.writeInt(this.toArgb())
    }
}
