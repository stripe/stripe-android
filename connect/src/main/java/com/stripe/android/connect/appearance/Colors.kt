package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
@Poko
class Colors private constructor(
    @ColorInt internal val primary: Int?,
    @ColorInt internal val background: Int?,
    @ColorInt internal val text: Int?,
    @ColorInt internal val secondaryText: Int?,
    @ColorInt internal val danger: Int?,
    @ColorInt internal val border: Int?,
    @ColorInt internal val actionPrimaryText: Int?,
    @ColorInt internal val actionSecondaryText: Int?,
    @ColorInt internal val offsetBackground: Int?,
    @ColorInt internal val formBackground: Int?,
    @ColorInt internal val formHighlightBorder: Int?,
    @ColorInt internal val formAccent: Int?
) : Parcelable {

    @Suppress("TooManyFunctions")
    class Builder {
        @ColorInt private var primary: Int? = null

        @ColorInt private var background: Int? = null

        @ColorInt private var text: Int? = null

        @ColorInt private var secondaryText: Int? = null

        @ColorInt private var danger: Int? = null

        @ColorInt private var border: Int? = null

        @ColorInt private var actionPrimaryText: Int? = null

        @ColorInt private var actionSecondaryText: Int? = null

        @ColorInt private var offsetBackground: Int? = null

        @ColorInt private var formBackground: Int? = null

        @ColorInt private var formHighlightBorder: Int? = null

        @ColorInt private var formAccent: Int? = null

        /**
         * The primary color used throughout the components. If null the default will be used.
         */
        fun primary(@ColorInt primary: Int?): Builder =
            apply { this.primary = primary }

        /**
         * The background color for components. If null the default will be used.
         */
        fun background(@ColorInt background: Int?): Builder =
            apply { this.background = background }

        /**
         * The primary text color used for regular content. If null the default will be used.
         */
        fun text(@ColorInt text: Int?): Builder =
            apply { this.text = text }

        /**
         * The secondary text color used for less emphasized content. If null the default will be used.
         */
        fun secondaryText(@ColorInt secondaryText: Int?): Builder =
            apply { this.secondaryText = secondaryText }

        /**
         * The color used to indicate errors or destructive actions. If null the default will be used.
         */
        fun danger(@ColorInt danger: Int?): Builder =
            apply { this.danger = danger }

        /**
         * The color used for component borders. If null the default will be used.
         */
        fun border(@ColorInt border: Int?): Builder =
            apply { this.border = border }

        /**
         * The color used for primary actions and link text. If null the default will be used.
         */
        fun actionPrimaryText(@ColorInt actionPrimaryText: Int?): Builder =
            apply { this.actionPrimaryText = actionPrimaryText }

        /**
         * The color used for secondary actions and link text. If null the default will be used.
         */
        fun actionSecondaryText(@ColorInt actionSecondaryText: Int?): Builder =
            apply { this.actionSecondaryText = actionSecondaryText }

        /**
         * The background color used to highlight information. If null the default will be used.
         */
        fun offsetBackground(@ColorInt offsetBackground: Int?): Builder =
            apply { this.offsetBackground = offsetBackground }

        /**
         * The background color used for form fields. If null the default will be used.
         */
        fun formBackground(@ColorInt formBackground: Int?): Builder =
            apply { this.formBackground = formBackground }

        /**
         * The border color used to highlight form items when focused. If null the default will be used.
         */
        fun formHighlightBorder(@ColorInt formHighlightBorder: Int?): Builder =
            apply { this.formHighlightBorder = formHighlightBorder }

        /**
         * The accent color used for filling form elements like checkboxes or radio buttons.
         * If null the default will be used.
         */
        fun formAccent(@ColorInt formAccent: Int?): Builder =
            apply { this.formAccent = formAccent }

        fun build(): Colors {
            return Colors(
                primary = primary,
                background = background,
                text = text,
                secondaryText = secondaryText,
                danger = danger,
                border = border,
                actionPrimaryText = actionPrimaryText,
                actionSecondaryText = actionSecondaryText,
                offsetBackground = offsetBackground,
                formBackground = formBackground,
                formHighlightBorder = formHighlightBorder,
                formAccent = formAccent
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
