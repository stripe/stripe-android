package com.stripe.android.elements

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement @Inject internal constructor() {

    fun present() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var appearance: Appearance = Appearance()
        private var buttonTitle: String? = null
        private var title: String? = null

        fun appearance(appearance: Appearance): Configuration = apply {
            this.appearance = appearance
        }

        fun buttonTitle(buttonTitle: String?): Configuration = apply {
            this.buttonTitle = buttonTitle
        }

        fun title(title: String?): Configuration = apply {
            this.title = title
        }

        @Parcelize
        internal data class State(
            val appearance: Appearance.State,
            val buttonTitle: String?,
            val title: String?,
        ) : Parcelable

        internal fun build(): State = State(
            appearance = appearance.build(),
            buttonTitle = buttonTitle,
            title = title,
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Appearance {
        private var colorsLight: Colors = Colors()
        private var colorsDark: Colors = Colors()
        private var shapes: Shapes = Shapes()
        private var typography: Typography = Typography()

        fun colorsLight(colorsLight: Colors): Appearance = apply {
            this.colorsLight = colorsLight
        }

        fun colorsDark(colorsDark: Colors): Appearance = apply {
            this.colorsDark = colorsDark
        }

        fun shapes(shapes: Shapes): Appearance = apply {
            this.shapes = shapes
        }

        fun typography(typography: Typography): Appearance = apply {
            this.typography = typography
        }

        @Parcelize
        internal data class State(
            val colorsLight: Colors.State,
            val colorsDark: Colors.State,
            val shapes: Shapes.State,
            val typography: Typography.State,
        ) : Parcelable

        internal fun build(): State = State(
            colorsLight = colorsLight.build(),
            colorsDark = colorsDark.build(),
            shapes = shapes.build(),
            typography = typography.build(),
        )

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("TooManyFunctions")
        class Colors {
            private var primary: Color? = null
            private var surface: Color? = null
            private var component: Color? = null
            private var componentBorder: Color? = null
            private var componentDivider: Color? = null
            private var onComponent: Color? = null
            private var onSurface: Color? = null
            private var subtitle: Color? = null
            private var placeholderText: Color? = null
            private var appBarIcon: Color? = null
            private var error: Color? = null

            fun primary(primary: Color): Colors = apply {
                this.primary = primary
            }

            fun surface(surface: Color): Colors = apply {
                this.surface = surface
            }

            fun component(component: Color): Colors = apply {
                this.component = component
            }

            fun componentBorder(componentBorder: Color): Colors = apply {
                this.componentBorder = componentBorder
            }

            fun componentDivider(componentDivider: Color): Colors = apply {
                this.componentDivider = componentDivider
            }

            fun onComponent(onComponent: Color): Colors = apply {
                this.onComponent = onComponent
            }

            fun onSurface(onSurface: Color): Colors = apply {
                this.onSurface = onSurface
            }

            fun subtitle(subtitle: Color): Colors = apply {
                this.subtitle = subtitle
            }

            fun placeholderText(placeholderText: Color): Colors = apply {
                this.placeholderText = placeholderText
            }

            fun appBarIcon(appBarIcon: Color): Colors = apply {
                this.appBarIcon = appBarIcon
            }

            fun error(error: Color): Colors = apply {
                this.error = error
            }

            @Parcelize
            internal data class State(
                @ColorInt val primary: Int?,
                @ColorInt val surface: Int?,
                @ColorInt val component: Int?,
                @ColorInt val componentBorder: Int?,
                @ColorInt val componentDivider: Int?,
                @ColorInt val onComponent: Int?,
                @ColorInt val onSurface: Int?,
                @ColorInt val subtitle: Int?,
                @ColorInt val placeholderText: Int?,
                @ColorInt val appBarIcon: Int?,
                @ColorInt val error: Int?,
            ) : Parcelable

            internal fun build(): State = State(
                primary = primary?.toArgb(),
                surface = surface?.toArgb(),
                component = component?.toArgb(),
                componentBorder = componentBorder?.toArgb(),
                componentDivider = componentDivider?.toArgb(),
                onComponent = onComponent?.toArgb(),
                onSurface = onSurface?.toArgb(),
                subtitle = subtitle?.toArgb(),
                placeholderText = placeholderText?.toArgb(),
                appBarIcon = appBarIcon?.toArgb(),
                error = error?.toArgb(),
            )
        }

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Shapes {
            private var cornerRadiusDp: Float? = null
            private var borderStrokeWidthDp: Float? = null

            fun cornerRadiusDp(cornerRadiusDp: Float): Shapes = apply {
                this.cornerRadiusDp = cornerRadiusDp
            }

            fun borderStrokeWidthDp(borderStrokeWidthDp: Float): Shapes = apply {
                this.borderStrokeWidthDp = borderStrokeWidthDp
            }

            @Parcelize
            internal data class State(
                val cornerRadiusDp: Float?,
                val borderStrokeWidthDp: Float?,
            ) : Parcelable

            internal fun build(): State = State(
                cornerRadiusDp = cornerRadiusDp,
                borderStrokeWidthDp = borderStrokeWidthDp,
            )
        }

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Typography {
            private var sizeScaleFactor: Float? = null

            @FontRes
            private var fontResId: Int? = null

            fun sizeScaleFactor(sizeScaleFactor: Float): Typography = apply {
                this.sizeScaleFactor = sizeScaleFactor
            }

            fun fontResId(@FontRes fontResId: Int?): Typography = apply {
                this.fontResId = fontResId
            }

            @Parcelize
            internal data class State(
                val sizeScaleFactor: Float?,
                @FontRes val fontResId: Int?,
            ) : Parcelable

            internal fun build(): State = State(
                sizeScaleFactor = sizeScaleFactor,
                fontResId = fontResId,
            )
        }
    }
}
