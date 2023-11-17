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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentMethodUpdateParams(
    internal val type: PaymentMethod.Type,
) : StripeParamsModel, Parcelable {

    internal abstract val billingDetails: PaymentMethod.BillingDetails?

    internal abstract fun generateTypeParams(): Map<String, Any>

    override fun toParamMap(): Map<String, Any> {
        val typeParams = mapOf(type.code to generateTypeParams())

        val billingInfo = billingDetails?.let {
            mapOf(PARAM_BILLING_DETAILS to it.toParamMap())
        }.orEmpty()

        return billingInfo + typeParams
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    class Card internal constructor(
        internal val expiryMonth: Int? = null,
        internal val expiryYear: Int? = null,
        internal val networks: Networks? = null,
        override val billingDetails: PaymentMethod.BillingDetails?,
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
                const val PARAM_PREFERRED = "preferred"
            }
        }

        private companion object {
            const val PARAM_EXP_MONTH: String = "exp_month"
            const val PARAM_EXP_YEAR: String = "exp_year"
            const val PARAM_NETWORKS: String = "networks"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        private const val PARAM_TYPE = "type"
        private const val PARAM_BILLING_DETAILS = "billing_details"

        @JvmStatic
        @JvmOverloads
        fun createCard(
            expiryMonth: Int? = null,
            expiryYear: Int? = null,
            networks: Card.Networks? = null,
            billingDetails: PaymentMethod.BillingDetails? = null,
        ): PaymentMethodUpdateParams {
            return Card(
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                networks = networks,
                billingDetails = billingDetails,
            )
        }
    }
}
