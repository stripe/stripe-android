package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.Deferred
import javax.inject.Inject

internal fun interface GetGooglePayState {
    suspend operator fun invoke(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        isGooglePaySupportedOnDevice: Deferred<Boolean>,
        isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    ): GooglePayState
}

internal data class GooglePayState(
    val isGooglePayReady: Boolean,
    val isGooglePaySupported: Boolean,
)

internal class DefaultGetGooglePayState @Inject constructor(
    private val userFacingLogger: UserFacingLogger,
    private val errorReporter: ErrorReporter,
) : GetGooglePayState {

    override suspend fun invoke(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        isGooglePaySupportedOnDevice: Deferred<Boolean>,
        isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    ): GooglePayState {
        val shouldDisableForAutomaticTaxBilling =
            (initializationMode as? PaymentElementLoader.InitializationMode.CheckoutSession)
            ?.checkoutSessionResponse
            ?.let { checkoutSessionResponse ->
                checkoutSessionResponse.automaticTaxEnabled &&
                    checkoutSessionResponse.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING &&
                    configuration.defaultBillingDetails == null
            } == true

        val isGooglePayReady = when {
            !elementsSession.isGooglePayEnabled -> {
                userFacingLogger.logWarningWithoutPii("Google Pay is not enabled for this session.")
                false
            }
            configuration.googlePay == null -> {
                userFacingLogger.logWarningWithoutPii("GooglePayConfiguration is not set.")
                false
            }
            shouldDisableForAutomaticTaxBilling -> {
                userFacingLogger.logWarningWithoutPii(
                    "Google Pay is disabled because automatic tax is configured to use the billing address and no " +
                        "default billing address was provided."
                )
                false
            }
            !isGooglePaySupportedByConfiguration.await() -> {
                @Suppress("MaxLineLength")
                userFacingLogger.logWarningWithoutPii(
                    """
                        Google Pay API check failed.
                        Possible reasons:
                        - Google Play service is not available on this device.
                        - Google account is not signed in on this device.
                        See https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#public-taskboolean-isreadytopay-isreadytopayrequest-request for more details.
                    """.trimIndent()
                )
                false
            }
            else -> true
        }

        val isGooglePaySupported = isGooglePaySupportedOnDevice.completeResultOrNull {
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_SKIPPED_DURING_LOAD)
        } ?: false

        return GooglePayState(
            isGooglePayReady = isGooglePayReady,
            isGooglePaySupported = isGooglePaySupported,
        )
    }
}

private suspend fun <T> Deferred<T>.completeResultOrNull(
    skippedCallback: () -> Unit,
): T? = if (isCompleted) {
    await()
} else {
    skippedCallback()
    null
}
