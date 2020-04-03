package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter.Args
import com.stripe.android.view.PaymentMethodsActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.PaymentMethodsActivityStarter.Result
import com.stripe.android.view.PaymentMethodsActivityStarter.Result.Companion.fromIntent
import kotlinx.android.parcel.Parcelize

/**
 * A class to start [PaymentMethodsActivity]. Arguments for the activity can be specified
 * with [Args] and constructed with [Args.Builder].
 *
 * The result data is a [Result] instance, obtained using [Result.fromIntent]}.
 * The result will be returned with request code [REQUEST_CODE].
 */
class PaymentMethodsActivityStarter : ActivityStarter<PaymentMethodsActivity, Args> {

    constructor(activity: Activity) : super(
        activity,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    constructor(activity: Activity, args: Args) : super (
            activity,
            PaymentMethodsActivity::class.java,
            args,
            REQUEST_CODE
    )

    @Parcelize
    data class Args internal constructor(
        internal val initialPaymentMethodId: String?,
        @LayoutRes val addPaymentMethodFooterLayoutId: Int,
        internal val isPaymentSessionActive: Boolean,
        internal val paymentMethodTypes: List<PaymentMethod.Type>,
        internal val paymentConfiguration: PaymentConfiguration?,
        internal val windowFlags: Int? = null,
        internal val billingAddressFields: BillingAddressFields,
        internal val shouldShowGooglePay: Boolean = false,
        internal val useGooglePay: Boolean = false
    ) : ActivityStarter.Args {
        class Builder : ObjectBuilder<Args> {
            private var billingAddressFields: BillingAddressFields = BillingAddressFields.None
            private var initialPaymentMethodId: String? = null
            private var isPaymentSessionActive = false
            private var paymentMethodTypes: List<PaymentMethod.Type>? = null
            private var shouldShowGooglePay: Boolean = false
            private var useGooglePay: Boolean = false
            private var paymentConfiguration: PaymentConfiguration? = null
            @LayoutRes
            private var addPaymentMethodFooterLayoutId: Int = 0
            private var windowFlags: Int? = null

            /**
             * @param billingAddressFields the billing address fields to require on [AddPaymentMethodActivity]
             */
            fun setBillingAddressFields(
                billingAddressFields: BillingAddressFields
            ): Builder = apply {
                this.billingAddressFields = billingAddressFields
            }

            fun setInitialPaymentMethodId(initialPaymentMethodId: String?): Builder = apply {
                this.initialPaymentMethodId = initialPaymentMethodId
            }

            fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder = apply {
                this.isPaymentSessionActive = isPaymentSessionActive
            }

            fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder = apply {
                this.paymentConfiguration = paymentConfiguration
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
            fun setPaymentMethodTypes(
                paymentMethodTypes: List<PaymentMethod.Type>
            ): Builder = apply {
                this.paymentMethodTypes = paymentMethodTypes
            }

            /**
             * @param shouldShowGooglePay if `true`, will show "Google Pay" as an option on the
             * Payment Methods selection screen. If a user selects the Google Pay option,
             * [PaymentMethodsActivityStarter.Result.useGooglePay] will be `true`.
             */
            fun setShouldShowGooglePay(shouldShowGooglePay: Boolean): Builder = apply {
                this.shouldShowGooglePay = shouldShowGooglePay
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
             * @param windowFlags optional flags to set on the `Window` object of Stripe Activities
             *
             * See [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
             */
            fun setWindowFlags(windowFlags: Int?): Builder = apply {
                this.windowFlags = windowFlags
            }

            internal fun setUseGooglePay(useGooglePay: Boolean): Builder = apply {
                this.useGooglePay = useGooglePay
            }

            override fun build(): Args {
                return Args(
                    initialPaymentMethodId = initialPaymentMethodId,
                    isPaymentSessionActive = isPaymentSessionActive,
                    paymentMethodTypes = paymentMethodTypes ?: listOf(PaymentMethod.Type.Card),
                    shouldShowGooglePay = shouldShowGooglePay,
                    useGooglePay = useGooglePay,
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId,
                    windowFlags = windowFlags,
                    billingAddressFields = billingAddressFields
                )
            }
        }

        internal companion object {
            internal val DEFAULT = Builder().build()

            @JvmSynthetic
            internal fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }
        }
    }

    /**
     * The result of a [PaymentMethodsActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    @Parcelize
    data class Result internal constructor(
        @JvmField val paymentMethod: PaymentMethod? = null,
        val useGooglePay: Boolean = false
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return Bundle().also {
                it.putParcelable(ActivityStarter.Result.EXTRA, this)
            }
        }

        companion object {
            /**
             * @return the [Result] object from the given `Intent`
             */
            @JvmStatic
            fun fromIntent(intent: Intent?): Result? {
                return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6000
    }
}
