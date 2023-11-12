package com.stripe.android.paymentsheet.prototype.paymentmethods

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.ContactInformationCollectionMode
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.paymentsheet.prototype.uielement.TextFieldUiElementDefinition.Companion.createTextField
import com.stripe.android.ui.core.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal object SepaDebitPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.SepaDebit

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_sepa_debit)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_sepa_debit
            }

            requireContactInformation(ContactInformationCollectionMode.Name)
            requireContactInformation(ContactInformationCollectionMode.Email)
            element(
                createTextField(
                    label = resolvableString(R.string.stripe_iban),
                    key = sepaDebitStateKey,
                    mapper = { sepaDebitState -> sepaDebitState.iban },
                    updater = { sepaDebitState, updatedValue ->
                        sepaDebitState.copy(iban = updatedValue)
                    },
                    // TODO: Add formatting and validation for IBAN.
                    // TODO: Add icon to right side.
                )
            )
            requireBillingAddress()
            mandate(resolvableString(R.string.stripe_sepa_mandate, metadata.merchantName))
        }

        state(SepaDebitState())
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        val sepaDebitState = uiState[sepaDebitStateKey]
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.create(
                sepaDebit = PaymentMethodCreateParams.SepaDebit(sepaDebitState.iban),
            )
        )
    }
}

private val sepaDebitStateKey = PaymentMethodDefinition.UiStateKey.create<SepaDebitState>(SepaDebitPaymentMethodDefinition)

@Parcelize
internal data class SepaDebitState(
    val iban: String = "",
) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<SepaDebitState> = sepaDebitStateKey
}
