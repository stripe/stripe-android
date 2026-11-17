package com.stripe.android.paymentelement.embedded.linkinline

/**
 * How inline Link verification changes the Embedded Payment Element's list of payment methods.
 */
internal data class EmbeddedLinkInlineListOptions(
    /**
     * Whether the Link row is the first row. Once the card shows, Link stays first so rows never reorder under the
     * customer.
     */
    val placesLinkFirst: Boolean,
    /**
     * Whether a selected Link row shows the payment details picked in the Link wallet with a "Change" accessory.
     */
    val linkPickIsChangeable: Boolean,
) {
    companion object {
        /**
         * For lists without inline Link verification.
         */
        val Disabled = EmbeddedLinkInlineListOptions(
            placesLinkFirst = false,
            linkPickIsChangeable = false,
        )
    }
}
