package com.stripe.android

import android.app.Activity
import androidx.annotation.IntRange
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization

/**
 * Configuration for authentication mechanisms via [PaymentController]
 */
class PaymentAuthConfig private constructor(
    internal val stripe3ds2Config: Stripe3ds2Config
) {
    class Builder : ObjectBuilder<PaymentAuthConfig> {
        private var stripe3ds2Config: Stripe3ds2Config? = null

        fun set3ds2Config(stripe3ds2Config: Stripe3ds2Config): Builder {
            this.stripe3ds2Config = stripe3ds2Config
            return this
        }

        override fun build(): PaymentAuthConfig {
            return PaymentAuthConfig(requireNotNull(stripe3ds2Config))
        }
    }

    data class Stripe3ds2Config internal constructor(
        @IntRange(from = 5, to = 99) internal val timeout: Int,
        internal val uiCustomization: Stripe3ds2UiCustomization
    ) {
        init {
            checkValidTimeout(timeout)
        }

        private fun checkValidTimeout(timeout: Int) {
            require(!(timeout < 5 || timeout > 99)) {
                "Timeout value must be between 5 and 99, inclusive"
            }
        }

        class Builder : ObjectBuilder<Stripe3ds2Config> {
            private var timeout = DEFAULT_TIMEOUT
            private var uiCustomization =
                Stripe3ds2UiCustomization.Builder().build()

            /**
             * The 3DS2 challenge flow timeout, in seconds.
             *
             * If the timeout is reached, the challenge screen will close, control will return to
             * the launching Activity/Fragment, payment authentication will not succeed, and the
             * outcome will be represented as [StripeIntentResult.Outcome.TIMEDOUT].
             *
             * Must be a value between 5 and 99, inclusive.
             */
            fun setTimeout(@IntRange(from = 5, to = 99) timeout: Int): Builder {
                this.timeout = timeout
                return this
            }

            fun setUiCustomization(uiCustomization: Stripe3ds2UiCustomization): Builder {
                this.uiCustomization = uiCustomization
                return this
            }

            override fun build(): Stripe3ds2Config {
                return Stripe3ds2Config(
                    timeout = timeout,
                    uiCustomization = uiCustomization
                )
            }
        }

        companion object {
            internal const val DEFAULT_TIMEOUT = 5
        }
    }

    /**
     * Customization for 3DS2 buttons
     */
    data class Stripe3ds2ButtonCustomization internal constructor(
        internal val buttonCustomization: ButtonCustomization
    ) {

        class Builder : ObjectBuilder<Stripe3ds2ButtonCustomization> {
            private val buttonCustomization: ButtonCustomization = StripeButtonCustomization()

            /**
             * Set the button's background color
             *
             * @param hexColor The button's background color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setBackgroundColor(hexColor: String): Builder {
                buttonCustomization.backgroundColor = hexColor
                return this
            }

            /**
             * Set the corner radius of the button
             *
             * @param cornerRadius The radius of the button in pixels
             * @throws RuntimeException If the corner radius is less than 0
             */
            @Throws(RuntimeException::class)
            fun setCornerRadius(cornerRadius: Int): Builder {
                buttonCustomization.cornerRadius = cornerRadius
                return this
            }

            /**
             * Set the button's text font
             *
             * @param fontName The name of the font. If not found, default system font used
             * @throws RuntimeException If font name is null or empty
             */
            @Throws(RuntimeException::class)
            fun setTextFontName(fontName: String): Builder {
                buttonCustomization.textFontName = fontName
                return this
            }

            /**
             * Set the button's text color
             *
             * @param hexColor The button's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setTextColor(hexColor: String): Builder {
                buttonCustomization.textColor = hexColor
                return this
            }

            /**
             * Set the button's text size
             *
             * @param fontSize The size of the font in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @Throws(RuntimeException::class)
            fun setTextFontSize(fontSize: Int): Builder {
                buttonCustomization.textFontSize = fontSize
                return this
            }

            /**
             * Build the button customization
             *
             * @return The built button customization
             */
            override fun build(): Stripe3ds2ButtonCustomization {
                return Stripe3ds2ButtonCustomization(buttonCustomization)
            }
        }
    }

    /**
     * Customization for 3DS2 labels
     */
    data class Stripe3ds2LabelCustomization internal constructor(
        internal val labelCustomization: LabelCustomization
    ) {
        class Builder : ObjectBuilder<Stripe3ds2LabelCustomization> {
            private val labelCustomization: LabelCustomization = StripeLabelCustomization()

            /**
             * Set the text color for heading labels
             *
             * @param hexColor The heading labels's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setHeadingTextColor(hexColor: String): Builder {
                labelCustomization.headingTextColor = hexColor
                return this
            }

            /**
             * Set the heading label's font
             *
             * @param fontName The name of the font. Defaults to system font if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @Throws(RuntimeException::class)
            fun setHeadingTextFontName(fontName: String): Builder {
                labelCustomization.headingTextFontName = fontName
                return this
            }

            /**
             * Set the heading label's text size
             *
             * @param fontSize The size of the heading label in scaled-pixels (sp).
             * @throws RuntimeException If the font size is 0 or less
             */
            @Throws(RuntimeException::class)
            fun setHeadingTextFontSize(fontSize: Int): Builder {
                labelCustomization.headingTextFontSize = fontSize
                return this
            }

            /**
             * Set the label's font
             *
             * @param fontName The name of the font. Defaults to system font if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @Throws(RuntimeException::class)
            fun setTextFontName(fontName: String): Builder {
                labelCustomization.textFontName = fontName
                return this
            }

            /**
             * Set the label's text color
             *
             * @param hexColor The labels's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setTextColor(hexColor: String): Builder {
                labelCustomization.textColor = hexColor
                return this
            }

            /**
             * Set the label's text size
             *
             * @param fontSize The label's font size in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @Throws(RuntimeException::class)
            fun setTextFontSize(fontSize: Int): Builder {
                labelCustomization.textFontSize = fontSize
                return this
            }

            /**
             * Build the configured label customization
             *
             * @return The built label customization
             */
            override fun build(): Stripe3ds2LabelCustomization {
                return Stripe3ds2LabelCustomization(labelCustomization)
            }
        }
    }

    /**
     * Customization for 3DS2 text entry
     */
    data class Stripe3ds2TextBoxCustomization internal constructor(
        internal val textBoxCustomization: TextBoxCustomization
    ) {
        class Builder : ObjectBuilder<Stripe3ds2TextBoxCustomization> {
            private val textBoxCustomization: TextBoxCustomization = StripeTextBoxCustomization()

            /**
             * Set the width of the border around the text entry box
             *
             * @param borderWidth Width of the border in pixels
             * @throws RuntimeException If the border width is less than 0
             */
            @Throws(RuntimeException::class)
            fun setBorderWidth(borderWidth: Int): Builder {
                textBoxCustomization.borderWidth = borderWidth
                return this
            }

            /**
             * Set the color of the border around the text entry box
             *
             * @param hexColor The border's color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setBorderColor(hexColor: String): Builder {
                textBoxCustomization.borderColor = hexColor
                return this
            }

            /**
             * Set the corner radius of the text entry box
             *
             * @param cornerRadius The corner radius in pixels
             * @throws RuntimeException If the corner radius is less than 0
             */
            @Throws(RuntimeException::class)
            fun setCornerRadius(cornerRadius: Int): Builder {
                textBoxCustomization.cornerRadius = cornerRadius
                return this
            }

            /**
             * Set the font for text entry
             *
             * @param fontName The name of the font. The system default is used if not found.
             * @throws RuntimeException If the font name is null or empty.
             */
            @Throws(RuntimeException::class)
            fun setTextFontName(fontName: String): Builder {
                textBoxCustomization.textFontName = fontName
                return this
            }

            /**
             * Set the text color for text entry
             *
             * @param hexColor The text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setTextColor(hexColor: String): Builder {
                textBoxCustomization.textColor = hexColor
                return this
            }

            /**
             * Set the text entry font size
             *
             * @param fontSize The font size in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @Throws(RuntimeException::class)
            fun setTextFontSize(fontSize: Int): Builder {
                textBoxCustomization.textFontSize = fontSize
                return this
            }

            /**
             * Build the text box customization
             *
             * @return The text box customization
             */
            override fun build(): Stripe3ds2TextBoxCustomization {
                return Stripe3ds2TextBoxCustomization(textBoxCustomization)
            }
        }
    }

    /**
     * Customization for the 3DS2 toolbar
     */
    data class Stripe3ds2ToolbarCustomization internal constructor(
        internal val toolbarCustomization: ToolbarCustomization
    ) {
        class Builder : ObjectBuilder<Stripe3ds2ToolbarCustomization> {
            private val toolbarCustomization: ToolbarCustomization = StripeToolbarCustomization()

            /**
             * Set the toolbar's background color
             *
             * @param hexColor The background color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setBackgroundColor(hexColor: String): Builder {
                toolbarCustomization.backgroundColor = hexColor
                return this
            }

            /**
             * Set the status bar color, if not provided a darkened version of the background
             * color will be used.
             *
             * @param hexColor The status bar color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setStatusBarColor(hexColor: String): Builder {
                toolbarCustomization.statusBarColor = hexColor
                return this
            }

            /**
             * Set the toolbar's title
             *
             * @param headerText The toolbar's title text
             * @throws RuntimeException if the title is null or empty
             */
            @Throws(RuntimeException::class)
            fun setHeaderText(headerText: String): Builder {
                toolbarCustomization.headerText = headerText
                return this
            }

            /**
             * Set the toolbar's cancel button text
             *
             * @param buttonText The cancel button's text
             * @throws RuntimeException If the button text is null or empty
             */
            @Throws(RuntimeException::class)
            fun setButtonText(buttonText: String): Builder {
                toolbarCustomization.buttonText = buttonText
                return this
            }

            /**
             * Set the font for the title text
             *
             * @param fontName The name of the font. System default is used if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @Throws(RuntimeException::class)
            fun setTextFontName(fontName: String): Builder {
                toolbarCustomization.textFontName = fontName
                return this
            }

            /**
             * Set the color of the title text
             *
             * @param hexColor The title's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setTextColor(hexColor: String): Builder {
                toolbarCustomization.textColor = hexColor
                return this
            }

            /**
             * Set the title text's font size
             *
             * @param fontSize The size of the title text in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @Throws(RuntimeException::class)
            fun setTextFontSize(fontSize: Int): Builder {
                toolbarCustomization.textFontSize = fontSize
                return this
            }

            /**
             * Build the toolbar customization
             *
             * @return The built toolbar customization
             */
            override fun build(): Stripe3ds2ToolbarCustomization {
                return Stripe3ds2ToolbarCustomization(toolbarCustomization)
            }
        }
    }

    /**
     * Customizations for the 3DS2 UI
     */
    data class Stripe3ds2UiCustomization internal constructor(
        val uiCustomization: StripeUiCustomization
    ) {
        /**
         * The type of button for which customization can be set
         */
        enum class ButtonType {
            SUBMIT,
            CONTINUE,
            NEXT,
            CANCEL,
            RESEND,
            SELECT
        }

        class Builder private constructor(
            private val uiCustomization: StripeUiCustomization
        ) : ObjectBuilder<Stripe3ds2UiCustomization> {

            constructor() : this(StripeUiCustomization())

            private constructor(activity: Activity) : this(
                StripeUiCustomization.createWithAppTheme(activity)
            )

            @Throws(RuntimeException::class)
            private fun getUiButtonType(buttonType: ButtonType): UiCustomization.ButtonType {
                return when (buttonType) {
                    ButtonType.SUBMIT ->
                        UiCustomization.ButtonType.SUBMIT
                    ButtonType.CONTINUE ->
                        UiCustomization.ButtonType.CONTINUE
                    ButtonType.NEXT ->
                        UiCustomization.ButtonType.NEXT
                    ButtonType.CANCEL ->
                        UiCustomization.ButtonType.CANCEL
                    ButtonType.RESEND ->
                        UiCustomization.ButtonType.RESEND
                    ButtonType.SELECT ->
                        UiCustomization.ButtonType.SELECT
                }
            }

            /**
             * Set the customization for a particular button
             *
             * @param buttonCustomization The button customization data
             * @param buttonType The type of button to customize
             * @throws RuntimeException If any customization data is invalid
             */
            @Throws(RuntimeException::class)
            fun setButtonCustomization(
                buttonCustomization: Stripe3ds2ButtonCustomization,
                buttonType: ButtonType
            ): Builder {
                uiCustomization.setButtonCustomization(buttonCustomization.buttonCustomization,
                    getUiButtonType(buttonType))
                return this
            }

            /**
             * Set the customization data for the 3DS2 toolbar
             *
             * @param toolbarCustomization Toolbar customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @Throws(RuntimeException::class)
            fun setToolbarCustomization(
                toolbarCustomization: Stripe3ds2ToolbarCustomization
            ): Builder {
                uiCustomization
                    .setToolbarCustomization(toolbarCustomization.toolbarCustomization)
                return this
            }

            /**
             * Set the 3DS2 label customization
             *
             * @param labelCustomization Label customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @Throws(RuntimeException::class)
            fun setLabelCustomization(
                labelCustomization: Stripe3ds2LabelCustomization
            ): Builder {
                uiCustomization.setLabelCustomization(labelCustomization.labelCustomization)
                return this
            }

            /**
             * Set the 3DS2 text box customization
             *
             * @param textBoxCustomization Text box customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @Throws(RuntimeException::class)
            fun setTextBoxCustomization(
                textBoxCustomization: Stripe3ds2TextBoxCustomization
            ): Builder {
                uiCustomization
                    .setTextBoxCustomization(textBoxCustomization.textBoxCustomization)
                return this
            }

            /**
             * Set the accent color
             *
             * @param hexColor The accent color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @Throws(RuntimeException::class)
            fun setAccentColor(hexColor: String): Builder {
                uiCustomization.setAccentColor(hexColor)
                return this
            }

            /**
             * Build the UI customization
             *
             * @return the built UI customization
             */
            override fun build(): Stripe3ds2UiCustomization {
                return Stripe3ds2UiCustomization(uiCustomization)
            }

            companion object {
                @JvmStatic
                fun createWithAppTheme(activity: Activity): Builder {
                    return Builder(activity)
                }
            }
        }
    }

    companion object {
        private var instance: PaymentAuthConfig? = null

        private val DEFAULT = Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder().build())
            .build()

        @JvmStatic
        fun init(config: PaymentAuthConfig) {
            instance = config
        }

        @JvmStatic
        fun get(): PaymentAuthConfig {
            return instance ?: DEFAULT
        }

        @JvmSynthetic
        internal fun reset() {
            instance = null
        }
    }
}
