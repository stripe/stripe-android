package com.stripe.android.checkout

import android.os.Bundle
import com.stripe.android.checkout.CheckoutElementsController.OperationResult
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutOperationMeasurer
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
    private val operationMeasurer: CheckoutOperationMeasurer,
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
    ): OperationResult {
        val parsedClientSecret = try {
            Result.success(CheckoutSessionClientSecret(clientSecret))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
        return measure(
            operation = Operation.Init,
            params = buildMap {
                putAll(configuration.toAnalyticsMap())
                parsedClientSecret.getOrNull()?.let { put("checkout_session_id", it.sessionId) }
            },
            startResponse = null,
        ) {
            withContext(Dispatchers.Main.immediate) {
                // A re-configure while a payment flow is presented would reload the session out from under
                // the open sheet. The first configure runs with null state, so this only blocks re-configures.
                if (sheetStateHolder.sheetIsOpen) {
                    return@withContext Result.failure(IllegalStateException(PAYMENT_FLOW_PRESENTED_MESSAGE))
                }
                val secret = parsedClientSecret.getOrElse { return@withContext Result.failure(it) }
                operationCoordinator.runConfigure(configuration) {
                    runCatching {
                        loadConfiguration(secret, configuration)
                        committedCheckoutSessionResponse()
                    }
                }
            }
        }
    }

    private suspend fun loadConfiguration(
        clientSecret: CheckoutSessionClientSecret,
        configuration: CheckoutElementsController.Configuration.State,
    ) {
        val response = checkoutSessionRepository.init(
            clientSecret = clientSecret,
            adaptivePricingAllowed = configuration.currencySelectorElementConfiguration != null,
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

    suspend fun applyPromotionCode(promotionCode: String): OperationResult = measure(
        operation = Operation.ApplyPromotionCode,
        params = emptyMap(),
        startResponse = stateHolder.state?.checkoutSessionResponse,
    ) {
        withCheckoutState {
            checkoutSessionRepository.applyPromotionCode(
                checkoutSessionResponse = checkoutSessionResponse,
                promotionCode = promotionCode.trim(),
            ).getOrThrow()
        }
    }

    suspend fun removePromotionCode(): OperationResult = measure(
        operation = Operation.RemovePromotionCode,
        params = emptyMap(),
        startResponse = stateHolder.state?.checkoutSessionResponse,
    ) {
        withCheckoutState {
            checkoutSessionRepository.removePromotionCode(checkoutSessionResponse).getOrThrow()
        }
    }

    suspend fun updateShippingAddress(
        name: String?,
        address: CheckoutElementsController.Address.State?,
    ): OperationResult {
        val state = stateHolder.state
        val analyticsParams = shippingAddressAnalyticsParams(state, address)
        return measure(
            operation = Operation.UpdateShippingAddress,
            params = analyticsParams,
            startResponse = state?.checkoutSessionResponse,
        ) {
            // Reject a disallowed country before queueing so invalid input never shows loading.
            if (address != null) {
                state?.checkoutSessionResponse?.validateShippingCountry(address.country)
                    ?.exceptionOrNull()
                    ?.let { return@measure Result.failure(it) }
            }
            withCheckoutState(
                additionalStateMutations = {
                    copy(
                        collectedDetails = collectedDetails.copy(
                            shippingName = name.takeIf { address != null },
                            shippingAddress = address,
                        ),
                    )
                },
            ) {
                // A queued mutation may have changed the state since the call-time snapshot.
                analyticsParams.putAll(shippingAddressAnalyticsParams(this, address))
                // Clearing an already-cleared address leaves the server untouched.
                if (address == null && collectedDetails.shippingAddress == null) {
                    checkoutSessionResponse
                } else {
                    checkoutSessionTaxRegionUpdater.updateTaxRegionIfNeeded(
                        checkoutSessionResponse = checkoutSessionResponse,
                        addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                        address = address,
                    ).getOrThrow()
                }
            }
        }
    }

    private fun shippingAddressAnalyticsParams(
        state: CheckoutElementsControllerState?,
        address: CheckoutElementsController.Address.State?,
    ): MutableMap<String, Any?> = checkoutSessionTaxRegionUpdater.analyticsParams(
        invocationSource = InvocationSource.Merchant,
        didRequestTaxUpdate = state != null &&
            (address != null || state.collectedDetails.shippingAddress != null) &&
            checkoutSessionTaxRegionUpdater.requiresUpdate(
                checkoutSessionResponse = state.checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            ),
    ).toMutableMap()

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
    }.toOperationResult()

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
    }.toOperationResult()

    suspend fun selectSavedPaymentMethod(
        selection: PaymentSelection.Saved,
    ): OperationResult {
        val state = stateHolder.state
        val address = selection.billingDetails?.address?.toCheckoutAddress()
        val analyticsParams = billingAddressAnalyticsParams(state, address)
        return measure(
            operation = Operation.UpdateBillingAddress,
            params = analyticsParams,
            startResponse = state?.checkoutSessionResponse,
        ) {
            withCheckoutState(
                additionalStateMutations = { commitSelection(selection) },
            ) {
                // A queued mutation may have changed the state since the call-time snapshot.
                analyticsParams.putAll(billingAddressAnalyticsParams(this, address))
                stateHolder.state = copy(
                    savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending(
                        selection.paymentMethod.id,
                    ),
                )
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
                checkoutSessionTaxRegionUpdater.updateTaxRegionIfNeeded(
                    checkoutSessionResponse = checkoutSessionResponse,
                    addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                    address = address,
                ).onFailure {
                    stateHolder.state = stateHolder.state?.copy(
                        savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Failed(
                            error = it.stripeErrorMessage(),
                        ),
                    )
                }.getOrThrow()
            }
        }
    }

    private fun billingAddressAnalyticsParams(
        state: CheckoutElementsControllerState?,
        address: CheckoutElementsController.Address.State?,
    ): MutableMap<String, Any?> = checkoutSessionTaxRegionUpdater.analyticsParams(
        invocationSource = InvocationSource.PaymentElement,
        didRequestTaxUpdate = state != null &&
            address != null &&
            checkoutSessionTaxRegionUpdater.requiresUpdate(
                checkoutSessionResponse = state.checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
    ).toMutableMap()

    suspend fun updateCurrency(currency: String): OperationResult = measure(
        operation = Operation.UpdateCurrency,
        params = mapOf("requested_currency" to currency),
        startResponse = stateHolder.state?.checkoutSessionResponse,
    ) {
        withCheckoutState {
            checkoutSessionRepository.updateCurrency(
                checkoutSessionResponse = checkoutSessionResponse,
                currencyCode = currency,
            ).getOrThrow()
        }
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
    }.toOperationResult()

    suspend fun runServerUpdate(
        serverUpdate: suspend () -> Result<Unit>,
    ): OperationResult {
        // Preserve the merchant's context while keeping the callback a child of the mutation's
        // timeout and cancellation scope.
        val callerContext = currentCoroutineContext().minusKey(Job)
        val analyticsParams = mutableMapOf<String, Any?>()
        return measure(
            operation = Operation.RunServerUpdate,
            params = analyticsParams,
            startResponse = stateHolder.state?.checkoutSessionResponse,
        ) {
            withCheckoutState {
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
        }
    }

    /**
     * Reports [operation] around [block] so the analytics outcome and duration match what the caller
     * of the public API observes, including queueing behind other operations and committing state.
     */
    private suspend fun measure(
        operation: Operation,
        params: Map<String, Any?>,
        startResponse: CheckoutSessionResponse?,
        block: suspend () -> Result<CheckoutSessionResponse>,
    ): OperationResult = operationMeasurer.measure(
        operation = operation,
        checkoutSessionResponse = startResponse,
        params = params,
        block = block,
    ).toOperationResult()

    /**
     * Runs a mutation against the checkout session, serializing it behind the operation coordinator
     * so mutations run in sequence. [block] produces the updated [CheckoutSessionResponse] (throwing on
     * failure); the result is folded into a new [CheckoutElementsControllerState] (with any
     * [additionalStateMutations] applied) and handed to [checkoutStateLoader] to reload the payment
     * element and atomically commit the new state.
     *
     * Returns the committed [CheckoutSessionResponse], or a failure if the session hasn't been
     * configured yet or a payment flow is currently presented.
     */
    private suspend fun withCheckoutState(
        additionalStateMutations: CheckoutElementsControllerState.() -> CheckoutElementsControllerState = { this },
        block: suspend CheckoutElementsControllerState.() -> CheckoutSessionResponse,
    ): Result<CheckoutSessionResponse> = withContext(Dispatchers.Main.immediate) {
        stateHolder.state
            ?: return@withContext Result.failure(
                IllegalStateException("Cannot mutate checkout session before it is configured.")
            )
        if (sheetStateHolder.sheetIsOpen) {
            return@withContext Result.failure(IllegalStateException(PAYMENT_FLOW_PRESENTED_MESSAGE))
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
                committedCheckoutSessionResponse()
            }
        }
    }

    private fun committedCheckoutSessionResponse(): CheckoutSessionResponse =
        requireNotNull(stateHolder.state).checkoutSessionResponse

    private fun Result<*>.toOperationResult(): OperationResult = fold(
        onSuccess = { OperationResult.Succeeded() },
        onFailure = ::failed,
    )

    private fun failed(error: Throwable): OperationResult = OperationResult.Failed(
        error = error,
        displayMessage = displayMessageResolver.resolve(error),
    )
}
