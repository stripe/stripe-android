package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.PaymentSheet

/**
 * An interface for a class that can initialize a [PaymentSheet.FlowController].
 */
internal interface FlowControllerInitializer {
    suspend fun init(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration
    ): InitResult

    suspend fun init(
        paymentIntentClientSecret: String
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
