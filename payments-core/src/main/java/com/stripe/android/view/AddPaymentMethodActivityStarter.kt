package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.AddPaymentMethodActivityStarter.Args
import com.stripe.android.view.AddPaymentMethodActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.AddPaymentMethodActivityStarter.Result.Companion.fromIntent
import kotlinx.parcelize.Parcelize

/**
 * A class to start [AddPaymentMethodActivity]. Arguments for the activity can be
 * specified with [Args] and constructed with [Args.Builder].
 *
 * The result will be returned with request code [REQUEST_CODE].
 */
class AddPaymentMethodActivityStarter : ActivityStarter<AddPaymentMethodActivity, Args> {

    constructor(activity: Activity) : super(
        activity,
        AddPaymentMethodActivity::class.java,
        REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment,
        AddPaymentMethodActivity::class.java,
        REQUEST_CODE
    )

    @Parcelize
    data class Args internal constructor(
        internal val billingAddressFields: BillingAddressFields,
        internal val shouldAttachToCustomer: Boolean,
        internal val isPaymentSessionActive: Boolean,
        internal val paymentMethodType: PaymentMethod.Type,
        internal val paymentConfiguration: PaymentConfiguration?,
        @LayoutRes internal val addPaymentMethodFooterLayoutId: Int,
        internal val windowFlags: Int? = null
    ) : ActivityStarter.Args {

        class Builder {
            private var billingAddressFields: BillingAddressFields = BillingAddressFields.PostalCode
            private var shouldAttachToCustomer: Boolean = false
            private var isPaymentSessionActive = false
            private var paymentMethodType: PaymentMethod.Type? = PaymentMethod.Type.Card
            private var paymentConfiguration: PaymentConfiguration? = null
            private var windowFlags: Int? = null

            @LayoutRes
            private var addPaymentMethodFooterLayoutId: Int = 0

            /**
             * If true, the created Payment Method will be attached to the current Customer
             * using an already-initialized [com.stripe.android.CustomerSession].
             */
            fun setShouldAttachToCustomer(shouldAttachToCustomer: Boolean): Builder = apply {
                this.shouldAttachToCustomer = shouldAttachToCustomer
            }

            /**
             * @param billingAddressFields the billing address fields to require on [AddPaymentMethodActivity]
             */
            fun setBillingAddressFields(
                billingAddressFields: BillingAddressFields
            ): Builder = apply {
                this.billingAddressFields = billingAddressFields
            }

            /**
             * Optional: specify the [PaymentMethod.Type] of the payment method to create based on
             * the customer's input (i.e. the form that will be presented to the customer).
             * If unspecified, defaults to [PaymentMethod.Type.Card].
             * Currently only [PaymentMethod.Type.Card] and [PaymentMethod.Type.Fpx] are supported.
             */
            fun setPaymentMethodType(paymentMethodType: PaymentMethod.Type): Builder = apply {
                this.paymentMethodType = paymentMethodType
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

            @JvmSynthetic
            internal fun setIsPaymentSessionActive(
                isPaymentSessionActive: Boolean
            ): Builder = apply {
                this.isPaymentSessionActive = isPaymentSessionActive
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @JvmSynthetic
            fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder = apply {
                this.paymentConfiguration = paymentConfiguration
            }

            fun build(): Args {
                return Args(
                    billingAddressFields = billingAddressFields,
                    shouldAttachToCustomer = shouldAttachToCustomer,
                    isPaymentSessionActive = isPaymentSessionActive,
                    paymentMethodType = paymentMethodType ?: PaymentMethod.Type.Card,
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId,
                    windowFlags = windowFlags
                )
            }
        }

        internal companion object {
            @JvmSynthetic
            internal fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }
        }
    }

    /**
     * The result of a [AddPaymentMethodActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    sealed class Result : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(ActivityStarter.Result.EXTRA to this)
        }

        @Parcelize
        data class Success internal constructor(
            val paymentMethod: PaymentMethod
        ) : Result()

        @Parcelize
        data class Failure internal constructor(
            val exception: Throwable
        ) : Result()

        @Parcelize
        data object Canceled : Result()

        companion object {
            /**
             * @return the [Result] object from the given `Intent`. [Canceled] by default.
             */
            @JvmStatic
            fun fromIntent(intent: Intent?): Result {
                return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA) ?: Canceled
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6001
    }
}
