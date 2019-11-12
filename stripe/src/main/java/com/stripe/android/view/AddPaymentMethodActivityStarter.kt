package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.LayoutRes
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.AddPaymentMethodActivityStarter.Args
import com.stripe.android.view.AddPaymentMethodActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.AddPaymentMethodActivityStarter.Result.Companion.fromIntent
import kotlinx.android.parcel.Parcelize

/**
 * A class to start [AddPaymentMethodActivity]. Arguments for the activity can be
 * specified with [Args] and constructed with [Args.Builder].
 *
 * The result will be returned with request code [REQUEST_CODE].
 */
class AddPaymentMethodActivityStarter constructor(
    activity: Activity
) : ActivityStarter<AddPaymentMethodActivity, Args>(
    activity,
    AddPaymentMethodActivity::class.java,
    Args.DEFAULT,
    REQUEST_CODE
) {
    @Parcelize
    data class Args internal constructor(
        internal val shouldAttachToCustomer: Boolean,
        internal val shouldRequirePostalCode: Boolean,
        internal val isPaymentSessionActive: Boolean,
        internal val shouldInitCustomerSessionTokens: Boolean,
        internal val paymentMethodType: PaymentMethod.Type,
        internal val paymentConfiguration: PaymentConfiguration?,
        @LayoutRes internal val addPaymentMethodFooter: Int
    ) : ActivityStarter.Args {

        class Builder : ObjectBuilder<Args> {
            private var shouldAttachToCustomer: Boolean = false
            private var shouldRequirePostalCode: Boolean = false
            private var isPaymentSessionActive = false
            private var shouldInitCustomerSessionTokens = true
            private var paymentMethodType: PaymentMethod.Type? = PaymentMethod.Type.Card
            private var paymentConfiguration: PaymentConfiguration? = null
            @LayoutRes
            private var addPaymentMethodFooter: Int = 0

            /**
             * If true, the created Payment Method will be attached to the current Customer
             * using an already-initialized [com.stripe.android.CustomerSession].
             */
            fun setShouldAttachToCustomer(shouldAttachToCustomer: Boolean): Builder {
                this.shouldAttachToCustomer = shouldAttachToCustomer
                return this
            }

            /**
             * If true, a postal code field will be shown and validated.
             * Currently, only US ZIP Codes are supported.
             */
            fun setShouldRequirePostalCode(shouldRequirePostalCode: Boolean): Builder {
                this.shouldRequirePostalCode = shouldRequirePostalCode
                return this
            }

            /**
             * Optional: specify the [PaymentMethod.Type] of the payment method to create based on
             * the customer's input (i.e. the form that will be presented to the customer).
             * If unspecified, defaults to [PaymentMethod.Type.Card].
             * Currently only [PaymentMethod.Type.Card] and [PaymentMethod.Type.Fpx] are supported.
             */
            fun setPaymentMethodType(paymentMethodType: PaymentMethod.Type): Builder {
                this.paymentMethodType = paymentMethodType
                return this
            }

            @JvmSynthetic
            internal fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder {
                this.isPaymentSessionActive = isPaymentSessionActive
                return this
            }

            @JvmSynthetic
            internal fun setShouldInitCustomerSessionTokens(
                shouldInitCustomerSessionTokens: Boolean
            ): Builder {
                this.shouldInitCustomerSessionTokens = shouldInitCustomerSessionTokens
                return this
            }

            @JvmSynthetic
            internal fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder {
                this.paymentConfiguration = paymentConfiguration
                return this
            }

            fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooter: Int): Builder {
                this.addPaymentMethodFooter = addPaymentMethodFooter
                return this
            }

            override fun build(): Args {
                return Args(
                    shouldAttachToCustomer = shouldAttachToCustomer,
                    shouldRequirePostalCode = shouldRequirePostalCode,
                    isPaymentSessionActive = isPaymentSessionActive,
                    shouldInitCustomerSessionTokens = shouldInitCustomerSessionTokens,
                    paymentMethodType = paymentMethodType ?: PaymentMethod.Type.Card,
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooter = addPaymentMethodFooter
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
     * The result of a [AddPaymentMethodActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    @Parcelize
    data class Result internal constructor(
        val paymentMethod: PaymentMethod
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ActivityStarter.Result.EXTRA, this)
            return bundle
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
        const val REQUEST_CODE: Int = 6001
    }
}
