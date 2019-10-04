package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.LayoutRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField
import java.util.Objects

/**
 * Class that tells [PaymentSession] what functionality it is supporting.
 */
class PaymentSessionConfig private constructor(
    val hiddenShippingInfoFields: List<String>,
    val optionalShippingInfoFields: List<String>,
    val prepopulatedShippingInfo: ShippingInformation?,
    val isShippingInfoRequired: Boolean,
    val isShippingMethodRequired: Boolean,

    @LayoutRes
    @get:LayoutRes
    val addPaymentMethodFooter: Int,

    val paymentMethodTypes: List<PaymentMethod.Type>
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

    private constructor(parcel: Parcel) : this(
        hiddenShippingInfoFields = readStringList(parcel),
        optionalShippingInfoFields = readStringList(parcel),
        prepopulatedShippingInfo = parcel.readParcelable(ShippingInformation::class.java.classLoader),
        isShippingInfoRequired = parcel.readInt() == 1,
        isShippingMethodRequired = parcel.readInt() == 1,
        addPaymentMethodFooter = parcel.readInt(),
        paymentMethodTypes = readList(parcel, PaymentMethod.Type::class.java.classLoader)
    )

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

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeList(hiddenShippingInfoFields)
        parcel.writeList(optionalShippingInfoFields)
        parcel.writeParcelable(prepopulatedShippingInfo, flags)
        parcel.writeInt(if (isShippingInfoRequired) 1 else 0)
        parcel.writeInt(if (isShippingMethodRequired) 1 else 0)
        parcel.writeInt(addPaymentMethodFooter)
        parcel.writeList(paymentMethodTypes)
    }

    companion object {
        private fun readStringList(parcel: Parcel): List<String> {
            return readList(parcel, String::class.java.classLoader)
        }

        private fun <T> readList(parcel: Parcel, classLoader: ClassLoader?): List<T> {
            val inList: List<T> = mutableListOf()
            parcel.readList(inList, classLoader)
            return inList.toList()
        }

        @JvmField
        val CREATOR: Parcelable.Creator<PaymentSessionConfig> =
            object : Parcelable.Creator<PaymentSessionConfig> {
                override fun createFromParcel(parcel: Parcel): PaymentSessionConfig {
                    return PaymentSessionConfig(parcel)
                }

                override fun newArray(size: Int): Array<PaymentSessionConfig?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
