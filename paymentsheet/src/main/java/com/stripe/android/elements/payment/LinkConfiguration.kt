package com.stripe.android.elements.payment

import android.os.Parcelable
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration related to Link.
 */
@Poko
@Parcelize
class LinkConfiguration internal constructor(
    internal val display: Display,
    internal val collectMissingBillingDetailsForExistingPaymentMethods: Boolean,
    internal val allowUserEmailEdits: Boolean,
) : Parcelable {

    @JvmOverloads
    constructor(
        display: Display = Display.Automatic
    ) : this(
        display = display,
        collectMissingBillingDetailsForExistingPaymentMethods = true,
        allowUserEmailEdits = true,
    )

    internal val shouldDisplay: Boolean
        get() = when (display) {
            Display.Automatic -> true
            Display.Never -> false
        }

    class Builder {
        private var display: Display = Display.Automatic
        private var collectMissingBillingDetailsForExistingPaymentMethods: Boolean = true

        fun display(display: Display) = apply {
            this.display = display
        }

        @CollectMissingLinkBillingDetailsPreview
        fun collectMissingBillingDetailsForExistingPaymentMethods(
            collectMissingBillingDetailsForExistingPaymentMethods: Boolean
        ) = apply {
            this.collectMissingBillingDetailsForExistingPaymentMethods =
                collectMissingBillingDetailsForExistingPaymentMethods
        }

        fun build() = LinkConfiguration(
            display = display,
            collectMissingBillingDetailsForExistingPaymentMethods =
            collectMissingBillingDetailsForExistingPaymentMethods,
            allowUserEmailEdits = true,
        )
    }

    /**
     * Display configuration for Link
     */
    enum class Display {
        /**
         * Link will be displayed when available.
         */
        Automatic,

        /**
         * Link will never be displayed.
         */
        Never;

        internal val analyticsValue: String
            get() = when (this) {
                Automatic -> "automatic"
                Never -> "never"
            }
    }
}
