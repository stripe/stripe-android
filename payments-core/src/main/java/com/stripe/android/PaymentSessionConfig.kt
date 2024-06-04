package com.stripe.android

import android.os.Parcelable
import androidx.annotation.LayoutRes
import androidx.annotation.WorkerThread
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.SelectShippingMethodWidget
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.Locale

/**
 * Configuration for [PaymentSession].
 */
@Parcelize
data class PaymentSessionConfig internal constructor(
    val hiddenShippingInfoFields: List<CustomizableShippingField> = emptyList(),
    val optionalShippingInfoFields: List<CustomizableShippingField> = emptyList(),
    val prepopulatedShippingInfo: ShippingInformation? = null,
    val isShippingInfoRequired: Boolean = false,
    val isShippingMethodRequired: Boolean = false,

    @LayoutRes
    @get:LayoutRes
    val paymentMethodsFooterLayoutId: Int = 0,

    @LayoutRes
    @get:LayoutRes
    val addPaymentMethodFooterLayoutId: Int = 0,

    val paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card),
    val shouldShowGooglePay: Boolean = false,
    val allowedShippingCountryCodes: Set<String> = emptySet(),
    val billingAddressFields: BillingAddressFields = DEFAULT_BILLING_ADDRESS_FIELDS,
    val canDeletePaymentMethods: Boolean = true,

    internal val shouldPrefetchCustomer: Boolean = true,
    internal val shippingInformationValidator: ShippingInformationValidator = DefaultShippingInfoValidator(),
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

        if (isShippingMethodRequired) {
            requireNotNull(shippingMethodsFactory) {
                """
                If isShippingMethodRequired is true a ShippingMethodsFactory must also be provided.
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

    class Builder {
        private var billingAddressFields: BillingAddressFields = DEFAULT_BILLING_ADDRESS_FIELDS
        private var shippingInfoRequired = true
        private var shippingMethodsRequired = true
        private var hiddenShippingInfoFields: List<CustomizableShippingField>? = null
        private var optionalShippingInfoFields: List<CustomizableShippingField>? = null
        private var shippingInformation: ShippingInformation? = null
        private var paymentMethodTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)
        private var shouldShowGooglePay: Boolean = false
        private var allowedShippingCountryCodes: Set<String> = emptySet()
        private var shippingInformationValidator: ShippingInformationValidator? = null
        private var shippingMethodsFactory: ShippingMethodsFactory? = null
        private var windowFlags: Int? = null
        private var shouldPrefetchCustomer: Boolean = true
        private var canDeletePaymentMethods: Boolean = true

        @LayoutRes
        private var paymentMethodsFooterLayoutId: Int = 0

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
            vararg hiddenShippingInfoFields: CustomizableShippingField
        ): Builder = apply {
            this.hiddenShippingInfoFields = listOf(*hiddenShippingInfoFields)
        }

        /**
         * @param optionalShippingInfoFields [CustomizableShippingField] fields that should be
         * optional in the [ShippingInfoWidget]
         */
        fun setOptionalShippingInfoFields(
            vararg optionalShippingInfoFields: CustomizableShippingField
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
         * @param paymentMethodsFooterLayoutId optional layout id that will be inflated and
         * displayed beneath the payment method selection list on [PaymentMethodsActivity]
         */
        fun setPaymentMethodsFooter(
            @LayoutRes paymentMethodsFooterLayoutId: Int
        ): Builder = apply {
            this.paymentMethodsFooterLayoutId = paymentMethodsFooterLayoutId
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
         * @param shouldShowGooglePay if `true`, will show "Google Pay" as an option on the
         * Payment Methods selection screen. If a user selects the Google Pay option,
         * [PaymentSessionData.useGooglePay] will be `true`.
         */
        fun setShouldShowGooglePay(shouldShowGooglePay: Boolean): Builder = apply {
            this.shouldShowGooglePay = shouldShowGooglePay
        }

        /**
         * @param canDeletePaymentMethods controls whether the user can
         * delete a payment method by swiping on it in [PaymentMethodsActivity]. Defaults to true.
         */
        fun setCanDeletePaymentMethods(canDeletePaymentMethods: Boolean): Builder = apply {
            this.canDeletePaymentMethods = canDeletePaymentMethods
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
         * @param shippingInformationValidator used to validate [ShippingInformation] in [PaymentFlowActivity]
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

        /**
         * @param shouldPrefetchCustomer If true, will immediately fetch the [Customer] associated
         * with this session. Otherwise, will only fetch when needed.
         *
         * Defaults to true.
         */
        fun setShouldPrefetchCustomer(shouldPrefetchCustomer: Boolean): Builder = apply {
            this.shouldPrefetchCustomer = shouldPrefetchCustomer
        }

        fun build(): PaymentSessionConfig {
            return PaymentSessionConfig(
                hiddenShippingInfoFields = hiddenShippingInfoFields.orEmpty(),
                optionalShippingInfoFields = optionalShippingInfoFields.orEmpty(),
                prepopulatedShippingInfo = shippingInformation,
                isShippingInfoRequired = shippingInfoRequired,
                isShippingMethodRequired = shippingMethodsRequired,
                paymentMethodsFooterLayoutId = paymentMethodsFooterLayoutId,
                addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId,
                paymentMethodTypes = paymentMethodTypes,
                shouldShowGooglePay = shouldShowGooglePay,
                allowedShippingCountryCodes = allowedShippingCountryCodes,
                shippingInformationValidator = shippingInformationValidator
                    ?: DefaultShippingInfoValidator(),
                shippingMethodsFactory = shippingMethodsFactory,
                windowFlags = windowFlags,
                billingAddressFields = billingAddressFields,
                shouldPrefetchCustomer = shouldPrefetchCustomer,
                canDeletePaymentMethods = canDeletePaymentMethods
            )
        }
    }

    /**
     * A [ShippingInformationValidator] that accepts any [ShippingInformation] as valid.
     */
    private class DefaultShippingInfoValidator : ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return true
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            return ""
        }
    }

    private companion object {
        private val DEFAULT_BILLING_ADDRESS_FIELDS = BillingAddressFields.PostalCode
    }
}
