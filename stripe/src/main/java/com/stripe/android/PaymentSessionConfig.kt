package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.LayoutRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField
import java.util.Objects
import kotlinx.android.parcel.Parcelize

/**
 * Class that tells [PaymentSession] what functionality it is supporting.
 */
@Parcelize
class PaymentSessionConfig private constructor(
    val hiddenShippingInfoFields: List<String> = emptyList(),
    val optionalShippingInfoFields: List<String> = emptyList(),
    val prepopulatedShippingInfo: ShippingInformation? = null,
    val isShippingInfoRequired: Boolean = false,
    val isShippingMethodRequired: Boolean = false,

    @LayoutRes
    @get:LayoutRes
    val addPaymentMethodFooter: Int = 0,

    val paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)
) : Parcelable {
    class Builder : ObjectBuilder<PaymentSessionConfig> {
        private var shippingInfoRequired = true
        private var shippingMethodsRequired = true
        private var hiddenShippingInfoFields: List<String>? = null
        private var optionalShippingInfoFields: List<String>? = null
        private var shippingInformation: ShippingInformation? = null
        private var paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)

        @LayoutRes
        private var addPaymentMethodFooter: Int = 0

        /**
         * @param hiddenShippingInfoFields fields that should be hidden in the [ShippingInfoWidget]
         */
        fun setHiddenShippingInfoFields(
            @CustomizableShippingField vararg hiddenShippingInfoFields: String
        ): Builder {
            this.hiddenShippingInfoFields = listOf(*hiddenShippingInfoFields)
            return this
        }

        /**
         * @param optionalShippingInfoFields field that should be optional in the [ShippingInfoWidget]
         */
        fun setOptionalShippingInfoFields(
            @CustomizableShippingField vararg optionalShippingInfoFields: String
        ): Builder {
            this.optionalShippingInfoFields = listOf(*optionalShippingInfoFields)
            return this
        }

        /**
         * @param shippingInfo [ShippingInformation] that should pre-populate the [ShippingInfoWidget]
         */
        fun setPrepopulatedShippingInfo(shippingInfo: ShippingInformation?): Builder {
            shippingInformation = shippingInfo
            return this
        }

        /**
         * @param shippingInfoRequired whether a [ShippingInformation] should be required.
         * If it is required, a screen with a [ShippingInfoWidget] can be shown to collect it.
         */
        fun setShippingInfoRequired(shippingInfoRequired: Boolean): Builder {
            this.shippingInfoRequired = shippingInfoRequired
            return this
        }

        /**
         * @param shippingMethodsRequired whether a [com.stripe.android.model.ShippingMethod]
         * should be required. If it is required, a screen with a
         * [com.stripe.android.view.SelectShippingMethodWidget] can be shown to collect it.
         */
        fun setShippingMethodsRequired(shippingMethodsRequired: Boolean): Builder {
            this.shippingMethodsRequired = shippingMethodsRequired
            return this
        }

        fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooterLayoutId: Int): Builder {
            this.addPaymentMethodFooter = addPaymentMethodFooterLayoutId
            return this
        }

        // TODO(mshafrir-stripe): make public
        /**
         * @param paymentMethodTypes a list of [PaymentMethod.Type] that indicates the types of
         * Payment Methods that the customer can select or add via the Stripe UI components.
         * If not specified or empty, [PaymentMethod.Type.Card] will be used.
         */
        internal fun setPaymentMethodTypes(paymentMethodTypes: List<PaymentMethod.Type>): Builder {
            this.paymentMethodTypes = paymentMethodTypes
            return this
        }

        override fun build(): PaymentSessionConfig {
            return PaymentSessionConfig(
                hiddenShippingInfoFields = hiddenShippingInfoFields.orEmpty(),
                optionalShippingInfoFields = optionalShippingInfoFields.orEmpty(),
                prepopulatedShippingInfo = shippingInformation,
                isShippingInfoRequired = shippingInfoRequired,
                isShippingMethodRequired = shippingMethodsRequired,
                addPaymentMethodFooter = addPaymentMethodFooter,
                paymentMethodTypes = paymentMethodTypes
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is PaymentSessionConfig -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(obj: PaymentSessionConfig): Boolean {
        return hiddenShippingInfoFields == obj.hiddenShippingInfoFields &&
            optionalShippingInfoFields == obj.optionalShippingInfoFields &&
            prepopulatedShippingInfo == obj.prepopulatedShippingInfo &&
            isShippingInfoRequired == obj.isShippingInfoRequired &&
            isShippingMethodRequired == obj.isShippingMethodRequired &&
            addPaymentMethodFooter == obj.addPaymentMethodFooter
    }

    override fun hashCode(): Int {
        return Objects.hash(hiddenShippingInfoFields, optionalShippingInfoFields,
            prepopulatedShippingInfo, isShippingInfoRequired, isShippingMethodRequired,
            addPaymentMethodFooter)
    }

    companion object {
        internal val EMPTY = PaymentSessionConfig()

        private fun readStringList(parcel: Parcel): List<String> {
            return readList(parcel, String::class.java.classLoader)
        }

        private fun <T> readList(parcel: Parcel, classLoader: ClassLoader?): List<T> {
            val inList: List<T> = mutableListOf()
            parcel.readList(inList, classLoader)
            return inList.toList()
        }
    }
}
