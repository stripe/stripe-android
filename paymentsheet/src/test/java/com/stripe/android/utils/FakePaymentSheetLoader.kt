package com.stripe.android.utils

import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakePaymentSheetLoader(
    private val stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
    private val shouldFail: Boolean = false,
    private var customerPaymentMethods: List<PaymentMethod> = emptyList(),
    private val savedSelection: SavedSelection = SavedSelection.None,
    private val isGooglePayAvailable: Boolean = false,
    private val delay: Duration = Duration.ZERO,
) : PaymentSheetLoader {

    fun updatePaymentMethods(paymentMethods: List<PaymentMethod>) {
        this.customerPaymentMethods = paymentMethods
    }

    override suspend fun load(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): PaymentSheetLoader.Result {
        delay(delay)
        return if (shouldFail) {
            PaymentSheetLoader.Result.Failure(IllegalStateException("oh no"))
        } else {
            PaymentSheetLoader.Result.Success(
                state = PaymentSheetState.Full(
                    config = paymentSheetConfiguration,
                    clientSecret = clientSecret,
                    stripeIntent = stripeIntent,
                    customerPaymentMethods = customerPaymentMethods,
                    savedSelection = savedSelection,
                    isGooglePayReady = isGooglePayAvailable,
                )
            )
        }
    }
}
