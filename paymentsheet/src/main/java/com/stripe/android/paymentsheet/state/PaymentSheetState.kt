package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentSheetState : Parcelable {

    @Parcelize
    object Loading : PaymentSheetState

    @Parcelize
    data class Full constructor(
        val config: PaymentSheet.Configuration?,
        val clientSecret: ClientSecret,
        val stripeIntent: StripeIntent,
        val supportedPaymentMethodTypes: List<PaymentMethodCode>,
        val customerPaymentMethods: List<PaymentMethod>,
        val selection: PaymentSelection?,
        val newPaymentSelection: PaymentSelection.New?,
        val savedSelection: SavedSelection,
        val isGooglePayReady: Boolean,
        val linkState: LinkState?,
        val amount: Amount?,
        val isEditing: Boolean,
        val isProcessing: Boolean,
        val notesText: String?,
        val primaryButtonUiState: PrimaryButton.UIState?,
    ) : PaymentSheetState {

        val areWalletButtonsEnabled: Boolean
            get() = !isProcessing && !isEditing

        val isPrimaryButtonEnabled: Boolean
            get() = if (primaryButtonUiState != null) {
                primaryButtonUiState.enabled && areWalletButtonsEnabled
            } else {
                areWalletButtonsEnabled && selection != null
            }

        val isLinkAvailable: Boolean
            get() = linkState != null

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

internal fun <T> StateFlow<PaymentSheetState>.mapAsLiveData(
    transform: (PaymentSheetState.Full) -> T,
): LiveData<T> {
    return filterIsInstance<PaymentSheetState.Full>().map(transform).asLiveData()
}
