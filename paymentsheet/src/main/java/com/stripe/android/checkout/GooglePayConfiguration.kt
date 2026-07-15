package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * @param environment The Google Pay environment to use. See
 * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
 * for more information.
 * @param countryCode The two-letter ISO 3166 code of the country of your business, e.g. "US".
 * See your account's country value [here](https://dashboard.stripe.com/settings/account).
 * @param currencyCode The three-letter ISO 4217 alphabetic currency code, e.g. "USD" or "EUR".
 * Required in order to support Google Pay when processing a Setup Intent.
 * @param amount An optional amount to display for setup intents. Google Pay may or may not
 * display this amount depending on its own internal logic. Defaults to 0 if none is provided.
 * @param label An optional label to display with the amount. Google Pay may or may not display
 * this label depending on its own internal logic. Defaults to a generic label if none is
 * provided.
 * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
 * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
 * for more information on button types.
 * @param additionalEnabledNetworks An optional List<String> to signal GooglePay to
 * display additional enabled networks (e.g. 'INTERAC')
 */
@Parcelize
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayConfiguration @JvmOverloads constructor(
    internal val environment: Environment,
    internal val countryCode: String,
    internal val currencyCode: String? = null,
    internal val amount: Long? = null,
    internal val label: String? = null,
    internal val buttonType: ButtonType = ButtonType.Pay,
    internal val additionalEnabledNetworks: List<String> = emptyList()
) : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Environment {
        Production,
        Test,
    }

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
