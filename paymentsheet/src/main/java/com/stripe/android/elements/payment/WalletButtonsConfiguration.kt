package com.stripe.android.elements.payment

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration for wallet buttons
 */
@Poko
@Parcelize
class WalletButtonsConfiguration(
    /**
     * Indicates the `WalletButtons` API will be used. This helps
     * ensure the Payment Element is not initialized with a displayed
     * wallet option as the default payment option.
     */
    val willDisplayExternally: Boolean = false,

    /**
     * Identifies the list of wallets that can be shown in `WalletButtons`. Wallets
     * are identified by their wallet identifier (google_pay, link, shop_pay). An
     * empty list means all wallets will be shown.
     */
    val walletsToShow: List<String> = emptyList(),
) : Parcelable
