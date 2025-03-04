package com.stripe.android.connect

import androidx.annotation.RestrictTo

/**
 * Controller for a full screen component.
 */
@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
