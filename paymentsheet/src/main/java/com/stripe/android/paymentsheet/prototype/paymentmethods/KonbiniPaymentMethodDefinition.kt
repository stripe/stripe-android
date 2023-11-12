package com.stripe.android.paymentsheet.prototype.paymentmethods

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
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

internal object KonbiniPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Konbini

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOfNotNull(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_konbini)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_konbini
            }

            requireContactInformation(ContactInformationCollectionMode.Name)
            requireContactInformation(ContactInformationCollectionMode.Email)
            element(
                createTextField(
                    label = resolvableString(R.string.stripe_konbini_confirmation_number_label),
                    key = konbiniStateKey,
                    mapper = { state -> state.confirmationNumber },
                    updater = { state, updatedValue ->
                        state.copy(confirmationNumber = updatedValue)
                    }
                )
            )
        }

        state(KonbiniState())
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        val confirmationNumber = uiState[konbiniStateKey].confirmationNumber
        return PaymentMethodConfirmParams(
            createParams = PaymentMethodCreateParams.createKonbini(),
            optionsParams = PaymentMethodOptionsParams.Konbini(confirmationNumber),
        )
    }
}

private val konbiniStateKey = PaymentMethodDefinition.UiStateKey.create<KonbiniState>(KonbiniPaymentMethodDefinition)

@Parcelize
internal data class KonbiniState(
    val confirmationNumber: String = "",
) : UiState.Value {
    @IgnoredOnParcel
    override val key: UiState.Key<KonbiniState> = konbiniStateKey
}
