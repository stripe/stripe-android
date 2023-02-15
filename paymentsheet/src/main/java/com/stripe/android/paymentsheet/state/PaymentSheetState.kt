package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentSheetState : Parcelable {

    @Parcelize
    object Loading : PaymentSheetState

    @Parcelize
    data class Full constructor(
        val config: PaymentSheet.Configuration?,
//        val data: PaymentSheetData,
        val stripeIntent: StripeIntent,
        val customerPaymentMethods: List<PaymentMethod>,
        val savedSelection: SavedSelection,
        val isGooglePayReady: Boolean,
        val linkState: LinkState?,
        val newPaymentSelection: PaymentSelection.New?,
    ) : PaymentSheetState {

        val hasPaymentOptions: Boolean
            get() = isGooglePayReady || linkState != null || customerPaymentMethods.isNotEmpty()

        val initialPaymentSelection: PaymentSelection?
            get() = when (savedSelection) {
                is SavedSelection.GooglePay -> PaymentSelection.GooglePay
                is SavedSelection.Link -> PaymentSelection.Link
                is SavedSelection.PaymentMethod -> {
                    val paymentMethod = customerPaymentMethods.firstOrNull {
                        it.id == savedSelection.id
                    }

                    paymentMethod?.let {
                        PaymentSelection.Saved(it)
                    }
                }
                else -> null
            }
    }
}
