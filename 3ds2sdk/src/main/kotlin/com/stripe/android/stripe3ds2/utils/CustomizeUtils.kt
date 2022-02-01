package com.stripe.android.stripe3ds2.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import com.stripe.android.stripe3ds2.init.ui.Customization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object CustomizeUtils {
    private const val DEFAULT_DARKEN_FACTOR = 0.8f

    /**
     * If the UI Customization provides an accent color, use that for the progress bar otherwise
     * do not set anything and use the default 3DS2 theme values
     *
     * @param progressBar the progress bar to apply the color to
     * @param uiCustomization the UI Customization info to pull the progress bar color from
     */
    internal fun applyProgressBarColor(
        progressBar: ProgressBar,
        uiCustomization: UiCustomization?
    ) {
        uiCustomization?.accentColor?.let { accentColor ->
            progressBar.indeterminateTintList = ColorStateList.valueOf(
                Color.parseColor(accentColor)
            )
        }
    }

    /**
     * Given the toolbar's background color, set the status bar color to a slightly darker
     * version if API levels allow.
     *
     * @param activity activity to set the status bar color of
     * @param statusBarColor the Toolbar's background color as reference to darken
     */
    fun setStatusBarColor(
        activity: AppCompatActivity,
        @ColorInt statusBarColor: Int
    ) {
        activity.window.statusBarColor = statusBarColor
    }

    /**
     * Darken the given color by the given factor
     *
     * @param color the color to darken
     * @param factor the amount to darken by
     * @return the color darkened by the given factor
     */
    @ColorInt
    internal fun darken(@ColorInt color: Int, factor: Float): Int {
        val alpha = Color.alpha(color)
        val red = (Color.red(color) * factor).toInt()
        val green = (Color.green(color) * factor).toInt()
        val blue = (Color.blue(color) * factor).toInt()

        return Color.argb(
            alpha,
            min(max(red, 0), 255),
            min(max(green, 0), 255),
            min(max(blue, 0), 255)
        )
    }

    /**
     * Given a color int, return the hex string with alpha
     *
     * @param color the color int to transform into a hex string
     * @return #AARRGGBB hex string representing the given color int
     */
    @JvmStatic
    fun colorIntToHex(@ColorInt color: Int): String {
        val alpha = Color.alpha(color)
        val blue = Color.blue(color)
        val green = Color.green(color)
        val red = Color.red(color)

        return "#" + String.format(Locale.ENGLISH, "%02X", alpha) +
            String.format(Locale.ENGLISH, "%02X", red) +
            String.format(Locale.ENGLISH, "%02X", green) +
            String.format(Locale.ENGLISH, "%02X", blue)
    }

    /**
     * Build a styled spanned string from a Customization object
     *
     * @param text The text to style
     * @param customization customization to style text with
     * @return spannable text customized per customization
     */
    fun buildStyledText(
        context: Context,
        text: String,
        customization: Customization
    ): SpannableString {
        val styledText = SpannableString(text)
        customization.textColor?.let { textColor ->
            styledText.setSpan(
                ForegroundColorSpan(Color.parseColor(textColor)),
                0, styledText.length, 0
            )
        }

        customization.textFontSize.takeIf { it > 0 }?.let { textFontSize ->
            val fontSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textFontSize.toFloat(),
                context.resources.displayMetrics
            ).toInt()
            val sizeSpan = AbsoluteSizeSpan(fontSize)
            styledText.setSpan(sizeSpan, 0, styledText.length, 0)
        }

        customization.textFontName?.let { textFontName ->
            styledText.setSpan(TypefaceSpan(textFontName), 0, styledText.length, 0)
        }
        return styledText
    }

    /**
     * Darken the given color by the default factor of 0.8
     *
     * @param color the color to darken
     * @return the color darkened by the given factor
     */
    @ColorInt
    internal fun darken(@ColorInt color: Int): Int {
        return darken(color, DEFAULT_DARKEN_FACTOR)
    }

    /**
     * Ensure the color is valid and parsable
     *
     * @param hexColor the color input to validate
     * @throws InvalidInputException if the color cannot be parsed
     */
    @Throws(InvalidInputException::class)
    @JvmStatic
    fun requireValidColor(hexColor: String): String {
        return runCatching {
            Color.parseColor(hexColor)
            hexColor
        }.getOrElse {
            throw InvalidInputException("Unable to parse color: $hexColor")
        }
    }

    /**
     * Ensure font size is greater than 0
     *
     * @param fontSize the font size to validate
     * @throws InvalidInputException if the font size is less than or equal to 0
     */
    @Throws(InvalidInputException::class)
    @JvmStatic
    fun requireValidFontSize(fontSize: Int): Int {
        if (fontSize <= 0) {
            throw InvalidInputException("Font size must be greater than 0")
        }
        return fontSize
    }

    /**
     * Ensure dimension such as corner radius and border width are 0 or greater
     *
     * @param dimension the dimension to validate
     * @throws InvalidInputException if dimension is less than 0
     */
    @Throws(InvalidInputException::class)
    @JvmStatic
    fun requireValidDimension(dimension: Int): Int {
        if (dimension < 0) {
            throw InvalidInputException("Dimension must be greater or equal to 0")
        }
        return dimension
    }

    /**
     * Ensure strings like font name and titles are not null/empty
     *
     * @param string the string to validate
     * @throws InvalidInputException if the string is null or empty
     */
    @Throws(InvalidInputException::class)
    @JvmStatic
    fun requireValidString(string: String): String {
        if (string.isBlank()) {
            throw InvalidInputException("String must not be null or empty")
        }
        return string
    }
}
