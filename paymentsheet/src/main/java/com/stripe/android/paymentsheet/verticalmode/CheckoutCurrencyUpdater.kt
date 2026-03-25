package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface CheckoutCurrencyUpdater {
    suspend fun updateCurrency(
        instancesKey: String,
        sessionId: String,
        currencyCode: String,
        config: PaymentSheet.Configuration,
        initializedViaCompose: Boolean,
    ): Result<CurrencyUpdateResult>

    data class CurrencyUpdateResult(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val loaderState: PaymentElementLoader.State,
    )
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutCurrencyUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val paymentElementLoader: PaymentElementLoader,
) : CheckoutCurrencyUpdater {

    override suspend fun updateCurrency(
        instancesKey: String,
        sessionId: String,
        currencyCode: String,
        config: PaymentSheet.Configuration,
        initializedViaCompose: Boolean,
    ): Result<CheckoutCurrencyUpdater.CurrencyUpdateResult> {
        val responseResult = checkoutSessionRepository.updateCurrency(
            sessionId = sessionId,
            currencyCode = currencyCode,
        )

        val response = responseResult.getOrElse { return Result.failure(it) }

        CheckoutInstances[instancesKey].forEach { it.updateWithResponse(response) }

        val newInitMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = instancesKey,
            checkoutSessionResponse = response,
        )

        val loaderResult = paymentElementLoader.load(
            initializationMode = newInitMode,
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(config),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = initializedViaCompose,
            ),
        )

        val loaderState = loaderResult.getOrElse { return Result.failure(it) }

        return Result.success(
            CheckoutCurrencyUpdater.CurrencyUpdateResult(
                checkoutSessionResponse = response,
                loaderState = loaderState,
            )
        )
    }
}
