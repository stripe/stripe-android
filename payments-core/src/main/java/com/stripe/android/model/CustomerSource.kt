package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Model of the "data" object inside a [Customer] "source" object.
 */
sealed class CustomerPaymentSource : StripeModel {
    abstract val id: String?
    abstract val tokenizationMethod: TokenizationMethod?
}

@Parcelize
data class CustomerCard(
    val card: Card
) : CustomerPaymentSource() {
    override val id: String? get() = card.id

    override val tokenizationMethod: TokenizationMethod?
        get() {
            return card.tokenizationMethod
        }
}

@Parcelize
data class CustomerBankAccount(
    val bankAccount: BankAccount
) : CustomerPaymentSource() {
    override val id: String? get() = bankAccount.id

    override val tokenizationMethod: TokenizationMethod? get() = null
}

@Parcelize
data class CustomerSource(
    val source: Source
) : CustomerPaymentSource() {
    override val id: String? get() = source.id

    override val tokenizationMethod: TokenizationMethod?
        get() {
            return when (source.sourceTypeModel) {
                is SourceTypeModel.Card -> {
                    source.sourceTypeModel.tokenizationMethod
                }
                else -> null
            }
        }
}
