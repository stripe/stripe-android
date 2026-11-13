package com.stripe.android.checkout

import android.os.Bundle
import com.stripe.android.checkout.CheckoutElementsController.OperationResult
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.LocalOperation
import com.stripe.android.checkout.analytics.measureDuration
import com.stripe.android.checkout.analytics.toAnalyticsMap
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds
private const val PAYMENT_FLOW_PRESENTED_MESSAGE =
    "Cannot mutate checkout session while a payment flow is presented."

@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutInteractor @Inject constructor(
    private val displayMessageResolver: CheckoutDisplayMessageResolver,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val localOperationFactory: LocalOperation.Factory,
    private val checkoutSessionTaxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    private val errorReporter: ErrorReporter,
    private val checkoutStateLoader: CheckoutStateLoader,
    private val stateHolder: CheckoutElementsControllerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val operationCoordinator: CheckoutOperationCoordinator,
) {
    suspend fun configure(
        clientSecret: String,
        configuration: CheckoutElementsController.Configuration.State,
    ): OperationResult = withContext(Dispatchers.Main.immediate) {
        // A re-configure while a payment flow is presented would reload the session out from under
        // the open sheet. The first configure runs with null state, so this only blocks re-configures.
        if (sheetStateHolder.sheetIsOpen) {
            return@withContext failed(IllegalStateException(PAYMENT_FLOW_PRESENTED_MESSAGE))
        }
        val parsedClientSecret = try {
            CheckoutSessionClientSecret(clientSecret)
        } catch (e: IllegalArgumentException) {
            return@withContext failed(e)
        }
        operationCoordinator.runConfigure(configuration) {
            runCatching {
                val response = checkoutSessionRepository.init(
                    clientSecret = parsedClientSecret,
                    adaptivePricingAllowed = configuration.currencySelectorElementConfiguration != null,
                    analyticsParams = configuration.toAnalyticsMap(),
                ).getOrThrow()
                val initialLoad = checkoutStateLoader.loadInitial(
                    configuration = configuration,
                    checkoutSessionResponse = response,
                )
                val addressSource = response.taxAddressSource
                val taxAddress = when (addressSource) {
                    CheckoutSessionResponse.TaxAddressSource.SHIPPING ->
                        initialLoad.state.collectedDetails.shippingAddress
                    CheckoutSessionResponse.TaxAddressSource.BILLING ->
                        initialLoad.state.paymentSelection?.billingDetails?.address?.toCheckoutAddress()
                            ?: configuration.defaults.billingDetails?.address
                    null -> null
                }
                if (taxAddress != null && addressSource != null && response.automaticTaxEnabled) {
                    val updatedResponse = checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
                        checkoutSessionResponse = response,
                        addressSource = addressSource,
                        invocationSource = InvocationSource.Merchant,
                        address = taxAddress,
                    ).getOrThrow()
                    // The updated totals change payment method metadata, so rebuild it before publishing.
                    checkoutStateLoader.reload(initialLoad.state.copy(checkoutSessionResponse = updatedResponse))
                } else {
                    checkoutStateLoader.publish(initialLoad)
                }
            }
        }.toOperationResult()
    }

    suspend fun applyPromotionCode(promotionCode: String): OperationResult = withCheckoutState {
        checkoutSessionRepository.applyPromotionCode(
            checkoutSessionResponse = checkoutSessionResponse,
            promotionCode = promotionCode.trim(),
        ).getOrThrow()
    }

    suspend fun removePromotionCode(): OperationResult = withCheckoutState {
        checkoutSessionRepository.removePromotionCode(checkoutSessionResponse).getOrThrow()
    }

    suspend fun updateShippingAddress(
        name: String?,
        address: CheckoutElementsController.Address.State?,
    ): OperationResult {
        if (address == null) {
            return clearShippingAddress()
        }
        stateHolder.state?.checkoutSessionResponse
            ?.validateShippingCountry(address.country)
            ?.exceptionOrNull()
            ?.let { return failed(it) }
        return withCheckoutState(
            additionalStateMutations = {
                copy(
                    collectedDetails = collectedDetails.copy(
                        shippingName = name,
                        shippingAddress = address,
                    ),
                )
            },
        ) {
            checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                invocationSource = InvocationSource.Merchant,
                address = address,
            ).getOrThrow()
        }
    }

    suspend fun commitShippingAddress(
        name: String?,
        address: CheckoutElementsController.Address.State,
        updatedCheckoutSessionResponse: CheckoutSessionResponse,
    ): OperationResult = withCheckoutState(
        additionalStateMutations = {
            copy(
                collectedDetails = collectedDetails.copy(
                    shippingName = name,
                    shippingAddress = address,
                ),
            )
        },
    ) {
        updatedCheckoutSessionResponse
    }

    suspend fun updateEmail(email: String?): OperationResult = withCheckoutState(
        additionalStateMutations = {
            copy(collectedDetails = collectedDetails.copy(email = email))
        },
    ) {
        localOperationFactory.create(
            operation = Operation.UpdateEmail,
            params = emptyMap(),
        ).succeeded(checkoutSessionResponse)
        checkoutSessionResponse
    }

    suspend fun selectSavedPaymentMethod(
        selection: PaymentSelection.Saved,
    ): OperationResult = withCheckoutState(
        additionalStateMutations = { commitSelection(selection) },
    ) {
        stateHolder.state = copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending(
                selection.paymentMethod.id,
            ),
        )
        val address = selection.billingDetails?.address?.toCheckoutAddress()
        if (address == null) {
            if (
                checkoutSessionTaxRegionUpdater.requiresUpdate(
                    checkoutSessionResponse = checkoutSessionResponse,
                    addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                )
            ) {
                // Billing-tax filtering should prevent this state from reaching selection.
                errorReporter.report(
                    errorEvent = ErrorReporter.UnexpectedErrorEvent
                        .CHECKOUT_SAVED_PAYMENT_METHOD_MISSING_BILLING_ADDRESS,
                )
            }
            return@withCheckoutState checkoutSessionResponse
        }
        checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            invocationSource = InvocationSource.PaymentElement,
            address = address,
        ).onFailure {
            stateHolder.state = stateHolder.state?.copy(
                savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Failed(
                    error = it.stripeErrorMessage(),
                ),
            )
        }.getOrThrow()
    }

    suspend fun updateCurrency(currency: String): OperationResult = withCheckoutState {
        checkoutSessionRepository.updateCurrency(
            checkoutSessionResponse = checkoutSessionResponse,
            currencyCode = currency,
        ).getOrThrow()
    }

    suspend fun clearPaymentOption(): OperationResult = withCheckoutState(
        additionalStateMutations = {
            copy(
                paymentSelection = null,
                temporarySelection = null,
                previousNewSelections = Bundle(),
            )
        },
    ) {
        localOperationFactory.create(
            operation = Operation.ClearPaymentOption,
            params = emptyMap(),
        ).invoked(checkoutSessionResponse)
        checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            invocationSource = InvocationSource.Merchant,
            address = null,
        ).getOrThrow()
    }

    suspend fun runServerUpdate(
        serverUpdate: suspend () -> Result<Unit>,
    ): OperationResult {
        // Preserve the merchant's context while keeping the callback a child of the mutation's
        // timeout and cancellation scope.
        val callerContext = currentCoroutineContext().minusKey(Job)
        return withCheckoutState {
            val analyticsParams = mutableMapOf<String, Any?>()
            checkoutSessionRepository.measure(
                operation = Operation.RunServerUpdate,
                checkoutSessionResponse = checkoutSessionResponse,
                params = analyticsParams,
            ) {
                runCatching {
                    analyticsParams.measureDuration("merchant_update_duration") {
                        withContext(callerContext) {
                            withTimeout(SERVER_UPDATE_TIMEOUT_MS) { serverUpdate() }
                        }.getOrThrow()
                    }
                    analyticsParams.measureDuration("refresh_duration") {
                        checkoutSessionRepository.fetch(
                            clientSecret = checkoutSessionResponse.clientSecret,
                        ).getOrThrow()
                    }
                }
            }.getOrThrow()
        }
    }

    private suspend fun clearShippingAddress(): OperationResult = withCheckoutState(
        additionalStateMutations = {
            copy(
                collectedDetails = collectedDetails.copy(
                    shippingName = null,
                    shippingAddress = null,
                ),
            )
        },
    ) {
        if (collectedDetails.shippingAddress == null) {
            localOperationFactory.create(
                operation = Operation.UpdateShippingAddress,
                params = mapOf(
                    "invocation_source" to InvocationSource.Merchant.code,
                    "did_request_tax_update" to false,
                ),
            ).succeeded(checkoutSessionResponse)
            checkoutSessionResponse
        } else {
            checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                invocationSource = InvocationSource.Merchant,
                address = null,
            ).getOrThrow()
        }
    }

    /**
     * Runs a mutation against the checkout session, serializing it behind the operation coordinator
     * so mutations run in sequence. [block] produces the updated [CheckoutSessionResponse] (throwing on
     * failure); the result is folded into a new [CheckoutElementsControllerState] (with any
     * [additionalStateMutations] applied) and handed to [checkoutStateLoader] to reload the payment
     * element and atomically commit the new state.
     *
     * Returns [OperationResult.Failed] if the session hasn't been configured yet or a payment flow is
     * currently presented.
     */
    private suspend fun withCheckoutState(
        additionalStateMutations: CheckoutElementsControllerState.() -> CheckoutElementsControllerState = { this },
        block: suspend CheckoutElementsControllerState.() -> CheckoutSessionResponse,
    ): OperationResult = withContext(Dispatchers.Main.immediate) {
        stateHolder.state
            ?: return@withContext failed(
                IllegalStateException("Cannot mutate checkout session before it is configured.")
            )
        if (sheetStateHolder.sheetIsOpen) {
            return@withContext failed(IllegalStateException(PAYMENT_FLOW_PRESENTED_MESSAGE))
        }
        operationCoordinator.runMutation {
            runCatching {
                // Re-read the latest committed state inside the lock so serialized mutations
                // build on each other's results rather than a stale snapshot.
                val state = requireNotNull(stateHolder.state)
                val response = state.block()
                val newState = state
                    .copy(checkoutSessionResponse = response)
                    .additionalStateMutations()
                // reload resolves flag images (reusing newState's carried-over cache) and commits
                // the fully reloaded state to the holder.
                checkoutStateLoader.reload(newState)
            }
        }.toOperationResult()
    }

    private fun Result<Unit>.toOperationResult(): OperationResult = fold(
        onSuccess = { OperationResult.Succeeded() },
        onFailure = ::failed,
    )

    private fun failed(error: Throwable): OperationResult = OperationResult.Failed(
        error = error,
        displayMessage = displayMessageResolver.resolve(error),
    )
}
