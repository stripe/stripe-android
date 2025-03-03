package com.stripe.android.connect

/**
 * Controller for a full screen component.
 */
@PrivateBetaConnectSDK
interface StripeComponentController<Listener : StripeEmbeddedComponentListener> {
    /**
     * Listener of component events.
     */
    var listener: Listener?

    /**
     * Optional listener of component dismissal.
     */
    var onDismissListener: OnDismissListener?

    /**
     * Shows the component.
     */
    fun show()

    /**
     * Dismisses the component.
     */
    fun dismiss()

    /**
     * Listener of component dismissal.
     */
    fun interface OnDismissListener {
        /**
         * Called when the full screen component is dismissed.
         */
        fun onDismiss()
    }
}
