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
import com.stripe.android.paymentsheet.prototype.uielement.DropdownItem
import com.stripe.android.paymentsheet.prototype.uielement.DropdownUiElementDefinition.Companion.createDropdownField
import com.stripe.android.ui.core.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal object IdealPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Ideal

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOfNotNull(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.takeIf { hasIntentToSetup },
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        val dropdownItems = parseDropdownItems("ideal[bank]") ?: defaultBanks()

        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_ideal)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_ideal
            }

            requireContactInformation(ContactInformationCollectionMode.Name)

            element(
                createDropdownField(
                    label = resolvableString(R.string.stripe_ideal_bank),
                    availableItems = dropdownItems,
                    key = idealStateKey,
                    mapper = { state -> state.selectedBank },
                    updater = { state, updatedValue -> state.copy(selectedBank = updatedValue) },
                )
            )

            if (metadata.hasIntentToSetup()) {
                mandate(resolvableString(R.string.stripe_sepa_mandate, metadata.merchantName))
            }
        }

        state(IdealState(dropdownItems.first().identifier))
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        val idealState = uiState[idealStateKey]
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.create(
                ideal = PaymentMethodCreateParams.Ideal(idealState.selectedBank),
            )
        )
    }
}

private val idealStateKey = PaymentMethodDefinition.UiStateKey.create<IdealState>(IdealPaymentMethodDefinition)

@Parcelize
internal data class IdealState(
    val selectedBank: String,
) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<IdealState> = idealStateKey
}

private fun defaultBanks(): List<DropdownItem> {
    return listOf(
        DropdownItem(identifier = "abn_amro", displayText = "ABN Amro"),
        DropdownItem(identifier = "asn_bank", displayText = "ASN Bank"),
        DropdownItem(identifier = "bunq", displayText = "bunq B.V."),
        DropdownItem(identifier = "ing", displayText = "ING Bank"),
        DropdownItem(identifier = "knab", displayText = "Knab"),
        DropdownItem(identifier = "n26", displayText = "N26"),
        DropdownItem(identifier = "rabobank", displayText = "Rabobank"),
        DropdownItem(identifier = "regiobank", displayText = "RegioBank"),
        DropdownItem(identifier = "revolut", displayText = "Revolut"),
        DropdownItem(identifier = "sns_bank", displayText = "SNS Bank"),
        DropdownItem(identifier = "triodos_bank", displayText = "Triodos Bank"),
        DropdownItem(identifier = "van_lanschot", displayText = "Van Lanschot"),
        DropdownItem(identifier = "yoursafe", displayText = "Yoursafe"),
    )
}
