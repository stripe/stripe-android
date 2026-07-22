package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns [CheckoutController]'s [CheckoutControllerState] — the single source of truth for the
 * controller — persisting it in [SavedStateHandle] so it survives process death. All observable
 * projections (e.g. [checkoutSession]) are derived from the one [stateFlow]. Kept separate from the
 * controller so [CheckoutStateLoader] can commit loaded state directly rather than reaching back
 * into the controller.
 *
 * Also serves as the checkout graph's [EmbeddedSelectionHolder] and [CustomerStateHolder]. The
 * selection projections derive from [stateFlow]; the customer state is set independently of a state
 * commit, so it's persisted under its own [SavedStateHandle] keys — mirroring
 * [com.stripe.android.paymentsheet.DefaultCustomerStateHolder].
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
    private val availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
) : EmbeddedSelectionHolder, CustomerStateHolder {
    var state: CheckoutControllerState?
        get() = savedStateHandle[STATE_KEY]
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val stateFlow: StateFlow<CheckoutControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    val checkoutSession: StateFlow<CheckoutSession?> =
        stateFlow.mapAsStateFlow {
            it?.asCheckoutSession(
                paymentOptionFactory,
                availableExpressButtonTypesFactory,
            )
        }

    override val selection: StateFlow<PaymentSelection?> =
        stateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        stateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: Bundle
        get() = state?.previousNewSelections ?: Bundle()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        val previousNewSelections = Bundle(current.previousNewSelections).apply {
            stashNewSelection(updatedSelection)
        }
        state = current.copy(
            paymentSelection = updatedSelection,
            previousNewSelections = previousNewSelections,
        )
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) {
        val current = requireState(operation = "setTemporarySelection") ?: return
        state = current.copy(temporarySelection = code)
    }

    override fun setPreviousNewSelections(bundle: Bundle) {
        val current = requireState(operation = "setPreviousNewSelections") ?: return
        val previousNewSelections = Bundle(current.previousNewSelections).apply {
            putAll(bundle)
        }
        state = current.copy(previousNewSelections = previousNewSelections)
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections.previousNewSelection(code)
    }

    fun clearSelection() {
        val current = requireState(operation = "clearSelection") ?: return
        state = current.copy(
            paymentSelection = null,
            temporarySelection = null,
            previousNewSelections = Bundle(),
        )
    }

    /**
     * The selection and customer state live on [CheckoutControllerState], so their mutators can only
     * act once [CheckoutStateLoader] has committed a state. A call before then is a programming error
     * (a mis-ordered write); report it and no-op rather than silently dropping the value.
     */
    private fun requireState(operation: String): CheckoutControllerState? {
        return state ?: run {
            errorReporter.report(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SELECTION_SET_BEFORE_LOAD,
                additionalNonPiiParams = mapOf("operation" to operation),
            )
            null
        }
    }

    // region CustomerStateHolder

    private val customerMetadata: StateFlow<CustomerMetadata?> =
        stateFlow.mapAsStateFlow { it?.paymentMethodMetadata?.customerMetadata }

    private val paymentMethodMetadataFlow: StateFlow<PaymentMethodMetadata?> =
        stateFlow.mapAsStateFlow { it?.paymentMethodMetadata }

    override val customer: StateFlow<CustomerState?> =
        stateFlow.mapAsStateFlow { it?.customerState }

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    override val paymentMethods: StateFlow<List<PaymentMethod>> = customer
        .mapAsStateFlow { state ->
            state?.paymentMethods ?: emptyList()
        }

    override val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?> =
        stateFlow.mapAsStateFlow { it?.mostRecentlySelectedSavedPaymentMethod }

    override val canRemove: StateFlow<Boolean> = combineAsStateFlow(
        paymentMethods,
        customerMetadata,
    ) { paymentMethods, customerMetadata ->
        customerMetadata?.run {
            val hasRemovePermissions = customerMetadata.canRemovePaymentMethods
            val hasRemoveLastPaymentMethodPermissions = customerMetadata.canRemoveLastPaymentMethod
            when (paymentMethods.size) {
                0 -> false
                1 -> hasRemoveLastPaymentMethodPermissions && hasRemovePermissions
                else -> hasRemovePermissions
            }
        } ?: false
    }

    override val canUpdateCardExpiryAndBillingDetails: StateFlow<Boolean> = customerMetadata.mapAsStateFlow {
        it?.canUpdateCardExpiryAndBillingDetails ?: false
    }

    override val canChangeCbc: StateFlow<Boolean> = combineAsStateFlow(
        customerMetadata,
        paymentMethodMetadataFlow,
    ) { metadata, pmMetadata ->
        val canUpdateBrandChoice = metadata?.canUpdateCardBrandChoice ?: false
        val isCbcEligible = pmMetadata?.cbcEligibility is CardBrandChoiceEligibility.Eligible
        canUpdateBrandChoice && isCbcEligible
    }

    override fun setCustomerState(customerState: CustomerState?) {
        val current = requireState(operation = "setCustomerState") ?: return
        val currentSelection = current.mostRecentlySelectedSavedPaymentMethod
        val newSelection = customerState?.paymentMethods?.firstOrNull { it.id == currentSelection?.id }
        state = current.copy(
            customerState = customerState,
            mostRecentlySelectedSavedPaymentMethod = newSelection,
        )
    }

    override fun setDefaultPaymentMethod(paymentMethod: PaymentMethod?) {
        val current = requireState(operation = "setDefaultPaymentMethod") ?: return
        state = current.copy(
            customerState = current.customerState?.copy(defaultPaymentMethodId = paymentMethod?.id),
        )
    }

    override fun updateMostRecentlySelectedSavedPaymentMethod(paymentMethod: PaymentMethod?) {
        val current = requireState(operation = "updateMostRecentlySelectedSavedPaymentMethod") ?: return
        state = current.copy(mostRecentlySelectedSavedPaymentMethod = paymentMethod)
    }

    override fun addPaymentMethod(paymentMethod: PaymentMethod) {
        val current = requireState(operation = "addPaymentMethod") ?: return
        val currentCustomer = current.customerState ?: return
        val currentMetadata = current.paymentMethodMetadata.customerMetadata ?: return

        val newCustomer = when (currentMetadata) {
            is CustomerMetadata.LegacyEphemeralKey -> {
                currentCustomer.copy(paymentMethods = currentCustomer.paymentMethods + paymentMethod)
            }
            is CustomerMetadata.CustomerSession,
            is CustomerMetadata.CheckoutSession -> {
                val currentCustomerPaymentMethods = currentCustomer.paymentMethods

                val samePaymentMethodIndex = currentCustomerPaymentMethods.indexOfFirst { customerPaymentMethod ->
                    customerPaymentMethod.fingerprint() == paymentMethod.fingerprint()
                }

                if (samePaymentMethodIndex == -1) {
                    currentCustomer.copy(paymentMethods = currentCustomerPaymentMethods + paymentMethod)
                } else {
                    val mutablePaymentMethods = currentCustomerPaymentMethods.toMutableList()
                    mutablePaymentMethods[samePaymentMethodIndex] = paymentMethod
                    currentCustomer.copy(paymentMethods = mutablePaymentMethods)
                }
            }
        }

        state = current.copy(customerState = newCustomer)
    }

    private fun PaymentMethod.fingerprint(): String? {
        return card?.fingerprint
            ?: usBankAccount?.fingerprint
            ?: sepaDebit?.fingerprint
    }

    // endregion

    companion object {
        const val STATE_KEY = "CheckoutController_InternalState"
    }
}
