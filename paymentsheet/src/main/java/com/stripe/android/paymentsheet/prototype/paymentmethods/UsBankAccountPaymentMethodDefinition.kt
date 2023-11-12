package com.stripe.android.paymentsheet.prototype.paymentmethods

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.ContactInformationCollectionMode
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.PrimaryButtonCustomizer
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.ui.core.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal object UsBankAccountPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.USBankAccount

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.ValidUsBankVerificationMethod,
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_us_bank_account)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank
            }

            requireContactInformation(ContactInformationCollectionMode.Name)
            requireContactInformation(ContactInformationCollectionMode.Email)

            // This seems odd, but we want this below the billing address collection.
            footer(UsBankAccountUiElementDefinition())
        }

        state(UsBankAccountState())

        primaryButtonCustomizer(UsBankPrimaryButtonCustomizer)
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        // TODO: Attach payment_method_data[us_bank_account][link_account_session]
        // TODO: Consider SFU in options
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.createUSBankAccount()
        )
    }
}

private val usBankAccountStateKey = PaymentMethodDefinition.UiStateKey.create<UsBankAccountState>(UsBankAccountPaymentMethodDefinition)

@Parcelize
internal data class UsBankAccountState(
    val linkAccountSession: String? = null,
) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<UsBankAccountState> = usBankAccountStateKey
}

private class UsBankAccountUiElementDefinition(): UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return uiState[usBankAccountStateKey].linkAccountSession != null
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return UsBankAccountUiRenderer(
            stateFlow = uiState[usBankAccountStateKey],
            onBankAccountRemoved = {
                uiState.update(usBankAccountStateKey) { state ->
                    state.copy(linkAccountSession = null)
                }
            }
        )
    }
}

private class UsBankAccountUiRenderer(
    private val stateFlow: StateFlow<UsBankAccountState>,
    private val onBankAccountRemoved: () -> Unit,
) : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        val state by stateFlow.collectAsState()
        if (state.linkAccountSession != null) {
            // TODO: Add US Bank UI + cancel thing.
            // TODO: Mandate only when state is not null.
        }
    }
}

private object UsBankPrimaryButtonCustomizer : PrimaryButtonCustomizer {
    override fun customize(uiState: UiState): Flow<PrimaryButtonCustomizer.State?> {
        return uiState[usBankAccountStateKey].map { state ->
            if (state.linkAccountSession == null) {
                // TODO: Set enabled based on if the rest of the form is filled out or not.
                PrimaryButtonCustomizer.State(text = resolvableString(R.string.stripe_continue_button_label), enabled = true) {
                    // TODO: Maybe update the state, and observe it in the UiRenderer, then launch the thing, and listen for updates there.
                }
            } else {
                // Perform the default action, with the default text when we already have a session.
                null
            }
        }
    }
}
