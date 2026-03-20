package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionCurrencyUpdater(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val paymentElementLoader: PaymentElementLoader,
    private val workContext: CoroutineContext,
) {
    suspend fun updateCurrency(
        paymentMethodMetadata: PaymentMethodMetadata,
        currencyCode: String,
        integrationConfiguration: PaymentElementLoader.Configuration,
        initializedViaCompose: Boolean,
    ): Result<PaymentElementLoader.State> {
        val checkoutSession = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession
            ?: return Result.failure(IllegalStateException("Not a checkout session"))

        val updateResult = withContext(workContext) {
            checkoutSessionRepository.updateCurrency(checkoutSession.id, currencyCode)
        }

        return updateResult.fold(
            onSuccess = { response ->
                // Sync Checkout instances
                CheckoutInstances[checkoutSession.instancesKey].forEach {
                    it.updateWithResponse(response)
                }

                // Reload state with updated response
                val updatedInitMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                    instancesKey = checkoutSession.instancesKey,
                    checkoutSessionResponse = response,
                )
                withContext(workContext) {
                    paymentElementLoader.load(
                        initializationMode = updatedInitMode,
                        integrationConfiguration = integrationConfiguration,
                        metadata = PaymentElementLoader.Metadata(
                            isReloadingAfterProcessDeath = false,
                            initializedViaCompose = initializedViaCompose,
                        ),
                    )
                }
            },
            onFailure = { Result.failure(it) },
        )
    }
}
