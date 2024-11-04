package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
class Typography(
    /**
     * The base font size used for typography in sp.
     * This scales the value of other font size variables. If null the default will be used.
     */
    val fontSizeBase: Float? = null,

    /**
     * The style for extra-large headings. If null the default will be used. If null the default will be used.
     */
    val headingXl: Style? = null,

    /**
     * The style for large headings. If null the default will be used. If null the default will be used.
     */
    val headingLg: Style? = null,

    /**
     * The style for medium headings. If null the default will be used.
     */
    val headingMd: Style? = null,

    /**
     * The style for small headings. If null the default will be used.
     */
    val headingSm: Style? = null,

    /**
     * The style for extra-small headings. If null the default will be used.
     */
    val headingXs: Style? = null,

    /**
     * The style for medium body text. If null the default will be used.
     * The `textTransform` property is ignored.
     */
    val bodyMd: Style? = null,

    /**
     * The style for small body text. If null the default will be used.
     * The `textTransform` property is ignored.
     */
    val bodySm: Style? = null,

    /**
     * The style for medium label text. If null the default will be used.
     */
    val labelMd: Style? = null,

    /**
     * The style for small label text. If null the default will be used.
     */
    val labelSm: Style? = null
) : Parcelable {
    internal companion object {
        internal val default = Typography()
    }

    @PrivateBetaConnectSDK
    @Parcelize
    class Style(
        /**
         * The font size for the typography style in sp. If null the default will be used.
         */
        val fontSize: Float? = null,

        /**
         * The weight of the font used in this style (e.g., bold, normal) between 0-1000. If null the default will be used.
         */
        val fontWeight: Int? = null,

        /**
         * The text transformation applied (e.g., uppercase, lowercase).
         * Default is 'none'.
         */
        val textTransform: TextTransform = TextTransform.None
    ) : Parcelable
}
