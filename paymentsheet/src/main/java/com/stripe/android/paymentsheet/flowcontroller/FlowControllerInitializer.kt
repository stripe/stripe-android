package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.ClientSecret

/**
 * An interface for a class that can initialize a [PaymentSheet.FlowController].
 */
internal interface FlowControllerInitializer {
    suspend fun init(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration? = null
    ): InitResult

    sealed class InitResult {
        data class Success(
            val initData: InitData
        ) : InitResult()

        class Failure(
            val throwable: Throwable
        ) : InitResult()
    }
}
