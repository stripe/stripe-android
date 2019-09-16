package com.stripe.android.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

internal class StripeColorUtils(private val context: Context) {

    fun getThemeAccentColor(): TypedValue {
        @IdRes val colorAttr: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.colorAccent
        } else {
            // Get colorAccent defined for AppCompat
            getIdentifier("colorAccent")
        }
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue
    }

    fun getThemeColorControlNormal(): TypedValue {
        @IdRes val colorAttr: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.colorControlNormal
        } else {
            // Get colorControlNormal defined for AppCompat
            getIdentifier("colorControlNormal")
        }
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue
    }

    private fun getIdentifier(attrName: String): Int {
        return context.resources
            .getIdentifier(attrName, "attr", context.packageName)
    }

    fun getThemeTextColorSecondary(): TypedValue {
        @IdRes val colorAttr: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.textColorSecondary
        } else {
            // Get textColorSecondary defined for AppCompat
            android.R.color.secondary_text_light
        }
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue
    }

    fun getThemeTextColorPrimary(): TypedValue {
        @IdRes val colorAttr: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.textColorPrimary
        } else {
            // Get textColorPrimary defined for AppCompat
            android.R.color.primary_text_light
        }
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue
    }

    fun getTintedIconWithAttribute(
        theme: Resources.Theme,
        @AttrRes attributeResource: Int,
        @DrawableRes iconResourceId: Int
    ): Drawable {
        val typedValue = TypedValue()
        theme.resolveAttribute(attributeResource, typedValue, true)
        @ColorInt val color = typedValue.data
        val icon = ContextCompat.getDrawable(context, iconResourceId)
        val compatIcon = DrawableCompat.wrap(icon!!)
        DrawableCompat.setTint(compatIcon.mutate(), color)
        return compatIcon
    }

    companion object {
        /**
         * Check to see whether the color int is essentially transparent.
         *
         * @param color a [ColorInt] integer
         * @return `true` if this color is too transparent to be seen
         */
        @JvmStatic
        fun isColorTransparent(@ColorInt color: Int): Boolean {
            return Color.alpha(color) < 0x10
        }

        /**
         * A crude mechanism by which we check whether or not a color is "dark."
         * This is subject to much interpretation, but we attempt to follow traditional
         * design standards.
         *
         * Formula comes from W3C standards and conventional theory about how to calculate the
         * "brightness" of a color, often thought of as how far along the spectrum from white to black
         * the gray-scale version would be.
         *
         * See [W3C's Techniques For Accessibility Evaluation And Repair Tools](https://www.w3.org/TR/AERT#color-contrast)
         * and [RGB colour space](http://paulbourke.net/miscellaneous/colourspace/) for further reading.
         *
         * @param color an integer representation of a color
         * @return `true` if the color is "dark," else [false]
         */
        @JvmStatic
        fun isColorDark(@ColorInt color: Int): Boolean {
            val luminescence = 0.299 * Color.red(color) +
                    0.587 * Color.green(color) +
                    0.114 * Color.blue(color)

            // Because the colors are all hex integers.
            val luminescencePercentage = luminescence / 255
            return luminescencePercentage <= 0.5
        }
    }
}
