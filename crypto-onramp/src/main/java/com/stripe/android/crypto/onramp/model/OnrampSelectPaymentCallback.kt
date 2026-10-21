package com.stripe.android.crypto.onramp.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko

@ExperimentalCryptoOnramp
fun interface OnrampCollectPaymentMethodCallback {
    fun onResult(result: OnrampCollectPaymentMethodResult)
}

/**
 * Result of selecting a payment type in Onramp.
 */
@ExperimentalCryptoOnramp
sealed class OnrampCollectPaymentMethodResult {
    /**
     * The user has selected a payment option. Details are a snapshot of this selection, not a
     * token-bound attestation. Use the latest successful collection in the same checkout attempt
     * and discard cached policy state on logout or when the selection is cleared.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val displayData: PaymentMethodDisplayData,
        val kycInfo: KycInfo?,
        /** Selected card metadata, when available. Mutually exclusive with [bankAccountDetails]. */
        val cardDetails: OnrampCardDetails?,
        /** Selected bank account metadata, when available. Mutually exclusive with [cardDetails]. */
        val bankAccountDetails: OnrampBankAccountDetails?
    ) : OnrampCollectPaymentMethodResult() {
        internal constructor(
            displayData: PaymentMethodDisplayData,
            kycInfo: KycInfo?
        ) : this(displayData, kycInfo, cardDetails = null, bankAccountDetails = null)
    }

    /**
     * The user declined to select a payment option.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampCollectPaymentMethodResult()

    /**
     * Selecting a payment option failed due to an error.
     * @param error The error that caused the failure.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(val error: Throwable) : OnrampCollectPaymentMethodResult()
}

@Poko
@ExperimentalCryptoOnramp
class PaymentMethodDisplayData internal constructor(
    val imageLoader: suspend () -> Drawable,
    /**
     * User facing strings representing payment method information
     */
    val label: String,
    val sublabel: String?,
    /**
     * The type of payment being displayed.
     */
    val type: Type
) {
    @ExperimentalCryptoOnramp
    enum class Type {
        Card,
        BankAccount,
        GooglePay,
        SamsungPay,
    }

    val icon: Drawable by lazy {
        DelegateDrawable(imageLoader = imageLoader)
    }

    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(icon)
}
