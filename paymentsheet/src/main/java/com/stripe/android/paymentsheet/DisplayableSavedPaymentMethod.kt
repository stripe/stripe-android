package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal data class DisplayableSavedPaymentMethod private constructor(
    val displayName: ResolvableString,
    val paymentMethod: PaymentMethod,
    val savedPaymentMethod: SavedPaymentMethod,
    val isCbcEligible: Boolean = false,
    val shouldShowDefaultBadge: Boolean = false
) {
    fun isModifiable(): Boolean {
        return when (savedPaymentMethod) {
            is SavedPaymentMethod.Card -> {
                val hasMultipleNetworks = savedPaymentMethod.card.networks?.available?.let { available ->
                    available.size > 1
                } ?: false

                return isCbcEligible && hasMultipleNetworks
            }
            is SavedPaymentMethod.SepaDebit,
            is SavedPaymentMethod.USBankAccount,
            SavedPaymentMethod.Unexpected -> false
        }
    }

    fun getDescription() = when (savedPaymentMethod) {
        is SavedPaymentMethod.Card -> {
            resolvableString(
                com.stripe.android.R.string.stripe_card_ending_in,
                brandDisplayName(),
                savedPaymentMethod.card.last4
            )
        }
        is SavedPaymentMethod.SepaDebit -> resolvableString(
            R.string.stripe_bank_account_ending_in,
            savedPaymentMethod.sepaDebit.last4
        )
        is SavedPaymentMethod.USBankAccount -> resolvableString(
            R.string.stripe_bank_account_ending_in,
            savedPaymentMethod.usBankAccount.last4
        )
        is SavedPaymentMethod.Unexpected -> resolvableString("")
    }

    fun getModifyDescription() = resolvableString(
        R.string.stripe_paymentsheet_modify_pm,
        getDescription()
    )

    fun getRemoveDescription(): ResolvableString {
        return resolvableString(
            R.string.stripe_paymentsheet_remove_pm,
            getDescription(),
        )
    }

    fun brandDisplayName(): String? {
        return when (savedPaymentMethod) {
            is SavedPaymentMethod.Card -> {
                val brand = savedPaymentMethod.card.displayBrand?.let { CardBrand.fromCode(it) }
                    ?: savedPaymentMethod.card.brand
                return brand.displayName
            }
            is SavedPaymentMethod.USBankAccount,
            is SavedPaymentMethod.SepaDebit,
            is SavedPaymentMethod.Unexpected -> null
        }
    }

    fun isDefaultPaymentMethod(defaultPaymentMethodId: String?): Boolean {
        return paymentMethod.id != null && paymentMethod.id == defaultPaymentMethodId
    }

    companion object {
        fun create(
            displayName: ResolvableString,
            paymentMethod: PaymentMethod,
            isCbcEligible: Boolean = false,
            shouldShowDefaultBadge: Boolean = false
        ): DisplayableSavedPaymentMethod {
            val savedPaymentMethod = when (paymentMethod.type) {
                PaymentMethod.Type.Card -> {
                    paymentMethod.card?.let { card ->
                        SavedPaymentMethod.Card(
                            card = card,
                            billingDetails = paymentMethod.billingDetails
                        )
                    }
                }
                PaymentMethod.Type.USBankAccount -> paymentMethod.usBankAccount?.let {
                    SavedPaymentMethod.USBankAccount(
                        it
                    )
                }
                PaymentMethod.Type.SepaDebit -> paymentMethod.sepaDebit?.let { SavedPaymentMethod.SepaDebit(it) }
                else -> null
            }

            return DisplayableSavedPaymentMethod(
                displayName = displayName,
                paymentMethod = paymentMethod,
                savedPaymentMethod = savedPaymentMethod ?: SavedPaymentMethod.Unexpected,
                isCbcEligible = isCbcEligible,
                shouldShowDefaultBadge = shouldShowDefaultBadge
            )
        }
    }
}

internal sealed interface SavedPaymentMethod {
    data class Card(
        val card: PaymentMethod.Card,
        val billingDetails: PaymentMethod.BillingDetails?
    ) : SavedPaymentMethod {
        fun isExpired(): Boolean {
            val cardExpiryMonth = card.expiryMonth
            val cardExpiryYear = card.expiryYear
            // If the card's expiration dates are missing, we can't conclude that it is expired, so we don't want to
            // show the user an expired card error.
            return cardExpiryMonth != null && cardExpiryYear != null &&
                !DateUtils.isExpiryDataValid(
                    expiryMonth = cardExpiryMonth,
                    expiryYear = cardExpiryYear,
                )
        }
    }
    data class USBankAccount(val usBankAccount: PaymentMethod.USBankAccount) : SavedPaymentMethod
    data class SepaDebit(val sepaDebit: PaymentMethod.SepaDebit) : SavedPaymentMethod
    data object Unexpected : SavedPaymentMethod
}
