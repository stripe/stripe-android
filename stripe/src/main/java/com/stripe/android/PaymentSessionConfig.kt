package com.stripe.android

import android.os.Parcelable
import androidx.annotation.LayoutRes
import androidx.annotation.WorkerThread
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowExtras
import com.stripe.android.view.SelectShippingMethodWidget
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField
import java.io.Serializable
import java.util.Locale
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
    val addPaymentMethodFooterLayoutId: Int = 0,
    val paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card),
    val allowedShippingCountryCodes: Set<String> = emptySet(),
    val billingAddressFields: BillingAddressFields = BillingAddressFields.None,

    internal val shippingInformationValidator: ShippingInformationValidator? = null,
    internal val shippingMethodsFactory: ShippingMethodsFactory? = null,
    internal val windowFlags: Int? = null
) : Parcelable {
    init {
        val countryCodes = Locale.getISOCountries()
        allowedShippingCountryCodes.forEach { allowedShippingCountryCode ->
            require(
                countryCodes.any { allowedShippingCountryCode.equals(it, ignoreCase = true) }
            ) {
                "'$allowedShippingCountryCode' is not a valid country code"
            }
        }

        if (shippingInformationValidator != null && isShippingMethodRequired) {
            requireNotNull(shippingMethodsFactory) {
                """
                If isShippingMethodRequired is true and a ShippingInformationValidator is provided,
                a ShippingMethodsFactory must also be provided.
                """.trimIndent()
            }
        }
    }

    interface ShippingInformationValidator : Serializable {
        /**
         * @return whether the customer's [ShippingInformation] is valid. Will run on
         * a background thread.
         */
        @WorkerThread
        fun isValid(shippingInformation: ShippingInformation): Boolean

        /**
         * @return the error message to show if [isValid] returns `false`. Will run on
         * a background thread.
         */
        @WorkerThread
        fun getErrorMessage(shippingInformation: ShippingInformation): String
    }

    interface ShippingMethodsFactory : Serializable {
        /**
         * @return a list of [ShippingMethod] options to present to the customer. Will run on
         * a background thread.
         */
        @WorkerThread
        fun create(
            shippingInformation: ShippingInformation
        ): List<ShippingMethod>
    }

    class Builder : ObjectBuilder<PaymentSessionConfig> {
        private var billingAddressFields: BillingAddressFields = BillingAddressFields.None
        private var shippingInfoRequired = true
        private var shippingMethodsRequired = true
        private var hiddenShippingInfoFields: List<String>? = null
        private var optionalShippingInfoFields: List<String>? = null
        private var shippingInformation: ShippingInformation? = null
        private var paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)
        private var allowedShippingCountryCodes: Set<String> = emptySet()
        private var shippingInformationValidator: ShippingInformationValidator? = null
        private var shippingMethodsFactory: ShippingMethodsFactory? = null
        private var windowFlags: Int? = null

        @LayoutRes
        private var addPaymentMethodFooterLayoutId: Int = 0

        /**
         * @param billingAddressFields the billing address fields to require on [AddPaymentMethodActivity]
         */
        fun setBillingAddressFields(
            billingAddressFields: BillingAddressFields
        ): Builder = apply {
            this.billingAddressFields = billingAddressFields
        }

        /**
         * @param hiddenShippingInfoFields [CustomizableShippingField] fields that should be
         * hidden in the shipping information screen. All fields will be shown if this list is
         * empty. Note that not all fields can be hidden, such as country or name.
         */
        fun setHiddenShippingInfoFields(
            @CustomizableShippingField vararg hiddenShippingInfoFields: String
        ): Builder = apply {
            this.hiddenShippingInfoFields = listOf(*hiddenShippingInfoFields)
        }

        /**
         * @param optionalShippingInfoFields [CustomizableShippingField] fields that should be
         * optional in the [ShippingInfoWidget]
         */
        fun setOptionalShippingInfoFields(
            @CustomizableShippingField vararg optionalShippingInfoFields: String
        ): Builder = apply {
            this.optionalShippingInfoFields = listOf(*optionalShippingInfoFields)
        }

        /**
         * @param shippingInfo [ShippingInformation] that will pre-populate the [ShippingInfoWidget]
         */
        fun setPrepopulatedShippingInfo(shippingInfo: ShippingInformation?): Builder = apply {
            shippingInformation = shippingInfo
        }

        /**
         * @param shippingInfoRequired whether a [ShippingInformation] should be required.
         * If it is required, a screen with a [ShippingInfoWidget] is shown to collect it.
         *
         * Default is `true`.
         */
        fun setShippingInfoRequired(shippingInfoRequired: Boolean): Builder = apply {
            this.shippingInfoRequired = shippingInfoRequired
        }

        /**
         * @param shippingMethodsRequired whether a [ShippingMethod] should be required.
         * If it is required, a screen with a [SelectShippingMethodWidget] is shown to collect it.
         *
         * Default is `true`.
         */
        fun setShippingMethodsRequired(shippingMethodsRequired: Boolean): Builder = apply {
            this.shippingMethodsRequired = shippingMethodsRequired
        }

        /**
         * @param addPaymentMethodFooterLayoutId optional layout id that will be inflated and
         * displayed beneath the payment details collection form on [AddPaymentMethodActivity]
         */
        fun setAddPaymentMethodFooter(
            @LayoutRes addPaymentMethodFooterLayoutId: Int
        ): Builder = apply {
            this.addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId
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
        fun setPaymentMethodTypes(paymentMethodTypes: List<PaymentMethod.Type>): Builder = apply {
            this.paymentMethodTypes = paymentMethodTypes
        }

        /**
         * @param allowedShippingCountryCodes A set of allowed country codes for the
         * customer's shipping address. Will be ignored if empty.
         */
        fun setAllowedShippingCountryCodes(
            allowedShippingCountryCodes: Set<String>
        ): Builder = apply {
            this.allowedShippingCountryCodes = allowedShippingCountryCodes
        }

        /**
         * @param windowFlags optional flags to set on the `Window` object of Stripe Activities
         *
         * See [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
         */
        fun setWindowFlags(windowFlags: Int?): Builder = apply {
            this.windowFlags = windowFlags
        }

        /**
         * @param shippingInformationValidator if specified, will be used to validate
         * [ShippingInformation] in [PaymentFlowActivity] instead of sending a broadcast with
         * [PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED].
         *
         * Note: this instance must be [Serializable].
         */
        fun setShippingInformationValidator(
            shippingInformationValidator: ShippingInformationValidator?
        ): Builder = apply {
            this.shippingInformationValidator = shippingInformationValidator
        }

        /**
         * @param shippingMethodsFactory required if [shippingInformationValidator] is specified
         * and [shippingMethodsRequired] is `true`. Used to create the [ShippingMethod] options
         * to be displayed in [PaymentFlowActivity].
         *
         * Note: this instance must be [Serializable].
         */
        fun setShippingMethodsFactory(
            shippingMethodsFactory: ShippingMethodsFactory?
        ): Builder = apply {
            this.shippingMethodsFactory = shippingMethodsFactory
        }

        override fun build(): PaymentSessionConfig {
            return PaymentSessionConfig(
                hiddenShippingInfoFields = hiddenShippingInfoFields.orEmpty(),
                optionalShippingInfoFields = optionalShippingInfoFields.orEmpty(),
                prepopulatedShippingInfo = shippingInformation,
                isShippingInfoRequired = shippingInfoRequired,
                isShippingMethodRequired = shippingMethodsRequired,
                addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId,
                paymentMethodTypes = paymentMethodTypes,
                allowedShippingCountryCodes = allowedShippingCountryCodes,
                shippingInformationValidator = shippingInformationValidator,
                shippingMethodsFactory = shippingMethodsFactory,
                windowFlags = windowFlags,
                billingAddressFields = billingAddressFields
            )
        }
    }
}
