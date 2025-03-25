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
        fun setPrimary(@ColorInt primary: Int?) =
            apply { this.primary = primary }

        /**
         * The background color for components. If null the default will be used.
         */
        fun setBackground(@ColorInt background: Int?) =
            apply { this.background = background }

        /**
         * The primary text color used for regular content. If null the default will be used.
         */
        fun setText(@ColorInt text: Int?) =
            apply { this.text = text }

        /**
         * The secondary text color used for less emphasized content. If null the default will be used.
         */
        fun setSecondaryText(@ColorInt secondaryText: Int?) =
            apply { this.secondaryText = secondaryText }

        /**
         * The color used to indicate errors or destructive actions. If null the default will be used.
         */
        fun setDanger(@ColorInt danger: Int?) =
            apply { this.danger = danger }

        /**
         * The color used for component borders. If null the default will be used.
         */
        fun setBorder(@ColorInt border: Int?) =
            apply { this.border = border }

        /**
         * The color used for primary actions and link text. If null the default will be used.
         */
        fun setActionPrimaryText(@ColorInt actionPrimaryText: Int?) =
            apply { this.actionPrimaryText = actionPrimaryText }

        /**
         * The color used for secondary actions and link text. If null the default will be used.
         */
        fun setActionSecondaryText(@ColorInt actionSecondaryText: Int?) =
            apply { this.actionSecondaryText = actionSecondaryText }

        /**
         * The background color used to highlight information. If null the default will be used.
         */
        fun setOffsetBackground(@ColorInt offsetBackground: Int?) =
            apply { this.offsetBackground = offsetBackground }

        /**
         * The background color used for form fields. If null the default will be used.
         */
        fun setFormBackground(@ColorInt formBackground: Int?) =
            apply { this.formBackground = formBackground }

        /**
         * The border color used to highlight form items when focused. If null the default will be used.
         */
        fun setFormHighlightBorder(@ColorInt formHighlightBorder: Int?) =
            apply { this.formHighlightBorder = formHighlightBorder }

        /**
         * The accent color used for filling form elements like checkboxes or radio buttons.
         * If null the default will be used.
         */
        fun setFormAccent(@ColorInt formAccent: Int?) =
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
