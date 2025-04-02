package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
@Poko
class Typography private constructor(
    internal val fontFamily: String?,
    internal val fontSizeBase: Float?,
    internal val headingXl: Style?,
    internal val headingLg: Style?,
    internal val headingMd: Style?,
    internal val headingSm: Style?,
    internal val headingXs: Style?,
    internal val bodyMd: Style?,
    internal val bodySm: Style?,
    internal val labelMd: Style?,
    internal val labelSm: Style?
) : Parcelable {

    @Suppress("TooManyFunctions")
    class Builder {
        private var fontFamily: String? = null
        private var fontSizeBase: Float? = null
        private var headingXl: Style? = null
        private var headingLg: Style? = null
        private var headingMd: Style? = null
        private var headingSm: Style? = null
        private var headingXs: Style? = null
        private var bodyMd: Style? = null
        private var bodySm: Style? = null
        private var labelMd: Style? = null
        private var labelSm: Style? = null

        /**
         * The font family used throughout the application. Refers to the [CustomFontSource] field.
         * If null the default will be used.
         */
        fun fontFamily(fontFamily: String?): Builder =
            apply { this.fontFamily = fontFamily }

        /**
         * The base font size used for typography in sp.
         * This scales the value of other font size variables. If null the default will be used.
         */
        fun fontSizeBase(fontSizeBase: Float?): Builder =
            apply { this.fontSizeBase = fontSizeBase }

        /**
         * The style for extra-large headings. If null the default will be used.
         */
        fun headingXl(headingXl: Style?): Builder =
            apply { this.headingXl = headingXl }

        /**
         * The style for large headings. If null the default will be used.
         */
        fun headingLg(headingLg: Style?): Builder =
            apply { this.headingLg = headingLg }

        /**
         * The style for medium headings. If null the default will be used.
         */
        fun headingMd(headingMd: Style?): Builder =
            apply { this.headingMd = headingMd }

        /**
         * The style for small headings. If null the default will be used.
         */
        fun headingSm(headingSm: Style?): Builder =
            apply { this.headingSm = headingSm }

        /**
         * The style for extra-small headings. If null the default will be used.
         */
        fun headingXs(headingXs: Style?): Builder =
            apply { this.headingXs = headingXs }

        /**
         * The style for medium body text. If null the default will be used.
         * The `textTransform` property is ignored.
         */
        fun bodyMd(bodyMd: Style?): Builder =
            apply { this.bodyMd = bodyMd }

        /**
         * The style for small body text. If null the default will be used.
         * The `textTransform` property is ignored.
         */
        fun bodySm(bodySm: Style?): Builder =
            apply { this.bodySm = bodySm }

        /**
         * The style for medium label text. If null the default will be used.
         */
        fun labelMd(labelMd: Style?): Builder =
            apply { this.labelMd = labelMd }

        /**
         * The style for small label text. If null the default will be used.
         */
        fun labelSm(labelSm: Style?): Builder =
            apply { this.labelSm = labelSm }

        fun build(): Typography {
            return Typography(
                fontFamily = fontFamily,
                fontSizeBase = fontSizeBase,
                headingXl = headingXl,
                headingLg = headingLg,
                headingMd = headingMd,
                headingSm = headingSm,
                headingXs = headingXs,
                bodyMd = bodyMd,
                bodySm = bodySm,
                labelMd = labelMd,
                labelSm = labelSm
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }

    /**
     * @param fontSize The font size for the typography style in sp. If null the default will be used.
     * @param fontWeight The weight of the font used in this style (e.g., bold, normal) between 0-1000.
     *  If null the default will be used.
     * @param textTransform The text transformation applied (e.g., uppercase, lowercase). Default is 'none'.
     */
    @PrivateBetaConnectSDK
    @Parcelize
    @Poko
    class Style(
        internal val fontSize: Float? = null,
        internal val fontWeight: Int? = null,
        internal val textTransform: TextTransform = TextTransform.None
    ) : Parcelable
}
