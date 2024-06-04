package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.util.Objects

/**
 * Model for PaymentMethod update parameters.
 *
 * See [Update a PaymentMethod](https://stripe.com/docs/api/payment_methods/update).
 */
sealed class PaymentMethodUpdateParams(
    internal val type: PaymentMethod.Type,
) : StripeParamsModel, Parcelable {

    internal abstract val billingDetails: PaymentMethod.BillingDetails?
    internal abstract val productUsageTokens: Set<String>

    internal abstract fun generateTypeParams(): Map<String, Any>

    override fun toParamMap(): Map<String, Any> {
        val typeParams = mapOf(type.code to generateTypeParams())

        val billingInfo = billingDetails?.let {
            mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
        }.orEmpty()

        return billingInfo + typeParams
    }

    @Parcelize
    class Card internal constructor(
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
        val networks: Networks? = null,
        override val billingDetails: PaymentMethod.BillingDetails?,
        override val productUsageTokens: Set<String> = emptySet(),
    ) : PaymentMethodUpdateParams(PaymentMethod.Type.Card) {

        override fun generateTypeParams(): Map<String, Any> {
            return listOf(
                PARAM_EXP_MONTH to expiryMonth,
                PARAM_EXP_YEAR to expiryYear,
                PARAM_NETWORKS to networks?.toParamMap(),
            ).mapNotNull {
                it.second?.let { value ->
                    it.first to value
                }
            }.toMap()
        }

        override fun equals(other: Any?): Boolean {
            return other is Card &&
                other.expiryMonth == expiryMonth &&
                other.expiryYear == expiryYear &&
                other.networks == networks &&
                other.billingDetails == billingDetails
        }

        override fun hashCode(): Int {
            return Objects.hash(expiryMonth, expiryYear, networks, billingDetails)
        }

        override fun toString(): String {
            return "PaymentMethodCreateParams.Card.(" +
                "expiryMonth=$expiryMonth, " +
                "expiryYear=$expiryYear, " +
                "networks=$networks, " +
                "billingDetails=$billingDetails" +
                ")"
        }

        @Parcelize
        class Networks(
            val preferred: String? = null,
        ) : StripeParamsModel, Parcelable {

            override fun toParamMap(): Map<String, Any> {
                return if (preferred != null) {
                    mapOf(PARAM_PREFERRED to preferred)
                } else {
                    emptyMap()
                }
            }

            override fun equals(other: Any?): Boolean {
                return other is Networks && other.preferred == preferred
            }

            override fun hashCode(): Int {
                return Objects.hash(preferred)
            }

            override fun toString(): String {
                return "PaymentMethodCreateParams.Card.Networks(preferred=$preferred)"
            }

            private companion object {
                private const val PARAM_PREFERRED = "preferred"
            }
        }

        private companion object {
            private const val PARAM_EXP_MONTH: String = "exp_month"
            private const val PARAM_EXP_YEAR: String = "exp_year"
            private const val PARAM_NETWORKS: String = "networks"
        }
    }

    companion object {

        private const val PARAM_BILLING_DETAILS = "billing_details"

        @JvmStatic
        @JvmOverloads
        fun createCard(
            expiryMonth: Int? = null,
            expiryYear: Int? = null,
            networks: Card.Networks? = null,
            billingDetails: PaymentMethod.BillingDetails? = null,
        ): PaymentMethodUpdateParams {
            return Card(expiryMonth, expiryYear, networks, billingDetails)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @JvmOverloads
        fun createCard(
            expiryMonth: Int? = null,
            expiryYear: Int? = null,
            networks: Card.Networks? = null,
            billingDetails: PaymentMethod.BillingDetails? = null,
            productUsageTokens: Set<String>,
        ): PaymentMethodUpdateParams {
            return Card(expiryMonth, expiryYear, networks, billingDetails, productUsageTokens)
        }
    }
}
