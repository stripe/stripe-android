package com.stripe.android.paymentsheet.state

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.injection.GooglePayRepositoryFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal fun interface GetGooglePayState {
    suspend operator fun invoke(
        configuration: CommonConfiguration,
        initializationMode: PaymentElementLoader.InitializationMode,
        elementsSession: Deferred<ElementsSession>,
    ): GooglePayState
}

internal data class GooglePayState(
    val isGooglePayReady: Boolean,
    val isGooglePaySupported: Boolean,
)

internal class DefaultGetGooglePayState @Inject constructor(
    private val googlePayRepositoryFactory: GooglePayRepositoryFactory,
    private val userFacingLogger: UserFacingLogger,
    private val errorReporter: ErrorReporter,
    private val durationProvider: DurationProvider,
) : GetGooglePayState {

    override suspend fun invoke(
        configuration: CommonConfiguration,
        initializationMode: PaymentElementLoader.InitializationMode,
        elementsSession: Deferred<ElementsSession>,
    ): GooglePayState = coroutineScope {
        val isGooglePaySupportedOnDevice = async(start = CoroutineStart.UNDISPATCHED) {
            durationProvider.measureDuration(
                DurationProvider.Key.PaymentSheetLoadIsGooglePaySupported
            ) {
                isGooglePayReadyForEnvironment(GooglePayEnvironment.Production)
            }
        }
        val isGooglePaySupportedByConfiguration = async {
            durationProvider.measureDuration(
                DurationProvider.Key.PaymentSheetLoadIsGooglePayReady
            ) {
                configuration.isGooglePayReady()
            }
        }

        val isGooglePayReady = isGooglePayReady(
            configuration = configuration,
            elementsSession = elementsSession.await(),
            initializationMode = initializationMode,
            isGooglePaySupportedByConfiguration = isGooglePaySupportedByConfiguration,
        )
        val isGooglePaySupported = isGooglePaySupportedOnDevice.completeResultOrNull {
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_SKIPPED_DURING_LOAD)
        } ?: false
        isGooglePaySupportedByConfiguration.cancel()

        GooglePayState(
            isGooglePayReady = isGooglePayReady,
            isGooglePaySupported = isGooglePaySupported,
        )
    }

    private suspend fun isGooglePayReady(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    ): Boolean {
        val shouldDisableForAutomaticTaxBilling =
            (initializationMode as? PaymentElementLoader.InitializationMode.CheckoutSession)
            ?.checkoutSessionResponse
            ?.let { checkoutSessionResponse ->
                checkoutSessionResponse.automaticTaxEnabled &&
                    checkoutSessionResponse.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING &&
                    configuration.defaultBillingDetails == null
            } == true

        if (!elementsSession.isGooglePayEnabled) {
            userFacingLogger.logWarningWithoutPii(
                "Google Pay is not enabled for this session."
            )
        } else if (configuration.googlePay == null) {
            userFacingLogger.logWarningWithoutPii(
                "GooglePayConfiguration is not set."
            )
        } else if (shouldDisableForAutomaticTaxBilling) {
            userFacingLogger.logWarningWithoutPii(
                "Google Pay is disabled because automatic tax is configured to use the billing address and no " +
                    "default billing address was provided."
            )
            return false
        } else if (!isGooglePaySupportedByConfiguration.await()) {
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
        }
        return elementsSession.isGooglePayEnabled && isGooglePaySupportedByConfiguration.await()
    }

    // Default filters are used here because this only determines the ready state,
    // not what's presented to Google Pay. This check runs async before we fetch the
    // elements session, so using merchant-defined filters would add latency.
    private suspend fun isGooglePayReadyForEnvironment(environment: GooglePayEnvironment): Boolean {
        return googlePayRepositoryFactory(
            environment = environment,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter
        ).isReady().first()
    }

    private suspend fun CommonConfiguration.isGooglePayReady(): Boolean {
        return googlePay?.environment?.let { environment ->
            isGooglePayReadyForEnvironment(
                when (environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production ->
                        GooglePayEnvironment.Production
                    PaymentSheet.GooglePayConfiguration.Environment.Test ->
                        GooglePayEnvironment.Test
                }
            )
        } ?: false
    }
}

private suspend fun <T> Deferred<T>.completeResultOrNull(
    skippedCallback: () -> Unit,
): T? = if (isCompleted) {
    await()
} else {
    skippedCallback()
    cancel()
    null
}
