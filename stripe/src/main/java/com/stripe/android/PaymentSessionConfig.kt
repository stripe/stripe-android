package com.stripe.android

import android.os.Parcelable
import androidx.annotation.LayoutRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.SelectShippingMethodWidget
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField
import kotlinx.android.parcel.Parcelize

/**
 * Configuration for [PaymentSession].
 */
@Parcelize
data class PaymentSessionConfig internal constructor(
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
         * @param hiddenShippingInfoFields [CustomizableShippingField] fields that should be
         * hidden in the shipping information screen. All fields will be shown if this list is
         * empty. Note that not all fields can be hidden, such as country or name.
         */
        fun setHiddenShippingInfoFields(
            @CustomizableShippingField vararg hiddenShippingInfoFields: String
        ): Builder {
            this.hiddenShippingInfoFields = listOf(*hiddenShippingInfoFields)
            return this
        }

        /**
         * @param optionalShippingInfoFields [CustomizableShippingField] fields that should be
         * optional in the [ShippingInfoWidget]
         */
        fun setOptionalShippingInfoFields(
            @CustomizableShippingField vararg optionalShippingInfoFields: String
        ): Builder {
            this.optionalShippingInfoFields = listOf(*optionalShippingInfoFields)
            return this
        }

        /**
         * @param shippingInfo [ShippingInformation] that will pre-populate the [ShippingInfoWidget]
         */
        fun setPrepopulatedShippingInfo(shippingInfo: ShippingInformation?): Builder {
            shippingInformation = shippingInfo
            return this
        }

        /**
         * @param shippingInfoRequired whether a [ShippingInformation] should be required.
         * If it is required, a screen with a [ShippingInfoWidget] is shown to collect it.
         *
         * Default is `true`.
         */
        fun setShippingInfoRequired(shippingInfoRequired: Boolean): Builder {
            this.shippingInfoRequired = shippingInfoRequired
            return this
        }

        /**
         * @param shippingMethodsRequired whether a [com.stripe.android.model.ShippingMethod]
         * should be required. If it is required, a screen with a [SelectShippingMethodWidget]
         * is shown to collect it.
         *
         * Default is `true`.
         */
        fun setShippingMethodsRequired(shippingMethodsRequired: Boolean): Builder {
            this.shippingMethodsRequired = shippingMethodsRequired
            return this
        }

        fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooterLayoutId: Int): Builder {
            this.addPaymentMethodFooter = addPaymentMethodFooterLayoutId
            return this
        }

        /**
         * @param paymentMethodTypes a list of [PaymentMethod.Type] that indicates the types of
         * Payment Methods that the customer can select or add via Stripe UI components.
         *
         * The order of the [PaymentMethod.Type] values in the list will be used to
         * arrange the add buttons in the Stripe UI components. They will be arranged vertically
         * from first to last.
         *
         * Currently only [PaymentMethod.Type.Card] and [PaymentMethod.Type.Fpx] are supported.
         * If not specified or empty, [PaymentMethod.Type.Card] will be used.
         */
        fun setPaymentMethodTypes(paymentMethodTypes: List<PaymentMethod.Type>): Builder {
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

    internal companion object {
        internal val EMPTY = PaymentSessionConfig()
    }
}
