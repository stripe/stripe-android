package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

/**
 * Builder for Google Pay configuration used by checkout.
 *
 * @param environment The Google Pay environment to use. See
 * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
 * for more information.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayConfiguration(
    private val environment: Environment,
) {
    private var label: String? = null
    private var buttonType: ButtonType = ButtonType.Pay
    private var additionalEnabledNetworks: List<String> = emptyList()

    /**
     * @param label An optional label to display with the amount. Google Pay may or may not display
     * this label depending on its own internal logic. Defaults to a generic label if none is
     * provided.
     */
    fun label(label: String): GooglePayConfiguration = apply {
        this.label = label
    }

    /**
     * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
     * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
     * for more information on button types.
     */
    fun buttonType(buttonType: ButtonType): GooglePayConfiguration = apply {
        this.buttonType = buttonType
    }

    /**
     * @param additionalEnabledNetworks An optional List<String> to signal GooglePay to
     * display additional enabled networks (e.g. 'INTERAC')
     */
    fun additionalEnabledNetworks(
        additionalEnabledNetworks: List<String>
    ): GooglePayConfiguration = apply {
        this.additionalEnabledNetworks = additionalEnabledNetworks
    }

    @Parcelize
    internal data class State(
        val environment: Environment,
        val label: String?,
        val buttonType: ButtonType,
        val additionalEnabledNetworks: List<String>,
    ) : Parcelable

    internal fun build(): State = State(
        environment = environment,
        label = label,
        buttonType = buttonType,
        additionalEnabledNetworks = additionalEnabledNetworks.toList(),
    )

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Environment {
        Production,
        Test,
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    /**
     * Google Pay button type options
     *
     * See
     * [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions)
     * for more information on button types.
     */
    enum class ButtonType {
        /**
         * Displays "Buy with" alongside the Google Pay logo.
         */
        Buy,

        /**
         * Displays "Book with" alongside the Google Pay logo.
         */
        Book,

        /**
         * Displays "Checkout with" alongside the Google Pay logo.
         */
        Checkout,

        /**
         * Displays "Donate with" alongside the Google Pay logo.
         */
        Donate,

        /**
         * Displays "Order with" alongside the Google Pay logo.
         */
        Order,

        /**
         * Displays "Pay with" alongside the Google Pay logo.
         */
        Pay,

        /**
         * Displays "Subscribe with" alongside the Google Pay logo.
         */
        Subscribe,

        /**
         * Displays only the Google Pay logo.
         */
        Plain
    }
}
