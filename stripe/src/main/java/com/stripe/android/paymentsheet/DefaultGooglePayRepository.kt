package com.stripe.android.paymentsheet

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * The default implementation of [GooglePayRepository].
 */
internal class DefaultGooglePayRepository(
    private val context: Context,
    private val environment: PaymentSheet.GooglePayConfiguration.Environment,
    private val logger: Logger = Logger.noop()
) : GooglePayRepository {
    private val googlePayJsonFactory = GooglePayJsonFactory(context)

    private val paymentsClient: PaymentsClient by lazy {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(
                when (environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production ->
                        WalletConstants.ENVIRONMENT_PRODUCTION
                    PaymentSheet.GooglePayConfiguration.Environment.Test ->
                        WalletConstants.ENVIRONMENT_TEST
                }
            )
            .build()

        Wallet.getPaymentsClient(context, options)
    }

    /**
     * @return a [Flow] that represents the result of a [PaymentsClient.isReadyToPay] operation.
     *
     * See [Google Pay API docs](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#isReadyToPay(com.google.android.gms.wallet.IsReadyToPayRequest))
     * for more details.
     */
    override fun isReady(): Flow<Boolean> {
        val isReadyState = MutableStateFlow<Boolean?>(null)

        val request = IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest(
                existingPaymentMethodRequired = true
            ).toString()
        )

        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                val isReady = runCatching {
                    task.getResult(ApiException::class.java) == true
                }.getOrDefault(false)
                logger.info("Google Pay ready? $isReady")
                isReadyState.value = isReady
            }

        return isReadyState.filterNotNull()
    }
}
